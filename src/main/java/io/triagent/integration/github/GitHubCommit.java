package io.triagent.integration.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub "List commits" 응답에서 필요한 필드만 옮긴 DTO.
 * https://docs.github.com/en/rest/commits/commits#list-commits
 */
public record GitHubCommit(String sha, @JsonProperty("html_url") String htmlUrl, Detail commit) {

	public record Detail(String message, Author author) {
	}

	public record Author(String name, Instant date) {
	}

	public String shortSha() {
		return sha.length() > 7 ? sha.substring(0, 7) : sha;
	}

	/** 커밋 메시지의 첫 줄(제목)만 돌려줍니다. */
	public String title() {
		String message = commit.message();
		int newline = message.indexOf('\n');
		return newline < 0 ? message : message.substring(0, newline);
	}
}
