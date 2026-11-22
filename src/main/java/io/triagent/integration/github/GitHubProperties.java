package io.triagent.integration.github;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("triagent.github")
public record GitHubProperties(
		@DefaultValue("https://api.github.com") URI baseUrl,
		String token,
		@DefaultValue("24h") Duration lookback,
		Map<String, String> repositories) {

	public GitHubProperties {
		repositories = repositories == null ? Map.of() : Map.copyOf(repositories);
	}

	public boolean hasToken() {
		return token != null && !token.isBlank();
	}
}
