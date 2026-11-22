package io.triagent.access;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("triagent.access")
public record AccessProperties(Map<String, Team> teams) {

	public AccessProperties {
		teams = teams != null ? Map.copyOf(teams) : Map.of();
	}

	public record Team(Set<String> members, Set<String> services) {

		public Team {
			members = members != null ? Set.copyOf(members) : Set.of();
			services = services != null ? Set.copyOf(services) : Set.of();
		}
	}
}
