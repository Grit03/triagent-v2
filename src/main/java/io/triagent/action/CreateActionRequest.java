package io.triagent.action;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateActionRequest(
		@NotNull ActionType type,
		@Size(max = 1000) String reason) {
}
