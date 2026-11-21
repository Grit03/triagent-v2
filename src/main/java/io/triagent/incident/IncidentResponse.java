package io.triagent.incident;

import java.time.Instant;

/**
 * API 응답용 DTO. 엔티티를 그대로 노출하지 않아 내부 구조가 바뀌어도 API 스펙이 흔들리지 않습니다.
 */
public record IncidentResponse(Long id, String alertName, String service, String severity, String summary,
		IncidentStatus status, Instant startedAt, Instant resolvedAt) {

	public static IncidentResponse from(Incident incident) {
		return new IncidentResponse(incident.getId(), incident.getAlertName(), incident.getService(),
				incident.getSeverity(), incident.getSummary(), incident.getStatus(), incident.getStartedAt(),
				incident.getResolvedAt());
	}
}
