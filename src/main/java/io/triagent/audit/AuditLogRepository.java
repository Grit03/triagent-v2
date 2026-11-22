package io.triagent.audit;

import java.util.List;
import org.springframework.data.repository.Repository;

/** 감사 로그를 수정, 삭제할 수 없도록 JpaRepository 대신 필요한 메서드만 고를 수 있는 Repository를 상속합니다. */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByIncidentIdOrderByOccurredAtAscIdAsc(Long incidentId);
}
