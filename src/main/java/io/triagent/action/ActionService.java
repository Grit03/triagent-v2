package io.triagent.action;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public ActionService(RemediationActionRepository actionRepository, IncidentService incidentService) {
		this.actionRepository = actionRepository;
		this.incidentService = incidentService;
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

	public ActionResponse getAction(Long actionId) {
		return ActionResponse.from(findAction(actionId));
	}

	private RemediationAction findAction(Long actionId) {
		return actionRepository.findById(actionId).orElseThrow(() -> new ActionNotFoundException(actionId));
	}
}
