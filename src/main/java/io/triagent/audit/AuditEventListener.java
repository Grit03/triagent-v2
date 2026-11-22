package io.triagent.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.triagent.action.ActionApprovedEvent;
import io.triagent.action.ActionDecisionDeniedEvent;
import io.triagent.action.ActionFinishedEvent;
import io.triagent.action.ActionRejectedEvent;
import io.triagent.action.ActionRequestedEvent;
import io.triagent.action.ActionResponse;
import io.triagent.action.ActionService;
import io.triagent.action.ActionStatus;
import io.triagent.incident.IncidentOpenedEvent;
import io.triagent.incident.IncidentResolvedEvent;
import io.triagent.incident.IncidentService;

/** 이미 커밋된 업무를 감사 로그 실패 때문에 오류로 응답하지 않도록, 기록 실패는 예외 대신 에러 로그로 남깁니다. */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventListener {

	static final String ALERTMANAGER = "alertmanager";

	static final String EXECUTOR = "triagent";

	private final AuditLogService auditLogService;

	private final IncidentService incidentService;

	private final ActionService actionService;

	@TransactionalEventListener
	public void onIncidentOpened(IncidentOpenedEvent event) {
		recordSafely(() -> {
			String detail = incidentService.findSnapshot(event.incidentId())
					.map(incident -> "%s (%s, %s)".formatted(incident.alertName(), incident.service(),
							incident.severity()))
					.orElse(null);
			auditLogService.record(event.openedAt(), ALERTMANAGER, AuditEventType.INCIDENT_OPENED, event.incidentId(), null, detail);
		});
	}

	@TransactionalEventListener
	public void onIncidentResolved(IncidentResolvedEvent event) {
		recordSafely(() -> {
			auditLogService.record(event.resolvedAt(), ALERTMANAGER, AuditEventType.INCIDENT_RESOLVED, event.incidentId(), null, null);
		});
	}

	@TransactionalEventListener
	public void onActionRequested(ActionRequestedEvent event) {
		recordSafely(() -> {
			ActionResponse action = actionService.getAction(event.actionId());
			String detail = "%s %s: %s".formatted(action.type(), action.service(), action.reason());
			auditLogService.record(action.requestedAt(), action.requestedBy(), AuditEventType.ACTION_REQUESTED, action.incidentId(),
					action.id(), detail);
		});
	}

	@TransactionalEventListener
	public void onActionApproved(ActionApprovedEvent event) {
		recordSafely(() -> recordDecision(event.actionId(), AuditEventType.ACTION_APPROVED));
	}

	@TransactionalEventListener
	public void onActionRejected(ActionRejectedEvent event) {
		recordSafely(() -> recordDecision(event.actionId(), AuditEventType.ACTION_REJECTED));
	}

	/** 거부된 결정은 트랜잭션이 롤백되므로 AFTER_ROLLBACK에서 받습니다. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
	public void onDecisionDenied(ActionDecisionDeniedEvent event) {
		recordSafely(() -> {
			ActionResponse action = actionService.getAction(event.actionId());
			auditLogService.record(event.deniedAt(), event.userId(), AuditEventType.ACTION_DENIED, action.incidentId(),
					action.id(), event.reason());
		});
	}

	@TransactionalEventListener
	public void onActionFinished(ActionFinishedEvent event) {
		recordSafely(() -> {
			ActionResponse action = actionService.getAction(event.actionId());
			AuditEventType type = action.status() == ActionStatus.EXECUTED ? AuditEventType.ACTION_EXECUTED
					: AuditEventType.ACTION_FAILED;
			auditLogService.record(action.finishedAt(), EXECUTOR, type, action.incidentId(), action.id(),
					action.result());
		});
	}

	private void recordDecision(Long actionId, AuditEventType type) {
		ActionResponse action = actionService.getAction(actionId);
		auditLogService.record(action.decidedAt(), action.decidedBy(), type, action.incidentId(), action.id(), null);
	}

	private void recordSafely(Runnable recording) {
		try {
			recording.run();
		}
		catch (RuntimeException ex) {
			log.error("Failed to write audit log", ex);
		}
	}
}
