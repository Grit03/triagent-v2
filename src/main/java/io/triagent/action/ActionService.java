package io.triagent.action;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.triagent.incident.IncidentNotFoundException;
import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;

@Service
@Transactional(readOnly = true)
public class ActionService {

	private static final Logger log = LoggerFactory.getLogger(ActionService.class);

	private final RemediationActionRepository actionRepository;

	private final IncidentService incidentService;

	private final ApplicationEventPublisher eventPublisher;

	public ActionService(RemediationActionRepository actionRepository, IncidentService incidentService,
			ApplicationEventPublisher eventPublisher) {
		this.actionRepository = actionRepository;
		this.incidentService = incidentService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 조치를 승인 대기 상태로 저장합니다. 요청만으로는 아무것도 실행되지 않습니다.
	 */
	@Transactional
	public ActionResponse request(Long incidentId, CreateActionRequest request) {
		IncidentSnapshot incident = incidentService.findSnapshot(incidentId)
				.orElseThrow(() -> new IncidentNotFoundException(incidentId));
		RemediationAction action = RemediationAction.request(incidentId, incident.service(), request.type(),
				request.reason(), request.requestedBy(), Instant.now());
		RemediationAction saved = actionRepository.save(action);
		log.info("Action requested: id={}, incidentId={}, type={}, service={}, requestedBy={}", saved.getId(),
				incidentId, saved.getType(), saved.getService(), saved.getRequestedBy());
		return ActionResponse.from(saved);
	}

	/**
	 * 승인만 기록하고 실행은 커밋 이후 별도 스레드에서 합니다. 실행이 오래 걸려도 승인 요청은 바로 끝납니다.
	 */
	@Transactional
	public ActionResponse approve(Long actionId, String approver) {
		RemediationAction action = findAction(actionId);
		action.approve(approver, Instant.now());
		log.info("Action approved: id={}, approver={}", actionId, approver);
		eventPublisher.publishEvent(new ActionApprovedEvent(actionId));
		return ActionResponse.from(action);
	}

	@Transactional
	public ActionResponse reject(Long actionId, String rejecter) {
		RemediationAction action = findAction(actionId);
		action.reject(rejecter, Instant.now());
		log.info("Action rejected: id={}, rejecter={}", actionId, rejecter);
		return ActionResponse.from(action);
	}

	@Transactional
	public void recordExecuted(Long actionId, String result) {
		findAction(actionId).markExecuted(result, Instant.now());
		log.info("Action executed: id={}", actionId);
	}

	@Transactional
	public void recordFailed(Long actionId, String reason) {
		findAction(actionId).markFailed(reason, Instant.now());
		log.warn("Action failed: id={}, reason={}", actionId, reason);
	}

	public ActionResponse getAction(Long actionId) {
		return ActionResponse.from(findAction(actionId));
	}

	private RemediationAction findAction(Long actionId) {
		return actionRepository.findById(actionId).orElseThrow(() -> new ActionNotFoundException(actionId));
	}
}
