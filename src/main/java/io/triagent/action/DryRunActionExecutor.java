package io.triagent.action;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DryRunActionExecutor implements ActionExecutor {

	@Override
	public String execute(ActionType type, String service) {
		log.info("[dry-run] {} {}", type, service);
		return "드라이런: %s에 %s를 실행했다고 가정했습니다. 실제 인프라는 바뀌지 않았습니다.".formatted(service, type);
	}
}
