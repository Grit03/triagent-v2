package io.triagent.incident;

import java.time.Instant;

/**
 * 새 장애가 열렸을 때 발행하는 이벤트. 엔티티 대신 ID만 담아 트랜잭션 경계 밖에서도 안전하게 다룹니다.
 *
 * @param openedAt 장애를 연 트랜잭션 안에서 잡은 시각. 리스너는 커밋 이후에 실행되므로 리스너에서 시각을 잡으면 늦어집니다.
 */
public record IncidentOpenedEvent(Long incidentId, Instant openedAt) {
}
