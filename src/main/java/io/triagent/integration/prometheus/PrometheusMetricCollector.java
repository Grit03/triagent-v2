package io.triagent.integration.prometheus;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.integration.LabelValues;
import io.triagent.investigation.ContextCollector;
import io.triagent.investigation.Evidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${triagent.prometheus.base-url:}' != ''")
@Slf4j
@RequiredArgsConstructor
public class PrometheusMetricCollector implements ContextCollector {

    static final String SOURCE = "prometheus";

    private final PrometheusClient prometheusClient;

    private final PrometheusProperties properties;

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
            lines.add(
                    describe(query.getKey(), query.getValue().formatted(service), baselineAt, now));
        }
        return Evidence.success(
                SOURCE,
                "기준: 장애 %d분 전 → 현재\n%s"
                        .formatted(properties.lookback().toMinutes(), String.join("\n", lines)));
    }

    private String describe(String name, String promQl, Instant baselineAt, Instant now) {
        try {
            return "- %s: %s → %s"
                    .formatted(
                            name,
                            format(prometheusClient.query(promQl, baselineAt)),
                            format(prometheusClient.query(promQl, now)));
        } catch (RuntimeException ex) {
            log.warn("Prometheus query failed: name={}", name, ex);
            return "- %s: 조회 실패 (%s)".formatted(name, ex.getMessage());
        }
    }

    private static String format(OptionalDouble value) {
        return value.isPresent()
                ? String.format(Locale.ROOT, "%.4f", value.getAsDouble())
                : "데이터 없음";
    }
}
