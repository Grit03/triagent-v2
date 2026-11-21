package io.triagent.notification;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.triagent.incident.IncidentResponse;
import io.triagent.incident.IncidentStatus;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.InvestigationReport;

/**
 * MockRestServiceServer로 실제 Slack 대신 HTTP 요청 내용을 검증합니다.
 */
class SlackNotifierTest {

	static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXXX";

	@Test
	void postsBlockKitMessageToWebhook() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SlackNotifier notifier = new SlackNotifier(builder, new SlackProperties(URI.create(WEBHOOK_URL)));

		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.text").value("[CRITICAL] payment-api 장애 발생: HighErrorRate"))
				.andExpect(jsonPath("$.blocks[0].type").value("header"))
				.andExpect(jsonPath("$.blocks[2].text.text").value(containsString(":warning: `loki` 조회 실패")))
				.andRespond(withSuccess());

		notifier.send(new InvestigationReport(
				new IncidentResponse(1L, "HighErrorRate", "payment-api", "critical", "5xx > 5%", IncidentStatus.OPEN,
						Instant.parse("2026-11-21T01:00:00Z"), null),
				List.of(Evidence.success("github", "최근 커밋 3건"), Evidence.failure("loki", "조회 실패: timeout"))));

		server.verify();
	}
}
