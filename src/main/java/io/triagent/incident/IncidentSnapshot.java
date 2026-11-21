package io.triagent.incident;

import java.time.Instant;

/**
 * 다른 패키지(조사, 분석, 알림)에 넘기는 장애 정보의 읽기 전용 복사본.
 * <p>
 * JPA 엔티티를 트랜잭션 밖으로 넘기면 지연 로딩 예외가 나거나 실수로 상태를 바꿀 수 있어, 필요한 값만 복사해 넘깁니다.
 * HTTP 응답 스펙인 {@link IncidentResponse}와 분리해 두면 API가 바뀌어도 내부 코드가 흔들리지 않습니다.
 */
public record IncidentSnapshot(Long id, String alertName, String service, String severity, String summary,
		Instant startedAt) {

	static IncidentSnapshot from(Incident incident) {
		return new IncidentSnapshot(incident.getId(), incident.getAlertName(), incident.getService(),
				incident.getSeverity(), incident.getSummary(), incident.getStartedAt());
	}
}
