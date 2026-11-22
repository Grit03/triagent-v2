package io.triagent.access;

import static org.assertj.core.api.Assertions.assertThat;

import io.triagent.access.AccessProperties.Team;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

class ServiceAccessPolicyTest {

    ServiceAccessPolicy policy =
            new ServiceAccessPolicy(
                    new AccessProperties(
                            Map.of(
                                    "payments", new Team(Set.of("U_LEAD"), Set.of("payment-api")),
                                    "orders", new Team(Set.of("U_ORDER"), Set.of("order-api")))));

    @Test
    @DisplayName("담당 팀원은 승인, 거절할 수 있다")
    void ownerTeamMember() {
        assertThat(policy.canDecide("U_LEAD", "payment-api")).isTrue();
    }

    @Test
    @DisplayName("다른 팀 구성원은 승인, 거절할 수 없다")
    void otherTeamMember() {
        assertThat(policy.canDecide("U_ORDER", "payment-api")).isFalse();
    }

    @Test
    @DisplayName("담당 팀이 없는 서비스는 아무도 승인, 거절할 수 없다")
    void noOwnerTeam() {
        assertThat(policy.canDecide("U_LEAD", "unknown-api")).isFalse();
    }

    /** YAML의 팀 설정이 AccessProperties에 들어오는지 확인합니다(src/test/resources/config/application.yml). */
    @SpringBootTest
    static class ConfigurationTest {

        @Autowired AccessProperties properties;

        @Test
        @DisplayName("YAML의 팀 설정을 바인딩한다")
        void bindTeams() {
            assertThat(properties.teams().get("payments").services())
                    .containsExactly("payment-api");
        }
    }
}
