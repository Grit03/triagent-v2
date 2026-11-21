package io.triagent.action;

/**
 * 조치를 결정할 권한이 없을 때 던집니다(담당 팀이 아니거나, 자기 요청을 승인하려는 경우).
 */
public class DecisionNotAllowedException extends RuntimeException {

	public DecisionNotAllowedException(String message) {
		super(message);
	}
}
