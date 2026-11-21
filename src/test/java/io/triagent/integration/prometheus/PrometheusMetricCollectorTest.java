package io.triagent.integration.prometheus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import io.triagent.incident.Incident;
import io.triagent.investigation.Evidence;

class PrometheusMetricCollectorTest {

	static final Instant STARTED_AT = Instant.parse("2026-11-21T01:00:00Z");

	static final Instant BASELINE_AT = STARTED_AT.minus(Duration.ofMinutes(30));

	PrometheusClient client = mock(PrometheusClient.class);

	PrometheusMetricCollector collector = new PrometheusMetricCollector(client, properties());

	@Test
	void comparesBaselineAndCurrentValues() {
		given(client.query(eq("errors{service=\"payment-api\"}"), any()))
				.willAnswer(inv -> inv.getArgument(1).equals(BASELINE_AT) ? OptionalDouble.of(0.002)
						: OptionalDouble.of(0.0712));
		given(client.query(eq("latency{service=\"payment-api\"}"), any()))
				.willThrow(new IllegalStateException("timeout"));

		Evidence evidence = collector.collect(Incident.open("fp", "HighErrorRate", "payment-api", "critical", null,
				STARTED_AT));

		assertThat(evidence).isEqualTo(Evidence.success("prometheus", """
				기준: 장애 30분 전 → 현재
				- error-rate: 0.0020 → 0.0712
				- p95-latency: 조회 실패 (timeout)"""));
	}

	@Test
	void showsMissingDataExplicitly() {
		given(client.query(any(), any())).willReturn(OptionalDouble.empty());

		assertThat(collector.collect(Incident.open("fp", "a", "payment-api", "critical", null, STARTED_AT)).content())
				.contains("- error-rate: 데이터 없음 → 데이터 없음");
	}

	private static PrometheusProperties properties() {
		Map<String, String> queries = new LinkedHashMap<>();
		queries.put("error-rate", "errors{service=\"%1$s\"}");
		queries.put("p95-latency", "latency{service=\"%1$s\"}");
		return new PrometheusProperties(URI.create("http://prometheus:9090"), Duration.ofMinutes(30), queries);
	}
}
