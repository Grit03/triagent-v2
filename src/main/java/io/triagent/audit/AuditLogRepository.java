package io.triagent.audit;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * 저장과 조회만 노출합니다. JpaRepository를 상속하면 delete, save(수정) 같은 메서드가 따라오므로,
 * 필요한 메서드만 고를 수 있는 {@link Repository}를 상속했습니다.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

	AuditLog save(AuditLog auditLog);

	List<AuditLog> findByIncidentIdOrderByOccurredAtAscIdAsc(Long incidentId);
}
