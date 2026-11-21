package io.triagent.incident;

/**
 * 새 장애가 열렸을 때 발행하는 이벤트. 엔티티 대신 ID만 담아 트랜잭션 경계 밖에서도 안전하게 다룹니다.
 */
public record IncidentOpenedEvent(Long incidentId) {
}
