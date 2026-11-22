package demo.payment;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 장애 상황을 재현하기 위한 데모 결제 서버.
 * 장애 시나리오를 고르면 그 원인에 맞는 에러 로그, 응답 코드, 지연을 냅니다.
 */
@SpringBootApplication
@RestController
public class PaymentApiApplication {

	private static final Logger log = LoggerFactory.getLogger(PaymentApiApplication.class);

	private static final String STABLE_VERSION = "1.7.3";

	/** 평소 버전은 며칠 전에 배포된 것으로 둡니다. 그래야 배포 직후 장애와 구분됩니다. */
	private static final Instant STABLE_DEPLOYED_AT = Instant.now().minus(Duration.ofDays(3));

	private volatile Scenario scenario = Scenario.NONE;

	private volatile String version = STABLE_VERSION;

	private volatile Instant deployedAt = STABLE_DEPLOYED_AT;

	public static void main(String[] args) {
		SpringApplication.run(PaymentApiApplication.class, args);
	}

	@PostMapping("/api/payments")
	public Map<String, String> pay() throws InterruptedException {
		Scenario current = scenario;
		Thread.sleep(current.latency);
		if (ThreadLocalRandom.current().nextDouble() < current.errorRate) {
			Throwable cause = current.failure();
			// 운영 서버처럼 버전과 배포 시각을 로그에 함께 남깁니다. 분석할 때 배포와의 관련성을 판단하는 근거가 됩니다.
			log.error("Payment failed (version={}, deployedAt={})", version, deployedAt, cause);
			throw new ResponseStatusException(current.status, "payment failed", cause);
		}
		return Map.of("status", "APPROVED");
	}

	/** 장애 시나리오를 바꿉니다. NONE이면 정상으로 돌아갑니다. */
	@PutMapping("/chaos/scenario")
	public Map<String, String> setScenario(@RequestParam Scenario name) {
		scenario = name;
		boolean deploy = name == Scenario.BAD_DEPLOY;
		version = deploy ? "1.8.0" : STABLE_VERSION;
		deployedAt = deploy ? Instant.now() : STABLE_DEPLOYED_AT;
		log.warn("Chaos scenario changed to {} (version={}, deployedAt={})", name, version, deployedAt);
		return Map.of("scenario", name.name(), "version", version);
	}

	/** 수수료 계산 코드에 들어간 버그를 흉내 냅니다. 새 버전에서 통화별 수수료율이 null이 됩니다. */
	static long calculateFee(long amount, Double rate) {
		return Math.round(amount * rate);
	}

	enum Scenario {

		NONE(0.0, 0, HttpStatus.OK),
		/** DB 커넥션 풀 고갈: 빠르게 실패합니다. 재시작이나 롤백으로는 해결되지 않습니다. */
		DB_POOL(0.5, 0, HttpStatus.INTERNAL_SERVER_ERROR),
		/** 잘못된 배포: 방금 배포한 1.8.0의 수수료 계산 버그. 롤백이 맞는 조치입니다. */
		BAD_DEPLOY(0.6, 0, HttpStatus.INTERNAL_SERVER_ERROR),
		/** 메모리 누수: 응답이 느려지고 힙 부족 에러가 납니다. 재시작이 당장의 조치입니다. */
		MEMORY_LEAK(0.3, 800, HttpStatus.SERVICE_UNAVAILABLE),
		/** 외부 결제 게이트웨이 지연: 우리 서버 문제가 아니므로 롤백, 재시작 모두 맞지 않습니다. */
		SLOW_GATEWAY(0.2, 2000, HttpStatus.GATEWAY_TIMEOUT);

		final double errorRate;

		final long latency;

		final HttpStatus status;

		Scenario(double errorRate, long latency, HttpStatus status) {
			this.errorRate = errorRate;
			this.latency = latency;
			this.status = status;
		}

		Throwable failure() {
			return switch (this) {
				case DB_POOL -> new IllegalStateException("Connection pool exhausted: payment-db (active=20, max=20)");
				case BAD_DEPLOY -> fail(() -> calculateFee(10_000, null));
				case MEMORY_LEAK -> new OutOfMemoryError("Java heap space");
				case SLOW_GATEWAY -> new SocketTimeoutException("Read timed out: pg-gateway.example.com (timeout=2000ms)");
				case NONE -> throw new IllegalStateException("NONE scenario never fails");
			};
		}

		/** 실제로 예외를 일으켜, 로그의 스택 트레이스에 버그가 난 메서드가 찍히게 합니다. */
		private static Throwable fail(Runnable buggyCode) {
			try {
				buggyCode.run();
				throw new IllegalStateException("expected the buggy code to fail");
			}
			catch (RuntimeException ex) {
				return ex;
			}
		}
	}
}
