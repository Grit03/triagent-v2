package io.triagent.integration.loki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.triagent.integration.loki.LokiClient.LogLine;

class LokiClientTest {

	RestClient.Builder builder = RestClient.builder();

	MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	LokiClient client = new LokiClient(builder,
			new LokiProperties(URI.create("http://loki:3100"), "unused", Duration.ofMinutes(15), 20));

	@Test
	@DisplayName("여러 stream의 로그를 합쳐 최신순으로 정렬한다")
	void queryRange() {
		server.expect(requestTo(startsWith("http://loki:3100/loki/api/v1/query_range?")))
				.andExpect(method(HttpMethod.GET))
				// LogQL의 특수문자({, ", | 등)가 퍼센트 인코딩되어 전송되는지 확인합니다.
				.andExpect(queryParam("query", "%7Bservice%3D%22payment-api%22%7D%20%7C~%20%22error%22"))
				.andExpect(queryParam("direction", "backward"))
				.andExpect(queryParam("limit", "20"))
				.andRespond(withSuccess("""
						{
						  "status": "success",
						  "data": {
						    "resultType": "streams",
						    "result": [
						      { "stream": { "pod": "payment-1" },
						        "values": [ ["1763686800000000000", "ERROR older"] ] },
						      { "stream": { "pod": "payment-2" },
						        "values": [ ["1763686860500000000", "ERROR newer"] ] }
						    ]
						  }
						}
						""", MediaType.APPLICATION_JSON));

		List<LogLine> lines = client.queryRange("{service=\"payment-api\"} |~ \"error\"",
				Instant.parse("2026-11-21T00:45:00Z"), Instant.parse("2026-11-21T01:05:00Z"), 20);

		server.verify();
		assertThat(lines).containsExactly(
				new LogLine(Instant.parse("2025-11-21T01:01:00.5Z"), "ERROR newer"),
				new LogLine(Instant.parse("2025-11-21T01:00:00Z"), "ERROR older"));
	}
}
