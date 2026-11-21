package demo.payment;

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
 * 평소에는 결제 요청에 정상 응답하고, 에러 비율을 올리면 그 비율만큼 500 에러와 에러 로그를 냅니다.
 */
@SpringBootApplication
@RestController
public class PaymentApiApplication {

	private static final Logger log = LoggerFactory.getLogger(PaymentApiApplication.class);

	/** 0.0이면 항상 성공, 1.0이면 항상 실패합니다. */
	private volatile double errorRate = 0.0;

	public static void main(String[] args) {
		SpringApplication.run(PaymentApiApplication.class, args);
	}

	@PostMapping("/api/payments")
	public Map<String, String> pay() {
		if (ThreadLocalRandom.current().nextDouble() < errorRate) {
			IllegalStateException cause = new IllegalStateException("Connection pool exhausted: payment-db");
			log.error("Payment failed", cause);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "payment failed", cause);
		}
		return Map.of("status", "APPROVED");
	}

	/** 장애를 일으키거나(예: rate=0.5) 되돌립니다(rate=0). */
	@PutMapping("/chaos/error-rate")
	public Map<String, Double> setErrorRate(@RequestParam double rate) {
		errorRate = Math.max(0.0, Math.min(1.0, rate));
		log.warn("Error rate changed to {}", errorRate);
		return Map.of("errorRate", errorRate);
	}
}
