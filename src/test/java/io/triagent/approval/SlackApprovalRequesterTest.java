package io.triagent.approval;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.triagent.action.ActionResponse;
import io.triagent.action.ActionService;
import io.triagent.action.ActionStatus;
import io.triagent.action.ActionType;
import io.triagent.notification.SlackProperties;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackApprovalRequesterTest {

    static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXXX";

    @Test
    @DisplayName("승인, 거절 버튼이 달린 메시지를 보낸다")
    void send() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackApprovalRequester requester =
                new SlackApprovalRequester(
                        builder,
                        new SlackProperties(URI.create(WEBHOOK_URL), null),
                        mock(ActionService.class));

        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.text").value("[승인 요청] payment-api ROLLBACK"))
                .andExpect(jsonPath("$.blocks[0].type").value("section"))
                .andExpect(jsonPath("$.blocks[0].elements").doesNotExist())
                .andExpect(jsonPath("$.blocks[1].type").value("actions"))
                .andExpect(jsonPath("$.blocks[1].elements[0].action_id").value("approve_action"))
                .andExpect(jsonPath("$.blocks[1].elements[0].value").value("7"))
                .andExpect(jsonPath("$.blocks[1].elements[0].style").value("primary"))
                .andExpect(jsonPath("$.blocks[1].elements[1].action_id").value("reject_action"))
                .andExpect(jsonPath("$.blocks[1].elements[1].text.type").value("plain_text"))
                .andRespond(withSuccess());

        requester.send(
                new ActionResponse(
                        7L,
                        3L,
                        "payment-api",
                        ActionType.ROLLBACK,
                        "a1b2c3d 배포 후 5xx 증가",
                        ActionStatus.PENDING,
                        "U_ONCALL",
                        Instant.parse("2026-11-21T02:00:00Z"),
                        null,
                        null,
                        null,
                        null));

        server.verify();
    }
}
