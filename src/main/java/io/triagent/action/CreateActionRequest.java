package io.triagent.action;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 요청자는 본문으로 받지 않습니다. 클라이언트가 적은 값은 검증할 수 없어, 남의 이름으로 요청하고 자기가 승인하면 "자기 요청은 승인할 수 없다"는 규칙과 감사 로그가 모두
 * 거짓이 됩니다. 요청자는 서버 코드가 정합니다.
 */
public record CreateActionRequest(@NotNull ActionType type, @Size(max = 1000) String reason) {}
