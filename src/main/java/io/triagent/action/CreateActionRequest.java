package io.triagent.action;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 조치 요청 본문.
 * <p>
 * 요청자는 본문으로 받지 않습니다. 클라이언트가 적은 값은 검증할 수 없어서, 남의 이름이나 에이전트 이름으로 요청하고
 * 자기가 승인하면 "요청자는 자기 요청을 승인할 수 없다"는 규칙과 감사 로그의 요청자가 모두 거짓이 됩니다.
 * 요청자는 호출 경로를 아는 서버 쪽 코드가 {@link ActionService#request}에 따로 넘깁니다.
 */
public record CreateActionRequest(
		@NotNull ActionType type,
		@Size(max = 1000) String reason) {
}
