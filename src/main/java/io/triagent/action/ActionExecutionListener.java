package io.triagent.action;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 승인 트랜잭션이 커밋된 뒤에 조치를 실행합니다. 장애 조사(InvestigationEventListener)와 같은 방식입니다.
 * <p>
 * 실행기 호출에는 DB 트랜잭션을 걸지 않고, 결과 기록만 ActionService의 짧은 트랜잭션으로 남깁니다.
 */
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
