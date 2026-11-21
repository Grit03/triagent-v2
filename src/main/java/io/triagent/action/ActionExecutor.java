package io.triagent.action;

/**
 * 승인된 조치를 실제 인프라에 적용합니다. 3단계에서 Kubernetes 구현체를 추가합니다.
 */
public interface ActionExecutor {

	/**
	 * @return 실행 결과 메시지
	 * @throws RuntimeException 실행에 실패한 경우
	 */
	String execute(ActionType type, String service);
}
