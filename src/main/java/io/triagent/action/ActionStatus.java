package io.triagent.action;

public enum ActionStatus {
	/** 요청되어 승인을 기다리는 상태 */
	PENDING,
	/** 승인되어 실행을 기다리는 상태 */
	APPROVED,
	/** 거절되어 실행하지 않는 상태 */
	REJECTED,
	/** 실행에 성공한 상태 */
	EXECUTED,
	/** 실행했지만 실패한 상태 */
	FAILED
}
