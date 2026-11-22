package io.triagent.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubClientTest {

	RestClient.Builder builder = RestClient.builder();

	MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	GitHubClient client = new GitHubClient(builder, new GitHubProperties(URI.create("https://api.github.com"),
			"test-token", Duration.ofHours(24), Map.of()));

	@Test
	@DisplayName("조회 기간 안의 커밋 목록을 가져온다")
	void listCommits() {
		server.expect(requestTo("https://api.github.com/repos/acme/payment-api/commits"
				+ "?since=2026-11-20T01:00:00Z&until=2026-11-21T01:00:00Z&per_page=10"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer test-token"))
				.andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
				.andRespond(withSuccess("""
						[
						  {
						    "sha": "a1b2c3d4e5f6a7b8c9d0",
						    "html_url": "https://github.com/acme/payment-api/commit/a1b2c3d",
						    "commit": {
						      "message": "fix: 결제 타임아웃 조정\\n\\n상세 설명",
						      "author": { "name": "kim", "date": "2026-11-21T00:40:00Z" }
						    }
						  }
						]
						""", MediaType.APPLICATION_JSON));

		List<GitHubCommit> commits = client.listCommits("acme/payment-api", Instant.parse("2026-11-20T01:00:00Z"),
				Instant.parse("2026-11-21T01:00:00Z"));

		server.verify();
		assertThat(commits).singleElement().satisfies(commit -> {
			assertThat(commit.shortSha()).isEqualTo("a1b2c3d");
			assertThat(commit.title()).isEqualTo("fix: 결제 타임아웃 조정");
			assertThat(commit.htmlUrl()).endsWith("/commit/a1b2c3d");
			assertThat(commit.commit().author().date()).isEqualTo(Instant.parse("2026-11-21T00:40:00Z"));
		});
	}
}
