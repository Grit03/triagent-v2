package io.triagent.integration.prometheus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PrometheusClientTest {

	static final Instant TIME = Instant.parse("2026-11-21T01:00:00Z");

	RestClient.Builder builder = RestClient.builder();

	MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	PrometheusClient client = new PrometheusClient(builder,
			new PrometheusProperties(URI.create("http://prometheus:9090"), Duration.ofMinutes(30), Map.of()));

	@Test
	@DisplayName("첫 번째 sample 값을 파싱한다")
	void query() {
		server.expect(requestTo(startsWith("http://prometheus:9090/api/v1/query?")))
				.andExpect(queryParam("query", "up%7Bservice%3D%22payment-api%22%7D"))
				.andExpect(queryParam("time", "2026-11-21T01:00:00Z"))
				.andRespond(withSuccess("""
						{ "status": "success",
						  "data": { "resultType": "vector",
						            "result": [ { "metric": {}, "value": [1763686800.123, "0.0712"] } ] } }
						""", MediaType.APPLICATION_JSON));

		OptionalDouble value = client.query("up{service=\"payment-api\"}", TIME);

		server.verify();
		assertThat(value).hasValue(0.0712);
	}

	@Test
	@DisplayName("일치하는 series가 없으면 빈 값을 반환한다")
	void noSeries() {
		server.expect(requestTo(startsWith("http://prometheus:9090/api/v1/query?")))
				.andRespond(withSuccess("""
						{ "status": "success", "data": { "resultType": "vector", "result": [] } }
						""", MediaType.APPLICATION_JSON));

		assertThat(client.query("up", TIME)).isEmpty();
	}
}
