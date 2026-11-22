package io.triagent.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("triagent.analysis.anthropic")
public record AnthropicProperties(
        String apiKey,
        @DefaultValue("claude-opus-5-5") String model,
        @DefaultValue("16000") long maxTokens,
        @DefaultValue("medium") String effort) {}
