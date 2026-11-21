package io.triagent.integration.loki;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Loki HTTP API 호출만 담당합니다.
 */
@Component
@ConditionalOnExpression("'${triagent.loki.base-url:}' != ''")
public class LokiClient {

	private final RestClient restClient;

	public LokiClient(RestClient.Builder restClientBuilder, LokiProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl().toString()).build();
	}

	/**
	 * 기간 안에서 LogQL에 맞는 로그를 최신순으로 최대 {@code limit}줄 가져옵니다.
	 */
	public List<LogLine> queryRange(String logQl, Instant start, Instant end, int limit) {
		LokiQueryResponse response = restClient.get()
				.uri(uri -> uri.path("/loki/api/v1/query_range")
						// LogQL에는 {, }, " 같은 문자가 있어 URI 변수로 넘겨야 올바르게 인코딩됩니다.
						.queryParam("query", "{query}")
						.queryParam("start", start)
						.queryParam("end", end)
						.queryParam("limit", limit)
						.queryParam("direction", "backward")
						.build(logQl))
				.retrieve()
				.body(LokiQueryResponse.class);
		if (response == null || response.data() == null || response.data().result() == null) {
			return List.of();
		}
		// 스트림(라벨 조합)별로 나뉜 결과를 하나로 합쳐 최신순으로 정렬합니다.
		return response.data().result().stream()
				.flatMap(stream -> stream.values().stream())
				.map(value -> new LogLine(toInstant(value.get(0)), value.get(1)))
				.sorted(Comparator.comparing(LogLine::timestamp).reversed())
				.limit(limit)
				.toList();
	}

	private static Instant toInstant(String epochNanos) {
		long nanos = Long.parseLong(epochNanos);
		return Instant.ofEpochSecond(nanos / 1_000_000_000L, nanos % 1_000_000_000L);
	}

	public record LogLine(Instant timestamp, String line) {
	}
}
