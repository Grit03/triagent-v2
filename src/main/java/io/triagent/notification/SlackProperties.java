package io.triagent.notification;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param webhookUrl 메시지를 보낼 Incoming Webhook URL
 * @param signingSecret 버튼 클릭 요청이 Slack에서 왔는지 검증하는 비밀 값(Slack 앱의 Signing Secret)
 */
@ConfigurationProperties("triagent.notification.slack")
public record SlackProperties(URI webhookUrl, String signingSecret) {}
