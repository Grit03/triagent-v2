package io.triagent.notification;

import io.triagent.action.ActionResponse;
import io.triagent.action.ActionStatus;
import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.InvestigationReport;
import io.triagent.investigation.Notifier;
import io.triagent.notification.SlackMessage.Block;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${triagent.notification.slack.webhook-url:}' != ''")
public class SlackNotifier implements Notifier {

    private static final int MAX_SECTION_LENGTH = 2900;

    private static final Pattern MARKDOWN_BOLD =
            Pattern.compile("\\*\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*\\*");

    private final RestClient restClient;

    private final SlackProperties properties;

    public SlackNotifier(RestClient.Builder restClientBuilder, SlackProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public void send(InvestigationReport report) {
        restClient
                .post()
                .uri(properties.webhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(toMessage(report))
                .retrieve()
                .toBodilessEntity();
    }

    SlackMessage toMessage(InvestigationReport report) {
        IncidentSnapshot incident = report.incident();
        String title =
                "[%s] %s 장애 발생: %s"
                        .formatted(
                                incident.severity().toUpperCase(),
                                incident.service(),
                                incident.alertName());
        String summary = incident.summary() != null ? incident.summary() : "-";
        String overview =
                "*요약*\n%s\n*시작 시각* %s\n*장애 ID* %d"
                        .formatted(summary, incident.startedAt(), incident.id());
        String evidenceText =
                report.evidences().isEmpty()
                        ? "수집된 근거가 없습니다."
                        : report.evidences().stream()
                                .map(SlackNotifier::formatEvidence)
                                .collect(Collectors.joining("\n"));
        return new SlackMessage(
                title,
                List.of(
                        Block.header(title),
                        Block.section(overview),
                        Block.section(formatRequestedActions(report.requestedActions())),
                        Block.section("*분석*\n" + truncate(toSlackBold(report.analysis()))),
                        Block.section("*조사 근거*\n" + truncate(evidenceText))));
    }

    /**
     * 모델이 일반 마크다운의 {@code **굵게**}를 쓰면 Slack mrkdwn은 별표를 그대로 보여 주므로 {@code *굵게*}로 바꿉니다. 한 줄 안에서 짝이
     * 맞고 안쪽이 공백으로 시작하거나 끝나지 않는 경우만 바꿔, {@code 2 ** 3} 같은 별표는 건드리지 않습니다.
     */
    static String toSlackBold(String text) {
        return MARKDOWN_BOLD.matcher(text).replaceAll("*$1*");
    }

    /** Slack section 블록의 text는 3,000자를 넘으면 전송이 실패하므로 여유 있게 자릅니다. */
    static String truncate(String text) {
        return text.length() <= MAX_SECTION_LENGTH
                ? text
                : text.substring(0, MAX_SECTION_LENGTH) + "\n...(생략)";
    }

    /** 분석 텍스트는 모델이 쓴 것이라, 실제로 요청된 조치는 DB 기준으로 분석보다 앞에 따로 보여 줍니다. */
    static String formatRequestedActions(List<ActionResponse> actions) {
        if (actions == null) {
            return "*요청된 조치*\n:warning: 조치 요청 내역을 확인하지 못했습니다. 채널의 승인 요청 메시지를 직접 확인하세요.";
        }
        if (actions.isEmpty()) {
            return "*요청된 조치*\n없음. 분석에 조치가 적혀 있어도 승인 요청은 나가지 않았습니다.";
        }
        return "*요청된 조치*\n"
                + actions.stream()
                        .map(
                                action ->
                                        "• `%s` 조치 ID %d, %s (요청자 `%s`)"
                                                .formatted(
                                                        action.type(),
                                                        action.id(),
                                                        statusLabel(action.status()),
                                                        action.requestedBy()))
                        .collect(Collectors.joining("\n"));
    }

    private static String statusLabel(ActionStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인됨";
            case REJECTED -> "거절됨";
            case EXECUTED -> "실행됨";
            case FAILED -> "실행 실패";
        };
    }

    private static String formatEvidence(Evidence evidence) {
        String bullet = evidence.success() ? "• " : "• :warning: ";
        return bullet + "`" + evidence.source() + "` " + evidence.content();
    }
}
