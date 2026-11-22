package io.triagent.analysis;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.triagent.action.ActionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${triagent.analysis.anthropic.api-key:}' != ''")
public class AnthropicConfig {

    @Bean
    AnthropicClient anthropicClient(AnthropicProperties properties) {
        return AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build();
    }

    @Bean
    ClaudeIncidentAnalyzer claudeIncidentAnalyzer(
            AnthropicClient anthropicClient,
            AnthropicProperties properties,
            ActionService actionService) {
        return new ClaudeIncidentAnalyzer(anthropicClient, properties, actionService);
    }
}
