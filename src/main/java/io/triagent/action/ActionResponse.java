package io.triagent.action;

import java.time.Instant;

public record ActionResponse(
        Long id,
        Long incidentId,
        String service,
        ActionType type,
        String reason,
        ActionStatus status,
        String requestedBy,
        Instant requestedAt,
        String decidedBy,
        Instant decidedAt,
        String result,
        Instant finishedAt) {

    static ActionResponse from(RemediationAction action) {
        return new ActionResponse(
                action.getId(),
                action.getIncidentId(),
                action.getService(),
                action.getType(),
                action.getReason(),
                action.getStatus(),
                action.getRequestedBy(),
                action.getRequestedAt(),
                action.getDecidedBy(),
                action.getDecidedAt(),
                action.getResult(),
                action.getFinishedAt());
    }
}
