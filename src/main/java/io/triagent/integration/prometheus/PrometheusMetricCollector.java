package io.triagent.integration.prometheus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.integration.LabelValues;
import io.triagent.investigation.ContextCollector;
import io.triagent.investigation.Evidence;

/**
 * 핵심 지표를 "장애 전 기준값 → 현재 값"으로 비교해 근거로 수집합니다.
 * 숫자 하나보다 변화량이 원인을 좁히는 데 더 도움이 되기 때문입니다.
 */
@Component
@ConditionalOnExpression("'${triagent.prometheus.base-url:}' != ''")
public class PrometheusMetricCollector implements ContextCollector {

	static final String SOURCE = "prometheus";

	private static final Logger log = LoggerFactory.getLogger(PrometheusMetricCollector.class);

	private final PrometheusClient prometheusClient;

	private final PrometheusProperties properties;

	public PrometheusMetricCollector(PrometheusClient prometheusClient, PrometheusProperties properties) {
		this.prometheusClient = prometheusClient;
		this.properties = properties;
	}

	@Override
	public String source() {
		return SOURCE;
	}

	@Override
	public Evidence collect(IncidentSnapshot incident) {
		if (properties.queries().isEmpty()) {
			return Evidence.failure(SOURCE, "설정된 지표 쿼리가 없습니다.");
		}
		String service = LabelValues.escape(incident.service());
		Instant baselineAt = incident.startedAt().minus(properties.lookback());
		Instant now = Instant.now();
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, String> query : properties.queries().entrySet()) {
			lines.add(describe(query.getKey(), query.getValue().formatted(service), baselineAt, now));
		}
		return Evidence.success(SOURCE, "기준: 장애 %d분 전 → 현재\n%s".formatted(properties.lookback().toMinutes(),
				String.join("\n", lines)));
	}

	/** 지표 하나가 실패해도 나머지 지표는 계속 보여줍니다. */
	private String describe(String name, String promQl, Instant baselineAt, Instant now) {
		try {
			return "- %s: %s → %s".formatted(name, format(prometheusClient.query(promQl, baselineAt)),
					format(prometheusClient.query(promQl, now)));
		}
		catch (RuntimeException ex) {
			log.warn("Prometheus query failed: name={}", name, ex);
			return "- %s: 조회 실패 (%s)".formatted(name, ex.getMessage());
		}
	}

	private static String format(OptionalDouble value) {
		return value.isPresent() ? String.format(Locale.ROOT, "%.4f", value.getAsDouble()) : "데이터 없음";
	}
}
