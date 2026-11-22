package io.triagent.incident;

import java.time.Instant;

public record IncidentResponse(Long id, String alertName, String service, String severity, String summary,
		IncidentStatus status, Instant startedAt, Instant resolvedAt) {

	public static IncidentResponse from(Incident incident) {
		return new IncidentResponse(incident.getId(), incident.getAlertName(), incident.getService(),
				incident.getSeverity(), incident.getSummary(), incident.getStatus(), incident.getStartedAt(),
				incident.getResolvedAt());
	}
}
