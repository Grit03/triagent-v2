package io.triagent.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
                .andExpect(jsonPath("$.blocks[2].text.text").value("*분석*\n원인 후보: *최근 배포*"))
                .andExpect(
                        jsonPath("$.blocks[3].text.text")
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
                        "원인 후보: **최근 배포**"));

        server.verify();
    }

    @Test
    @DisplayName("Slack 길이 제한을 넘는 섹션은 자른다")
    void truncateSection() {
        String truncated = SlackNotifier.truncate("a".repeat(5000));

        assertThat(truncated).hasSize(2900 + "\n...(생략)".length());
    }

    @Test
    @DisplayName("일반 마크다운의 **굵게**를 Slack의 *굵게*로 바꾼다")
    void markdownBold() {
        String analysis =
                """
                *원인 후보*
                1. **v1.8.0 배포에 포함된 NPE (가장 유력)**: 로그 20건
                2. **입력 데이터의 null 값 (낮음)**: 근거 부족""";

        assertThat(SlackNotifier.toSlackBold(analysis))
                .isEqualTo(
                        """
                        *원인 후보*
                        1. *v1.8.0 배포에 포함된 NPE (가장 유력)*: 로그 20건
                        2. *입력 데이터의 null 값 (낮음)*: 근거 부족""");
    }

    @Test
    @DisplayName("짝이 없거나 줄을 넘는 별표는 그대로 둔다")
    void unmatchedAsterisks() {
        String text = "**짝이 없음\n다음 줄** 그리고 2 ** 3";

        assertThat(SlackNotifier.toSlackBold(text)).isEqualTo(text);
    }
}
