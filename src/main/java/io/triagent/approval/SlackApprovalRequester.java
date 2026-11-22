package io.triagent.approval;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import io.triagent.action.ActionRequestedEvent;
import io.triagent.action.ActionResponse;
import io.triagent.action.ActionService;
import io.triagent.approval.SlackApprovalMessage.Block;
import io.triagent.approval.SlackApprovalMessage.Button;
import io.triagent.notification.SlackProperties;

@Component
@ConditionalOnExpression("'${triagent.notification.slack.webhook-url:}' != ''")
public class SlackApprovalRequester {

	static final String APPROVE_ACTION_ID = "approve_action";

	static final String REJECT_ACTION_ID = "reject_action";

	private final RestClient restClient;

	private final SlackProperties properties;

	private final ActionService actionService;

	public SlackApprovalRequester(RestClient.Builder restClientBuilder, SlackProperties properties,
			ActionService actionService) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.actionService = actionService;
	}

	/** 요청 트랜잭션이 커밋된 뒤에 보내야, 버튼을 누를 때 조치가 DB에 있는 것이 보장됩니다. */
	@Async
	@TransactionalEventListener
	public void onActionRequested(ActionRequestedEvent event) {
		send(actionService.getAction(event.actionId()));
	}

	void send(ActionResponse action) {
		restClient.post()
				.uri(properties.webhookUrl())
				.contentType(MediaType.APPLICATION_JSON)
				.body(toMessage(action))
				.retrieve()
				.toBodilessEntity();
	}

	static SlackApprovalMessage toMessage(ActionResponse action) {
		String title = "[승인 요청] %s %s".formatted(action.service(), action.type());
		String reason = action.reason() != null ? action.reason() : "-";
		String details = "*%s*\n*사유* %s\n*요청자* `%s`\n*장애 ID* %d  *조치 ID* %d".formatted(title, reason,
				action.requestedBy(), action.incidentId(), action.id());
		return new SlackApprovalMessage(title, List.of(
				Block.section(details),
				Block.actions(
						Button.of("승인", APPROVE_ACTION_ID, action.id(), "primary"),
						Button.of("거절", REJECT_ACTION_ID, action.id(), "danger"))));
	}
}
