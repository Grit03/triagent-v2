package io.triagent.integration.loki;

import java.util.List;
import java.util.Map;

public record LokiQueryResponse(String status, Data data) {

	public record Data(String resultType, List<Stream> result) {
	}

	public record Stream(Map<String, String> stream, List<List<String>> values) {
	}
}
