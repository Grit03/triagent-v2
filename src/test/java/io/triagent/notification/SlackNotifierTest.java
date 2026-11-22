package io.triagent.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.triagent.action.ActionResponse;
import io.triagent.action.ActionStatus;
import io.triagent.action.ActionType;
import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.InvestigationReport;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** MockRestServiceServer로 실제 Slack 대신 HTTP 요청 내용을 검증합니다. */
class SlackNotifierTest {

    static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXXX";

    @Test
    @DisplayName("Block Kit 메시지를 webhook으로 보낸다")
    void send() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackNotifier notifier =
                new SlackNotifier(builder, new SlackProperties(URI.create(WEBHOOK_URL), null));

        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.text").value("[CRITICAL] payment-api 장애 발생: HighErrorRate"))
                .andExpect(jsonPath("$.blocks[0].type").value("header"))
                .andExpect(jsonPath("$.blocks[0].text.type").value("plain_text"))
                .andExpect(jsonPath("$.blocks[1].type").value("section"))
                .andExpect(jsonPath("$.blocks[1].text.type").value("mrkdwn"))
                .andExpect(
                        jsonPath("$.blocks[2].text.text")
                                .value(containsString("`ROLLBACK` 조치 ID 7, 승인 대기")))
                .andExpect(jsonPath("$.blocks[3].text.text").value("*분석*\n원인 후보: 최근 배포"))
                .andExpect(
                        jsonPath("$.blocks[4].text.text")
                                .value(containsString(":warning: `loki` 조회 실패")))
                .andRespond(withSuccess());

        notifier.send(
                new InvestigationReport(
                        new IncidentSnapshot(
                                1L,
                                "HighErrorRate",
                                "payment-api",
                                "critical",
                                "5xx > 5%",
                                Instant.parse("2026-11-21T01:00:00Z")),
                        List.of(
                                Evidence.success("github", "최근 커밋 3건"),
                                Evidence.failure("loki", "조회 실패: timeout")),
                        "원인 후보: 최근 배포",
                        List.of(action(7L, ActionType.ROLLBACK, ActionStatus.PENDING))));

        server.verify();
    }

    @Test
    @DisplayName("요청된 조치가 없으면 분석 내용과 관계없이 승인 요청이 나가지 않았다고 알린다")
    void noRequestedActions() {
        assertThat(SlackNotifier.formatRequestedActions(List.of()))
                .isEqualTo("*요청된 조치*\n없음. 분석에 조치가 적혀 있어도 승인 요청은 나가지 않았습니다.");
    }

    @Test
    @DisplayName("요청된 조치를 상태와 요청자와 함께 나열한다")
    void requestedActions() {
        String text =
                SlackNotifier.formatRequestedActions(
                        List.of(
                                action(1L, ActionType.ROLLBACK, ActionStatus.PENDING),
                                action(2L, ActionType.RESTART, ActionStatus.EXECUTED)));

        assertThat(text)
                .isEqualTo(
                        """
                        *요청된 조치*
                        • `ROLLBACK` 조치 ID 1, 승인 대기 (요청자 `triagent-agent`)
                        • `RESTART` 조치 ID 2, 실행됨 (요청자 `triagent-agent`)""");
    }

    @Test
    @DisplayName("조치 요청 조회에 실패했으면 직접 확인하라고 알린다")
    void requestedActionsUnknown() {
        assertThat(SlackNotifier.formatRequestedActions(null))
                .startsWith("*요청된 조치*\n:warning: 조치 요청 내역을 확인하지 못했습니다.");
    }

    private static ActionResponse action(Long id, ActionType type, ActionStatus status) {
        return new ActionResponse(
                id,
                1L,
                "payment-api",
                type,
                "배포 직후 NPE",
                status,
                "triagent-agent",
                Instant.parse("2026-11-21T01:01:00Z"),
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Slack 길이 제한을 넘는 섹션은 자른다")
    void truncateSection() {
        String truncated = SlackNotifier.truncate("a".repeat(5000));

        assertThat(truncated).hasSize(2900 + "\n...(생략)".length());
    }
}
