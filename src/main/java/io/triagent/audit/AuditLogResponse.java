package io.triagent.audit;

import java.time.Instant;

public record AuditLogResponse(Long id, Instant occurredAt, String actor, AuditEventType type, Long incidentId,
		Long actionId, String detail) {

	static AuditLogResponse from(AuditLog log) {
		return new AuditLogResponse(log.getId(), log.getOccurredAt(), log.getActor(), log.getType(),
				log.getIncidentId(), log.getActionId(), log.getDetail());
	}
}
