package io.triagent.notification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.triagent.incident.IncidentResponse;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.InvestigationReport;

/**
 * Slack Incoming Webhook으로 조사 결과를 보냅니다.
 * <p>
 * https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks
 * webhook-url이 설정된 경우에만 빈으로 등록됩니다.
 */
@Component
@ConditionalOnExpression("'${triagent.notification.slack.webhook-url:}' != ''")
public class SlackNotifier implements Notifier {

	private final RestClient restClient;

	private final SlackProperties properties;

	/** Spring Boot가 자동 구성한 RestClient.Builder를 받아 쓰면 공통 설정(메시지 컨버터, 관측)이 그대로 적용됩니다. */
	public SlackNotifier(RestClient.Builder restClientBuilder, SlackProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public void send(InvestigationReport report) {
		restClient.post()
				.uri(properties.webhookUrl())
				.contentType(MediaType.APPLICATION_JSON)
				.body(toMessage(report))
				.retrieve()
				.toBodilessEntity();
	}

	/**
	 * Block Kit 메시지를 만듭니다. text는 알림 미리보기와 blocks를 표시하지 못하는 클라이언트에서 쓰입니다.
	 */
	Map<String, Object> toMessage(InvestigationReport report) {
		IncidentResponse incident = report.incident();
		String title = "[%s] %s 장애 발생: %s".formatted(incident.severity().toUpperCase(), incident.service(),
				incident.alertName());
		String evidenceText = report.evidences().isEmpty() ? "수집된 근거가 없습니다."
				: report.evidences().stream().map(SlackNotifier::formatEvidence).collect(Collectors.joining("\n"));
		return Map.of("text", title, "blocks", List.of(
				Map.of("type", "header", "text", Map.of("type", "plain_text", "text", title)),
				section("*요약*\n" + (incident.summary() != null ? incident.summary() : "-")
						+ "\n*시작 시각* " + incident.startedAt() + "\n*장애 ID* " + incident.id()),
				section("*조사 근거*\n" + evidenceText)));
	}

	private static Map<String, Object> section(String markdown) {
		return Map.of("type", "section", "text", Map.of("type", "mrkdwn", "text", markdown));
	}

	private static String formatEvidence(Evidence evidence) {
		return (evidence.success() ? "• " : "• :warning: ") + "`" + evidence.source() + "` " + evidence.content();
	}
}
