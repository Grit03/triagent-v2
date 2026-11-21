package io.triagent.integration.github;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.triagent.incident.Incident;
import io.triagent.investigation.ContextCollector;
import io.triagent.investigation.Evidence;

/**
 * 장애 직전에 들어간 커밋을 배포 후보로 보고 근거로 수집합니다.
 * 장애 원인의 상당수가 최근 변경에서 나오기 때문에, 당직자가 가장 먼저 확인하는 정보입니다.
 */
@Component
public class GitHubDeploymentCollector implements ContextCollector {

	static final String SOURCE = "github";

	private final GitHubClient gitHubClient;

	private final GitHubProperties properties;

	public GitHubDeploymentCollector(GitHubClient gitHubClient, GitHubProperties properties) {
		this.gitHubClient = gitHubClient;
		this.properties = properties;
	}

	@Override
	public String source() {
		return SOURCE;
	}

	@Override
	public Evidence collect(Incident incident) {
		String repository = properties.repositories().get(incident.getService());
		if (repository == null) {
			return Evidence.failure(SOURCE, "서비스 '%s'에 연결된 저장소가 없습니다.".formatted(incident.getService()));
		}
		Instant until = incident.getStartedAt();
		Instant since = until.minus(properties.lookback());
		List<GitHubCommit> commits = gitHubClient.listCommits(repository, since, until);
		long hours = properties.lookback().toHours();
		if (commits.isEmpty()) {
			return Evidence.success(SOURCE, "%s: 장애 전 %d시간 동안 커밋이 없습니다.".formatted(repository, hours));
		}
		String lines = commits.stream()
				.map(c -> "- %s %s (%s, %s)".formatted(c.shortSha(), c.title(), c.commit().author().name(),
						c.commit().author().date()))
				.collect(Collectors.joining("\n"));
		return Evidence.success(SOURCE,
				"%s: 장애 전 %d시간 동안 커밋 %d건\n%s".formatted(repository, hours, commits.size(), lines));
	}
}
