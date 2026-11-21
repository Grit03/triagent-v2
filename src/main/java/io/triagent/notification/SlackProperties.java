package io.triagent.notification;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code triagent.notification.slack.*} 설정.
 */
@ConfigurationProperties("triagent.notification.slack")
public record SlackProperties(URI webhookUrl) {
}
