package io.triagent.analysis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * API 키가 있을 때만 Claude 클라이언트를 빈으로 등록합니다.
 * AnthropicClient는 AutoCloseable이라 애플리케이션 종료 시 Spring이 close()를 호출해 커넥션을 정리합니다.
 */
@Configuration
@ConditionalOnExpression("'${triagent.analysis.anthropic.api-key:}' != ''")
public class AnthropicConfig {

	@Bean
	AnthropicClient anthropicClient(AnthropicProperties properties) {
		return AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build();
	}

	@Bean
	ClaudeIncidentAnalyzer claudeIncidentAnalyzer(AnthropicClient anthropicClient, AnthropicProperties properties) {
		return new ClaudeIncidentAnalyzer(anthropicClient, properties);
	}
}
