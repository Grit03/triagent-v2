package io.triagent.incident;

import java.time.Instant;

/**
 * @param openedAt 리스너는 커밋 이후에 실행되므로, 리스너에서 시각을 잡으면 늦어져 장애를 연 트랜잭션 안에서 잡습니다.
 */
public record IncidentOpenedEvent(Long incidentId, Instant openedAt) {}
