package io.triagent.action;

import io.triagent.access.ServiceAccessPolicy;
import io.triagent.incident.IncidentNotFoundException;
import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ActionService {

    private final RemediationActionRepository actionRepository;

    private final IncidentService incidentService;

    private final ServiceAccessPolicy accessPolicy;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ActionResponse request(
            Long incidentId, CreateActionRequest request, String requestedBy) {
        IncidentSnapshot incident =
                incidentService
                        .findSnapshot(incidentId)
                        .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        RemediationAction action =
                RemediationAction.request(
                        incidentId,
                        incident.service(),
                        request.type(),
                        request.reason(),
                        requestedBy,
                        Instant.now());
        RemediationAction saved = actionRepository.save(action);
        log.info(
                "Action requested: id={}, incidentId={}, type={}, service={}, requestedBy={}",
                saved.getId(),
                incidentId,
                saved.getType(),
                saved.getService(),
                saved.getRequestedBy());
        eventPublisher.publishEvent(new ActionRequestedEvent(saved.getId()));
        return ActionResponse.from(saved);
    }

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

    /** 예외로 트랜잭션이 롤백되어도 거부 사실이 감사 로그에 남도록, 거부 이벤트를 발행한 뒤 예외를 다시 던집니다. */
    private void decideOrPublishDenial(RemediationAction action, String userId, Runnable decision) {
        try {
            checkCanDecide(userId, action);
            decision.run();
        } catch (DecisionNotAllowedException ex) {
            eventPublisher.publishEvent(
                    new ActionDecisionDeniedEvent(
                            action.getId(), userId, ex.getMessage(), Instant.now()));
            throw ex;
        }
    }

    private void checkCanDecide(String userId, RemediationAction action) {
        if (!accessPolicy.canDecide(userId, action.getService())) {
            throw new DecisionNotAllowedException(
                    "%s is not a member of the team owning %s"
                            .formatted(userId, action.getService()));
        }
    }

    private RemediationAction findAction(Long actionId) {
        return actionRepository
                .findById(actionId)
                .orElseThrow(() -> new ActionNotFoundException(actionId));
    }
}
