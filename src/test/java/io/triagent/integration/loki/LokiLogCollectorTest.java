package io.triagent.integration.loki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.integration.loki.LokiClient.LogLine;
import io.triagent.investigation.Evidence;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LokiLogCollectorTest {

    static final Instant STARTED_AT = Instant.parse("2026-11-21T01:00:00Z");

    LokiClient client = mock(LokiClient.class);

    LokiLogCollector collector =
            new LokiLogCollector(
                    client,
                    new LokiProperties(
                            URI.create("http://loki:3100"),
                            "{service=\"%s\"} |= \"ERROR\"",
                            Duration.ofMinutes(15),
                            20));

    @Test
    @DisplayName("에러 로그를 근거로 정리한다")
    void collect() {
        given(
                        client.queryRange(
                                eq("{service=\"payment-api\"} |= \"ERROR\""),
                                eq(STARTED_AT.minusSeconds(900)),
                                any(),
                                eq(20)))
                .willReturn(
                        List.of(
                                new LogLine(
                                        STARTED_AT,
                                        "ERROR Connection pool exhausted\n\tat Hikari...")));

        Evidence evidence = collector.collect(incident("payment-api"));

        assertThat(evidence)
                .isEqualTo(
                        Evidence.success(
                                "loki",
                                """
				에러 로그 1건 (최신순)
				- 2026-11-21T01:00:00Z ERROR Connection pool exhausted \tat Hikari..."""));
    }

    @Test
    @DisplayName("에러 로그가 없으면 없다고 알려 준다")
    void noErrorLogs() {
        given(client.queryRange(any(), any(), any(), eq(20))).willReturn(List.of());

        assertThat(collector.collect(incident("payment-api")).content()).startsWith("에러 로그가 없습니다.");
    }

    private static IncidentSnapshot incident(String service) {
        return new IncidentSnapshot(1L, "HighErrorRate", service, "critical", null, STARTED_AT);
    }
}
