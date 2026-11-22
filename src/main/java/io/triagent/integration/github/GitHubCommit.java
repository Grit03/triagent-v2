package io.triagent.integration.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GitHubCommit(String sha, @JsonProperty("html_url") String htmlUrl, Detail commit) {

    public record Detail(String message, Author author) {}

    public record Author(String name, Instant date) {}

    public String shortSha() {
        return sha.length() > 7 ? sha.substring(0, 7) : sha;
    }

    public String toSummaryLine() {
        Author author = commit.author();
        return "- %s %s (%s, %s)".formatted(shortSha(), title(), author.name(), author.date());
    }

    public String title() {
        String message = commit.message();
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
