package io.triagent.notification;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("triagent.notification.slack")
public record SlackProperties(URI webhookUrl, String signingSecret) {
}
