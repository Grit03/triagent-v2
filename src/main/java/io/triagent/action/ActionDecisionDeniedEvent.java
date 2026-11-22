package io.triagent.action;

import java.time.Instant;

/**
 * @param deniedAt 결정 트랜잭션이 롤백되어 엔티티에 남지 않으므로 이벤트에 담아 넘깁니다.
 */
public record ActionDecisionDeniedEvent(
        Long actionId, String userId, String reason, Instant deniedAt) {}
