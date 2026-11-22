package io.triagent.incident;

import java.time.Instant;

public record IncidentSnapshot(Long id, String alertName, String service, String severity, String summary,
		Instant startedAt) {

	static IncidentSnapshot from(Incident incident) {
		return new IncidentSnapshot(incident.getId(), incident.getAlertName(), incident.getService(),
				incident.getSeverity(), incident.getSummary(), incident.getStartedAt());
	}
}
