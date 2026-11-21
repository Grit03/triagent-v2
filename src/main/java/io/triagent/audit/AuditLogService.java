package io.triagent.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	/**
	 * 업무 트랜잭션이 끝난 뒤(커밋 또는 롤백 후)에 호출되므로, 항상 새 트랜잭션에서 저장합니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(String actor, AuditEventType type, Long incidentId, Long actionId, String detail) {
		auditLogRepository.save(AuditLog.of(Instant.now(), actor, type, incidentId, actionId, detail));
	}

	public List<AuditLogResponse> findByIncident(Long incidentId) {
		return auditLogRepository.findByIncidentIdOrderByOccurredAtAscIdAsc(incidentId).stream()
				.map(AuditLogResponse::from)
				.toList();
	}
}
