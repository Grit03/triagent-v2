package io.triagent.access;

import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param teams 팀 이름 → 팀 정보
 */
@ConfigurationProperties("triagent.access")
public record AccessProperties(Map<String, Team> teams) {

    public AccessProperties {
        teams = teams != null ? Map.copyOf(teams) : Map.of();
    }

    /**
     * @param members 구성원의 Slack 사용자 ID
     * @param services 이 팀이 담당하는 서비스(알림의 service 라벨 값)
     */
    public record Team(Set<String> members, Set<String> services) {

        public Team {
            members = members != null ? Set.copyOf(members) : Set.of();
            services = services != null ? Set.copyOf(services) : Set.of();
        }
    }
}
