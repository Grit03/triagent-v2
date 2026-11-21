package io.triagent.integration.loki;

import java.util.List;
import java.util.Map;

/**
 * Loki query_range 응답 중 로그 스트림 부분.
 * https://grafana.com/docs/loki/latest/reference/loki-http-api/#query-logs-within-a-range-of-time
 * <p>
 * values의 각 원소는 ["나노초 타임스탬프", "로그 한 줄"] 형태의 배열입니다.
 */
public record LokiQueryResponse(String status, Data data) {

	public record Data(String resultType, List<Stream> result) {
	}

	public record Stream(Map<String, String> stream, List<List<String>> values) {
	}
}
