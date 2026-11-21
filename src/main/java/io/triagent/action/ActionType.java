package io.triagent.action;

/**
 * 시스템 상태를 바꾸는 조치의 종류. 모두 사람의 승인을 받아야 실행됩니다.
 */
public enum ActionType {
	/** 직전 버전으로 되돌리기 */
	ROLLBACK,
	/** 현재 버전을 다시 배포하기 */
	REDEPLOY,
	/** 서버(파드) 재시작 */
	RESTART
}
