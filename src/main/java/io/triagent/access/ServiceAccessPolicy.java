package io.triagent.access;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/** 담당 팀이 설정되지 않은 서비스는 아무도 결정할 수 없습니다. 설정 누락으로 누구나 승인하게 되는 것을 막기 위해서입니다. */
@Component
@RequiredArgsConstructor
public class ServiceAccessPolicy {

	private final AccessProperties properties;

	public boolean canDecide(String userId, String service) {
		return properties.teams().values().stream()
				.filter(team -> team.services().contains(service))
				.anyMatch(team -> team.members().contains(userId));
	}
}
