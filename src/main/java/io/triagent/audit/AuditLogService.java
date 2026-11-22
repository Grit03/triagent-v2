package io.triagent.audit;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 업무 트랜잭션이 끝난 뒤(커밋 또는 롤백 후)에 호출되므로, 항상 새 트랜잭션에서 저장합니다.
     *
     * @param occurredAt 일이 일어난 시각. 기록하는 시각이 아니라 업무 데이터의 시각을 넣어, 리스너 실행 순서와 상관없이 정렬이 맞게 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Instant occurredAt,
            String actor,
            AuditEventType type,
            Long incidentId,
            Long actionId,
            String detail) {
        auditLogRepository.save(AuditLog.of(occurredAt, actor, type, incidentId, actionId, detail));
    }

    public List<AuditLogResponse> findByIncident(Long incidentId) {
        return auditLogRepository.findByIncidentIdOrderByOccurredAtAscIdAsc(incidentId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
