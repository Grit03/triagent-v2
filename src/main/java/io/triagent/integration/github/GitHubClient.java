package io.triagent.integration.github;

import java.time.Instant;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {

	private static final int MAX_COMMITS = 10;

	private final RestClient restClient;

	public GitHubClient(RestClient.Builder restClientBuilder, GitHubProperties properties) {
		RestClient.Builder builder = restClientBuilder.baseUrl(properties.baseUrl().toString())
				.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
				.defaultHeader("X-GitHub-Api-Version", "2022-11-28");
		if (properties.hasToken()) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token());
		}
		this.restClient = builder.build();
	}

	public List<GitHubCommit> listCommits(String repository, Instant since, Instant until) {
		String[] ownerAndRepo = repository.split("/", 2);
		List<GitHubCommit> commits = restClient.get()
				.uri(uri -> uri.path("/repos/{owner}/{repo}/commits")
						.queryParam("since", since)
						.queryParam("until", until)
						.queryParam("per_page", MAX_COMMITS)
						.build(ownerAndRepo[0], ownerAndRepo[1]))
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		return commits == null ? List.of() : commits;
	}
}
