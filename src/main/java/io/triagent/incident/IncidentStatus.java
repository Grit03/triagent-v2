package io.triagent.incident;

public enum IncidentStatus {
	/** 알림이 firing 상태로 들어와 장애가 열린 상태 */
	OPEN,
	/** Alertmanager가 resolved 알림을 보내 장애가 해소된 상태 */
	RESOLVED
}
