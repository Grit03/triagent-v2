package io.triagent.integration.prometheus;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param lookback 기준값을 볼 시점. 장애 시작 시각에서 이만큼 이전입니다
 * @param queries 지표 이름 → PromQL 템플릿({@code %1$s}에 서비스 이름이 들어갑니다)
 */
@ConfigurationProperties("triagent.prometheus")
public record PrometheusProperties(
        URI baseUrl, @DefaultValue("30m") Duration lookback, Map<String, String> queries) {

    public PrometheusProperties {
        // YAML에 적은 순서대로 보고하도록 순서를 유지하는 Map으로 복사합니다.
        queries = queries == null ? Map.of() : new LinkedHashMap<>(queries);
    }
}
