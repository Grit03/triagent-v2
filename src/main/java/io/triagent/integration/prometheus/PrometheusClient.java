package io.triagent.integration.prometheus;

import io.triagent.integration.prometheus.PrometheusQueryResponse.Sample;
import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${triagent.prometheus.base-url:}' != ''")
public class PrometheusClient {

    private final RestClient restClient;

    public PrometheusClient(RestClient.Builder restClientBuilder, PrometheusProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl().toString()).build();
    }

    /** 집계(sum 등)된 단일 값을 기대하므로, 여러 시계열이 나오면 첫 번째 값만 씁니다. */
    public OptionalDouble query(String promQl, Instant time) {
        PrometheusQueryResponse response =
                restClient
                        .get()
                        .uri(
                                uri ->
                                        uri.path("/api/v1/query")
                                                .queryParam("query", "{query}")
                                                .queryParam("time", time)
                                                .build(promQl))
                        .retrieve()
                        .body(PrometheusQueryResponse.class);
        if (response == null || response.data() == null || response.data().result() == null) {
            return OptionalDouble.empty();
        }
        List<Sample> samples = response.data().result();
        if (samples.isEmpty()) {
            return OptionalDouble.empty();
        }
        Sample firstSample = samples.get(0);
        return OptionalDouble.of(firstSample.numericValue());
    }
}
