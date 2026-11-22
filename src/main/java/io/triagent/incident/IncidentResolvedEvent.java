package io.triagent.incident;

import java.time.Instant;

/**
 * @param resolvedAt 장애를 해소한 트랜잭션 안에서 잡은 시각. 리스너는 커밋 이후에 실행되므로 리스너에서 시각을 잡으면 늦어집니다.
 */
public record IncidentResolvedEvent(Long incidentId, Instant resolvedAt) {
}
