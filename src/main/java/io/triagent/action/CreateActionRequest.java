package io.triagent.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 조치 요청 본문.
 *
 * @param requestedBy 요청한 사람 또는 시스템(예: Slack 사용자 ID, "triagent-agent")
 */
public record CreateActionRequest(
		@NotNull ActionType type,
		@NotBlank @Size(max = 255) String requestedBy,
		@Size(max = 1000) String reason) {
}
