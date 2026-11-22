package io.triagent.integration.loki;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("triagent.loki")
public record LokiProperties(
		URI baseUrl,
		@DefaultValue("{service=\"%s\"} |~ \"(?i)(error|exception)\"") String query,
		@DefaultValue("15m") Duration lookback,
		@DefaultValue("20") int limit) {
}
