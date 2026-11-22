package io.triagent.action;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActionExecutionListener {

	private final ActionService actionService;

	private final ActionExecutor actionExecutor;

	@Async
	@TransactionalEventListener
	public void onActionApproved(ActionApprovedEvent event) {
		ActionResponse action = actionService.getAction(event.actionId());
		try {
			String result = actionExecutor.execute(action.type(), action.service());
			actionService.recordExecuted(action.id(), result);
		}
		catch (RuntimeException ex) {
			log.error("Action execution failed: id={}", action.id(), ex);
			actionService.recordFailed(action.id(), "실행 실패: " + ex.getMessage());
		}
	}
}
