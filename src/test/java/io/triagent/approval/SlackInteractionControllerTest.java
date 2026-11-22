package io.triagent.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.triagent.action.ActionService;
import io.triagent.action.ActionStatus;
import io.triagent.action.ActionType;
import io.triagent.action.CreateActionRequest;
import io.triagent.action.RemediationAction;
import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

/**
 * 실제 서명을 만들어 보내, 서명 검증부터 승인 처리까지 전체 흐름을 확인합니다.
 * Slack으로 결과를 보내는 부분(response_url 호출)만 가짜로 바꿉니다.
 */
@SpringBootTest(properties = "SLACK_SIGNING_SECRET=test-signing-secret")
@AutoConfigureMockMvc
class SlackInteractionControllerTest {

	static final String RESPONSE_URL = "https://hooks.slack.com/actions/T000/1/abc";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	SlackSignatureVerifier signatureVerifier;

	@MockitoSpyBean
	ActionService actionService;

	@Autowired
	IncidentService incidentService;

	@MockitoSpyBean
	SlackInteractionService interactionService;

	Long actionId;

	@BeforeEach
	void requestAction() {
		willDoNothing().given(interactionService).replaceOriginalMessage(anyString(), anyString());
		Long incidentId = incidentService.openIfAbsent(new OpenIncidentCommand("slack-" + System.nanoTime(),
				"HighErrorRate", "payment-api", "critical", null, Instant.parse("2026-11-21T01:00:00Z")))
				.orElseThrow();
		actionId = actionService.request(incidentId, new CreateActionRequest(ActionType.RESTART, "U_ONCALL", null))
				.id();
	}

	@Test
	void signedApproveClickApprovesAction() {
		assertThat(click("approve_action", true)).hasStatusOk();

		assertThat(actionService.getAction(actionId).decidedBy()).isEqualTo("U_LEAD");
		verify(interactionService).replaceOriginalMessage(eq(RESPONSE_URL), contains("승인했습니다"));
	}

	@Test
	void secondClickOnDecidedActionIsReportedNotApplied() {
		click("reject_action", true);

		assertThat(click("approve_action", true)).hasStatusOk();

		assertThat(actionService.getAction(actionId).status()).isEqualTo(ActionStatus.REJECTED);
		verify(interactionService).replaceOriginalMessage(eq(RESPONSE_URL), contains("처리할 수 없습니다"));
	}

	@Test
	void clickThatLosesToConcurrentDecisionIsReported() {
		willThrow(new ObjectOptimisticLockingFailureException(RemediationAction.class, actionId)).given(actionService)
				.approve(actionId, "U_LEAD");

		assertThat(click("approve_action", true)).hasStatusOk();

		verify(interactionService).replaceOriginalMessage(eq(RESPONSE_URL), contains("다른 사람이 먼저 처리했습니다"));
	}

	@Test
	void clickFromUserOutsideOwningTeamIsDenied() {
		assertThat(click("approve_action", true, "U_STRANGER")).hasStatusOk();

		assertThat(actionService.getAction(actionId).status()).isEqualTo(ActionStatus.PENDING);
		verify(interactionService).replaceOriginalMessage(eq(RESPONSE_URL), contains("not a member"));
	}

	@Test
	void unsignedClickIsRejected() {
		assertThat(click("approve_action", false)).hasStatus(HttpStatus.UNAUTHORIZED);

		assertThat(actionService.getAction(actionId).status()).isEqualTo(ActionStatus.PENDING);
	}

	private MvcTestResult click(String buttonActionId, boolean signed) {
		return click(buttonActionId, signed, "U_LEAD");
	}

	private MvcTestResult click(String buttonActionId, boolean signed, String userId) {
		String payload = """
				{"type":"block_actions","user":{"id":"%s"},"response_url":"%s",
				 "actions":[{"action_id":"%s","value":"%d"}]}""".formatted(userId, RESPONSE_URL, buttonActionId, actionId);
		String body = "payload=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
		String timestamp = String.valueOf(Instant.now().getEpochSecond());
		String signature = signed ? signatureVerifier.sign("v0:" + timestamp + ":" + body) : "v0=forged";
		return mvc.post().uri("/api/v1/slack/interactions")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.header("X-Slack-Request-Timestamp", timestamp)
				.header("X-Slack-Signature", signature)
				.content(body)
				.exchange();
	}
}
