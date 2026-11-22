package io.triagent.integration.github;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl GitHub REST API 주소(GitHub Enterprise를 쓰면 바꿉니다)
 * @param token Personal Access Token. 비어 있으면 인증 없이 호출합니다
 * @param lookback 장애 시작 시각 기준으로 얼마나 이전 커밋까지 볼지
 * @param repositories 서비스 이름 → "owner/repo"
 */
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
