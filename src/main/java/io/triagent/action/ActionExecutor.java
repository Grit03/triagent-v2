package io.triagent.action;

public interface ActionExecutor {

	/**
	 * @return 실행 결과 메시지
	 * @throws RuntimeException 실행에 실패한 경우. 호출하는 쪽이 잡아서 실패로 기록합니다.
	 */
	String execute(ActionType type, String service);
}
