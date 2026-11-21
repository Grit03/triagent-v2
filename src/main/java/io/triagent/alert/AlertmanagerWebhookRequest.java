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

		public String alertName() {
			return labels.getOrDefault("alertname", "unknown");
		}

		/** service 라벨이 없으면 Prometheus가 기본으로 붙이는 job 라벨을 씁니다. */
		public String service() {
			String job = labels.getOrDefault("job", "unknown");
			return labels.getOrDefault("service", job);
		}

		public String severity() {
			return labels.getOrDefault("severity", "none");
		}

		/** summary 어노테이션이 없으면 description을 씁니다. */
		public String summary() {
			if (annotations == null) {
				return null;
			}
			String description = annotations.get("description");
			return annotations.getOrDefault("summary", description);
		}

		/** resolved 알림에 endsAt이 없으면 받은 시각을 해소 시각으로 씁니다. */
		public Instant resolvedAt() {
			return endsAt != null ? endsAt : Instant.now();
		}
	}
}
