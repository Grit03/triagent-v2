package io.triagent.action;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * 인프라를 바꾸지 않고 실행할 내용만 기록하는 실행기. 실제 실행기가 없는 2단계와 로컬 개발에서 씁니다.
 * <p>
 * 결과 메시지에 "드라이런"을 명시해, 실제로 실행된 것처럼 보이지 않게 합니다.
 */
@Component
@Slf4j
public class DryRunActionExecutor implements ActionExecutor {

	@Override
	public String execute(ActionType type, String service) {
		log.info("[dry-run] {} {}", type, service);
		return "드라이런: %s에 %s를 실행했다고 가정했습니다. 실제 인프라는 바뀌지 않았습니다.".formatted(service, type);
	}
}
