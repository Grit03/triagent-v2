package io.triagent.integration.prometheus;

import java.util.List;
import java.util.Map;

/**
 * Prometheus instant query 응답.
 * https://prometheus.io/docs/prometheus/latest/querying/api/#instant-queries
 * <p>
 * value는 [유닉스 초(숫자), "값(문자열)"] 형태라 타입이 섞여 있어 {@code List<Object>}로 받습니다.
 */
public record PrometheusQueryResponse(String status, Data data) {

	public record Data(String resultType, List<Sample> result) {
	}

	public record Sample(Map<String, String> metric, List<Object> value) {

		public double numericValue() {
			Object sampleValue = value.get(1);
			return Double.parseDouble(sampleValue.toString());
		}
	}
}
