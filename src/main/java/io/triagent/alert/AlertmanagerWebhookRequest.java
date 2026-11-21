package io.triagent.alert;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Prometheus Alertmanager 웹훅 요청 본문.
 * <p>
 * 형식: https://prometheus.io/docs/alerting/latest/configuration/#webhook_config
 * 우리가 쓰지 않는 필드(groupKey, receiver 등)는 Jackson이 무시합니다.
 */
public record AlertmanagerWebhookRequest(
		@NotBlank String status,
		Map<String, String> commonLabels,
		@NotEmpty List<@Valid Alert> alerts) {

	public record Alert(
			@NotBlank String status,
			@NotBlank String fingerprint,
			@NotNull Map<String, String> labels,
			Map<String, String> annotations,
			@NotNull Instant startsAt,
			Instant endsAt) {

		public boolean isFiring() {
			return "firing".equalsIgnoreCase(status);
		}

		public String label(String key, String defaultValue) {
			return labels.getOrDefault(key, defaultValue);
		}

		public String annotation(String key) {
			return annotations == null ? null : annotations.get(key);
		}
	}
}
