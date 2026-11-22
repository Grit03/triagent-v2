package io.triagent.action;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.triagent.access.ServiceAccessPolicy;
import io.triagent.incident.IncidentNotFoundException;
import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ActionService {

	private final RemediationActionRepository actionRepository;

	private final IncidentService incidentService;

	private final ServiceAccessPolicy accessPolicy;

	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 조치를 승인 대기 상태로 저장합니다. 요청만으로는 아무것도 실행되지 않습니다.
	 *
	 * @param requestedBy 서버가 확인한 요청자(예: {@code triagent-agent}). 요청 본문의 값을 그대로 넘기면 안 됩니다.
	 */
	@Transactional
	public ActionResponse request(Long incidentId, CreateActionRequest request, String requestedBy) {
		IncidentSnapshot incident = incidentService.findSnapshot(incidentId)
				.orElseThrow(() -> new IncidentNotFoundException(incidentId));
		RemediationAction action = RemediationAction.request(incidentId, incident.service(), request.type(),
				request.reason(), requestedBy, Instant.now());
		RemediationAction saved = actionRepository.save(action);
		log.info("Action requested: id={}, incidentId={}, type={}, service={}, requestedBy={}", saved.getId(),
				incidentId, saved.getType(), saved.getService(), saved.getRequestedBy());
		eventPublisher.publishEvent(new ActionRequestedEvent(saved.getId()));
		return ActionResponse.from(saved);
	}

	/**
	 * 승인만 기록하고 실행은 커밋 이후 별도 스레드에서 합니다. 실행이 오래 걸려도 승인 요청은 바로 끝납니다.
	 */
	@Transactional
	public ActionResponse approve(Long actionId, String approver) {
		RemediationAction action = findAction(actionId);
		decideOrPublishDenial(action, approver, () -> action.approve(approver, Instant.now()));
		log.info("Action approved: id={}, approver={}", actionId, approver);
		eventPublisher.publishEvent(new ActionApprovedEvent(actionId));
		return ActionResponse.from(action);
	}

	@Transactional
	public ActionResponse reject(Long actionId, String rejecter) {
		RemediationAction action = findAction(actionId);
		decideOrPublishDenial(action, rejecter, () -> action.reject(rejecter, Instant.now()));
		log.info("Action rejected: id={}, rejecter={}", actionId, rejecter);
		eventPublisher.publishEvent(new ActionRejectedEvent(actionId));
		return ActionResponse.from(action);
	}

	@Transactional
	public void recordExecuted(Long actionId, String result) {
		findAction(actionId).markExecuted(result, Instant.now());
		log.info("Action executed: id={}", actionId);
		eventPublisher.publishEvent(new ActionFinishedEvent(actionId));
	}

	@Transactional
	public void recordFailed(Long actionId, String reason) {
		findAction(actionId).markFailed(reason, Instant.now());
		log.warn("Action failed: id={}, reason={}", actionId, reason);
		eventPublisher.publishEvent(new ActionFinishedEvent(actionId));
	}

	public ActionResponse getAction(Long actionId) {
		return ActionResponse.from(findAction(actionId));
	}

	/**
	 * 권한이 없으면 거부 이벤트를 발행하고 예외를 다시 던집니다. 예외로 트랜잭션이 롤백되어도 거부 사실은 감사 로그에 남깁니다.
	 */
	private void decideOrPublishDenial(RemediationAction action, String userId, Runnable decision) {
		try {
			checkCanDecide(userId, action);
			decision.run();
		}
		catch (DecisionNotAllowedException ex) {
			eventPublisher.publishEvent(new ActionDecisionDeniedEvent(action.getId(), userId, ex.getMessage()));
			throw ex;
		}
	}

	private void checkCanDecide(String userId, RemediationAction action) {
		if (!accessPolicy.canDecide(userId, action.getService())) {
			throw new DecisionNotAllowedException(
					"%s is not a member of the team owning %s".formatted(userId, action.getService()));
		}
	}

	private RemediationAction findAction(Long actionId) {
		return actionRepository.findById(actionId).orElseThrow(() -> new ActionNotFoundException(actionId));
	}
}
