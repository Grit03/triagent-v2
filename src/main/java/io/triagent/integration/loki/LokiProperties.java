package io.triagent.integration.loki;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code triagent.loki.*} 설정.
 *
 * @param baseUrl Loki 주소
 * @param query 에러 로그를 찾는 LogQL 템플릿. {@code %s}에 서비스 이름이 들어갑니다
 * @param lookback 장애 시작 시각 기준으로 얼마나 이전 로그까지 볼지
 * @param limit 가져올 최대 로그 줄 수
 */
@ConfigurationProperties("triagent.loki")
public record LokiProperties(
		URI baseUrl,
		@DefaultValue("{service=\"%s\"} |~ \"(?i)(error|exception)\"") String query,
		@DefaultValue("15m") Duration lookback,
		@DefaultValue("20") int limit) {
}
