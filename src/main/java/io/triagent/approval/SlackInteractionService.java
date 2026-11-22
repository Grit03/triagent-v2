package io.triagent.approval;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.triagent.action.ActionAlreadyDecidedException;
import io.triagent.action.ActionNotFoundException;
import io.triagent.action.ActionService;
import io.triagent.action.DecisionNotAllowedException;
import io.triagent.approval.SlackInteractionPayload.Action;

/**
 * 서명 검증을 통과한 버튼 클릭을 승인 또는 거절로 처리하고, 결과로 원래 Slack 메시지를 바꿉니다.
 */
@Service
public class SlackInteractionService {

	private static final Logger log = LoggerFactory.getLogger(SlackInteractionService.class);

	private final ActionService actionService;

	private final RestClient restClient;

	public SlackInteractionService(ActionService actionService, RestClient.Builder restClientBuilder) {
		this.actionService = actionService;
		this.restClient = restClientBuilder.build();
	}

	void handle(SlackInteractionPayload payload) {
		if (!"block_actions".equals(payload.type()) || payload.actions() == null || payload.actions().isEmpty()) {
			return;
		}
		Action clicked = payload.actions().get(0);
		String userId = payload.user().id();
		String resultText = decide(clicked, userId);
		replaceOriginalMessage(payload.responseUrl(), resultText);
	}

	private String decide(Action clicked, String userId) {
		Long actionId = Long.valueOf(clicked.value());
		try {
			return switch (clicked.actionId()) {
				case SlackApprovalRequester.APPROVE_ACTION_ID -> {
					actionService.approve(actionId, userId);
					yield ":white_check_mark: <@%s>님이 조치 %d를 승인했습니다. 실행을 시작합니다.".formatted(userId, actionId);
				}
				case SlackApprovalRequester.REJECT_ACTION_ID -> {
					actionService.reject(actionId, userId);
					yield ":no_entry: <@%s>님이 조치 %d를 거절했습니다.".formatted(userId, actionId);
				}
				default -> throw new IllegalArgumentException("Unknown action_id: " + clicked.actionId());
			};
		}
		catch (ActionAlreadyDecidedException | ActionNotFoundException | DecisionNotAllowedException ex) {
			log.info("Slack decision ignored: actionId={}, user={}, reason={}", actionId, userId, ex.getMessage());
			return ":warning: 조치 %d는 처리할 수 없습니다: %s".formatted(actionId, ex.getMessage());
		}
		catch (OptimisticLockingFailureException ex) {
			// 거의 동시에 다른 사람이 먼저 결정해 커밋한 경우입니다. 이쪽 결정은 롤백되어 실행되지 않습니다.
			log.info("Slack decision lost to a concurrent decision: actionId={}, user={}", actionId, userId);
			return ":warning: 조치 %d는 처리할 수 없습니다: 다른 사람이 먼저 처리했습니다.".formatted(actionId);
		}
	}

	/** response_url로 보내면 버튼이 있던 메시지가 결과 문구로 바뀌어, 같은 버튼을 다시 누를 수 없게 됩니다. */
	void replaceOriginalMessage(String responseUrl, String text) {
		restClient.post()
				.uri(responseUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("replace_original", true, "text", text))
				.retrieve()
				.toBodilessEntity();
	}
}
