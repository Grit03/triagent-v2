package io.triagent.access;

import org.springframework.stereotype.Component;

/**
 * 누가 어떤 서비스의 조치를 결정(승인, 거절)할 수 있는지 판단합니다.
 * <p>
 * 서비스를 담당하는 팀이 설정되어 있지 않으면 아무도 결정할 수 없습니다. 설정을 빠뜨렸을 때 누구나 승인할 수 있게 되는 것보다 안전하기 때문입니다.
 */
@Component
public class ServiceAccessPolicy {

	private final AccessProperties properties;

	public ServiceAccessPolicy(AccessProperties properties) {
		this.properties = properties;
	}

	public boolean canDecide(String userId, String service) {
		return properties.teams().values().stream()
				.filter(team -> team.services().contains(service))
				.anyMatch(team -> team.members().contains(userId));
	}
}
