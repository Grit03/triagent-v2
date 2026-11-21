package io.triagent.integration.prometheus;

import java.time.Instant;
import java.util.OptionalDouble;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Prometheus HTTP API 호출만 담당합니다.
 */
@Component
@ConditionalOnExpression("'${triagent.prometheus.base-url:}' != ''")
public class PrometheusClient {

	private final RestClient restClient;

	public PrometheusClient(RestClient.Builder restClientBuilder, PrometheusProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl().toString()).build();
	}

	/**
	 * 특정 시각의 PromQL 결과 값을 조회합니다. 결과가 없으면 빈 값을 돌려줍니다.
	 * 집계(sum 등)된 단일 값을 기대하므로, 여러 시계열이 나오면 첫 번째 값만 씁니다.
	 */
	public OptionalDouble query(String promQl, Instant time) {
		PrometheusQueryResponse response = restClient.get()
				.uri(uri -> uri.path("/api/v1/query")
						.queryParam("query", "{query}")
						.queryParam("time", time)
						.build(promQl))
				.retrieve()
				.body(PrometheusQueryResponse.class);
		if (response == null || response.data() == null || response.data().result() == null
				|| response.data().result().isEmpty()) {
			return OptionalDouble.empty();
		}
		Object value = response.data().result().get(0).value().get(1);
		return OptionalDouble.of(Double.parseDouble(value.toString()));
	}
}
