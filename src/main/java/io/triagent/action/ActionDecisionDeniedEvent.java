package io.triagent.action;

import java.time.Instant;

/**
 * 권한이 없어 승인, 거절이 거부되었을 때 발행합니다. 결정 트랜잭션은 롤백되므로 AFTER_ROLLBACK 단계에서 받아야 합니다.
 *
 * @param deniedAt 거부한 시각. 롤백되어 엔티티에 남지 않으므로 이벤트에 담아 넘깁니다.
 */
public record ActionDecisionDeniedEvent(Long actionId, String userId, String reason, Instant deniedAt) {
}
