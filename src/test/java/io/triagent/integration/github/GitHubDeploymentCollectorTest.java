package io.triagent.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;

class GitHubDeploymentCollectorTest {

	static final Instant STARTED_AT = Instant.parse("2026-11-21T01:00:00Z");

	GitHubClient client = mock(GitHubClient.class);

	GitHubDeploymentCollector collector = new GitHubDeploymentCollector(client,
			new GitHubProperties(URI.create("https://api.github.com"), null, Duration.ofHours(6),
					Map.of("payment-api", "acme/payment-api")));

	@Test
	void summarizesRecentCommitsBeforeIncident() {
		given(client.listCommits("acme/payment-api", STARTED_AT.minus(Duration.ofHours(6)), STARTED_AT))
				.willReturn(List.of(new GitHubCommit("a1b2c3d4e5", "url", new GitHubCommit.Detail(
						"fix: 결제 타임아웃 조정", new GitHubCommit.Author("kim", Instant.parse("2026-11-21T00:40:00Z"))))));

		Evidence evidence = collector.collect(incident("payment-api"));

		assertThat(evidence.success()).isTrue();
		assertThat(evidence.content()).isEqualTo("""
				acme/payment-api: 장애 전 6시간 동안 커밋 1건
				- a1b2c3d fix: 결제 타임아웃 조정 (kim, 2026-11-21T00:40:00Z)""");
	}

	@Test
	void reportsMissingRepositoryMappingWithoutCallingGitHub() {
		Evidence evidence = collector.collect(incident("order-api"));

		assertThat(evidence).isEqualTo(Evidence.failure("github", "서비스 'order-api'에 연결된 저장소가 없습니다."));
		verifyNoInteractions(client);
	}

	private static IncidentSnapshot incident(String service) {
		return new IncidentSnapshot(1L, "HighErrorRate", service, "critical", null, STARTED_AT);
	}
}
