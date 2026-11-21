package io.triagent.audit;

public enum AuditEventType {
	INCIDENT_OPENED,
	INCIDENT_RESOLVED,
	ACTION_REQUESTED,
	ACTION_APPROVED,
	ACTION_REJECTED,
	/** 권한이 없어 승인, 거절이 거부됨 */
	ACTION_DENIED,
	ACTION_EXECUTED,
	ACTION_FAILED
}
