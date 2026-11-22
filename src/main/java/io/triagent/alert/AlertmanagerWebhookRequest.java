package io.triagent.alert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

        public String service() {
            String job = labels.getOrDefault("job", "unknown");
            return labels.getOrDefault("service", job);
        }

        public String severity() {
            return labels.getOrDefault("severity", "none");
        }

        public String summary() {
            if (annotations == null) {
                return null;
            }
            String description = annotations.get("description");
            return annotations.getOrDefault("summary", description);
        }

        public Instant resolvedAt() {
            return endsAt != null ? endsAt : Instant.now();
        }
    }
}
