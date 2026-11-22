package io.triagent.integration.github;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.ContextCollector;
import io.triagent.investigation.Evidence;

@Component
@RequiredArgsConstructor
public class GitHubDeploymentCollector implements ContextCollector {

	static final String SOURCE = "github";

	private final GitHubClient gitHubClient;

	private final GitHubProperties properties;

	@Override
	public String source() {
		return SOURCE;
	}

	@Override
	public Evidence collect(IncidentSnapshot incident) {
		String repository = properties.repositories().get(incident.service());
		if (repository == null) {
			return Evidence.failure(SOURCE, "서비스 '%s'에 연결된 저장소가 없습니다.".formatted(incident.service()));
		}
		Instant until = incident.startedAt();
		Instant since = until.minus(properties.lookback());
		List<GitHubCommit> commits = gitHubClient.listCommits(repository, since, until);
		long hours = properties.lookback().toHours();
		if (commits.isEmpty()) {
			return Evidence.success(SOURCE, "%s: 장애 전 %d시간 동안 커밋이 없습니다.".formatted(repository, hours));
		}
		String commitLines = commits.stream()
				.map(GitHubCommit::toSummaryLine)
				.collect(Collectors.joining("\n"));
		return Evidence.success(SOURCE,
				"%s: 장애 전 %d시간 동안 커밋 %d건\n%s".formatted(repository, hours, commits.size(), commitLines));
	}
}
