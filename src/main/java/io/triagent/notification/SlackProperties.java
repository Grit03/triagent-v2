package io.triagent.notification;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code triagent.notification.slack.*} 설정을 타입 있는 객체로 묶습니다.
 */
@ConfigurationProperties("triagent.notification.slack")
public record SlackProperties(URI webhookUrl) {
}
