package io.triagent.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async} 메서드를 별도 스레드에서 실행하도록 켭니다.
 * 스레드 풀은 Spring Boot가 자동 구성하는 applicationTaskExecutor를 사용합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
