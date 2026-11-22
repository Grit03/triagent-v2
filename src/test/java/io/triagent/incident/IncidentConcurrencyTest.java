package io.triagent.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 같은 알림이 동시에 들어와도 장애가 한 건만 열리는지 확인합니다. 기본은 H2에서 돌고, {@link PostgresIncidentConcurrencyTest}가 같은
 * 테스트를 PostgreSQL에서 다시 실행합니다.
 */
@SpringBootTest
class IncidentConcurrencyTest {

    /** 고치기 전에는 거의 매 라운드 중복이 생겼으므로(H2 200번 중 198번) 이 정도면 충분히 재현됩니다. */
    private static final int ROUNDS = 50;

    @Autowired IncidentService incidentService;

    @Autowired IncidentRepository incidentRepository;

    @Autowired JdbcTemplate jdbcTemplate;

    ExecutorService executor = Executors.newFixedThreadPool(2);

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("같은 firing 알림이 동시에 두 번 들어와도 장애는 한 건만 열리고 두 요청 모두 성공한다")
    void concurrentFiring() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            String fingerprint = "concurrent-" + round;
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<Optional<Long>> first = executor.submit(() -> openAfter(barrier, fingerprint));
            Future<Optional<Long>> second = executor.submit(() -> openAfter(barrier, fingerprint));

            // 예외 없이 끝나야 하고(웹훅 500 없음), 둘 중 정확히 하나만 장애를 엽니다.
            List<Optional<Long>> results = List.of(first.get(), second.get());
            assertThat(results).filteredOn(Optional::isPresent).hasSize(1);
            assertThat(
                            incidentRepository.findAllByFingerprintAndStatus(
                                    fingerprint, IncidentStatus.OPEN))
                    .hasSize(1);
        }
    }

    @Test
    @DisplayName("장애가 해소된 뒤 같은 알림이 다시 울리면 새 장애를 연다")
    void reopenAfterResolve() {
        Long firstId = incidentService.openIfAbsent(command("reopen", "summary")).orElseThrow();
        incidentService.resolveIfOpen("reopen", Instant.parse("2026-11-21T01:30:00Z"));

        Optional<Long> secondId = incidentService.openIfAbsent(command("reopen", "summary"));

        assertThat(secondId).isPresent().get().isNotEqualTo(firstId);
    }

    @Test
    @DisplayName("중복이 아닌 제약 위반은 삼키지 않고 그대로 던진다")
    void otherViolationIsThrown() {
        String tooLongSummary = "x".repeat(1001);

        assertThatThrownBy(() -> incidentService.openIfAbsent(command("too-long", tooLongSummary)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("제약을 추가하기 전에 중복으로 열린 장애도 resolved 알림 한 번에 모두 닫힌다")
    void resolveLegacyDuplicates() {
        incidentService.openIfAbsent(command("legacy", "summary")).orElseThrow();
        // V3 마이그레이션은 기존 중복 중 한 건에만 open_fingerprint를 넣습니다. 나머지 한 건을 직접 만듭니다.
        jdbcTemplate.update(
                """
				insert into incident (fingerprint, alert_name, service, severity, status, started_at)
				values ('legacy', 'HighErrorRate', 'payment-api', 'critical', 'OPEN', current_timestamp)
				""");

        List<Long> resolved =
                incidentService.resolveIfOpen("legacy", Instant.parse("2026-11-21T01:30:00Z"));

        assertThat(resolved).hasSize(2);
        assertThat(incidentRepository.findAllByFingerprintAndStatus("legacy", IncidentStatus.OPEN))
                .isEmpty();
    }

    private Optional<Long> openAfter(CyclicBarrier barrier, String fingerprint) throws Exception {
        barrier.await();
        return incidentService.openIfAbsent(command(fingerprint, "summary"));
    }

    private static OpenIncidentCommand command(String fingerprint, String summary) {
        return new OpenIncidentCommand(
                fingerprint,
                "HighErrorRate",
                "payment-api",
                "critical",
                summary,
                Instant.parse("2026-11-21T01:00:00Z"));
    }
}
