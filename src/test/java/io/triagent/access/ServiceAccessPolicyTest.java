package io.triagent.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.triagent.access.AccessProperties.Team;

class ServiceAccessPolicyTest {

	ServiceAccessPolicy policy = new ServiceAccessPolicy(new AccessProperties(Map.of(
			"payments", new Team(Set.of("U_LEAD"), Set.of("payment-api")),
			"orders", new Team(Set.of("U_ORDER"), Set.of("order-api")))));

	@Test
	void memberOfOwningTeamCanDecide() {
		assertThat(policy.canDecide("U_LEAD", "payment-api")).isTrue();
	}

	@Test
	void memberOfAnotherTeamCannotDecide() {
		assertThat(policy.canDecide("U_ORDER", "payment-api")).isFalse();
	}

	@Test
	void serviceWithoutOwningTeamCannotBeDecidedByAnyone() {
		assertThat(policy.canDecide("U_LEAD", "unknown-api")).isFalse();
	}

	/** YAML의 팀 설정이 AccessProperties에 들어오는지 확인합니다(src/test/resources/config/application.yml). */
	@SpringBootTest
	static class ConfigurationTest {

		@Autowired
		AccessProperties properties;

		@Test
		void teamsAreBoundFromYaml() {
			assertThat(properties.teams().get("payments").services()).containsExactly("payment-api");
		}
	}
}
