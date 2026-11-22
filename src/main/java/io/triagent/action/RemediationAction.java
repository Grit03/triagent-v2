package io.triagent.action;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장애는 {@code @ManyToOne} 대신 ID로만 참조합니다. incident 패키지의 엔티티에 직접 의존하지 않기 위해서입니다. */
@Entity
@Table(name = "remediation_action")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemediationAction {

    private static final int MAX_RESULT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActionType type;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActionStatus status;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private Instant requestedAt;

    private String decidedBy;

    private Instant decidedAt;

    @Column(length = MAX_RESULT_LENGTH)
    private String result;

    private Instant finishedAt;

    /**
     * 두 사람이 동시에 승인, 거절하면 둘 다 PENDING을 읽고 결정할 수 있습니다. UPDATE 조건에 버전을 넣어 나중에 커밋하는 쪽이 실패하게 해서, 승인된 조치가
     * 두 번 실행되지 않게 합니다.
     */
    @Version
    @Getter(AccessLevel.NONE)
    private long version;

    private RemediationAction(
            Long incidentId,
            String service,
            ActionType type,
            String reason,
            String requestedBy,
            Instant requestedAt) {
        this.incidentId = incidentId;
        this.service = service;
        this.type = type;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = ActionStatus.PENDING;
    }

    public static RemediationAction request(
            Long incidentId,
            String service,
            ActionType type,
            String reason,
            String requestedBy,
            Instant requestedAt) {
        return new RemediationAction(incidentId, service, type, reason, requestedBy, requestedAt);
    }

    /** 요청한 사람은 자기 요청을 승인할 수 없습니다(두 사람 확인 원칙). 거절, 즉 요청 철회는 할 수 있습니다. */
    public void approve(String approver, Instant decidedAt) {
        if (approver.equals(requestedBy)) {
            throw new DecisionNotAllowedException(
                    "Requester %s cannot approve their own action".formatted(approver));
        }
        decide(ActionStatus.APPROVED, approver, decidedAt);
    }

    public void reject(String rejecter, Instant decidedAt) {
        decide(ActionStatus.REJECTED, rejecter, decidedAt);
    }

    public void markExecuted(String result, Instant finishedAt) {
        finish(ActionStatus.EXECUTED, result, finishedAt);
    }

    public void markFailed(String reason, Instant finishedAt) {
        finish(ActionStatus.FAILED, reason, finishedAt);
    }

    private void decide(ActionStatus decision, String decider, Instant decidedAt) {
        if (status != ActionStatus.PENDING) {
            throw new ActionAlreadyDecidedException(id, status);
        }
        this.status = decision;
        this.decidedBy = decider;
        this.decidedAt = decidedAt;
    }

    /** 실행 결과는 승인된 조치에만 기록할 수 있습니다. 승인 없이 실행된 기록이 남지 않게 합니다. */
    private void finish(ActionStatus outcome, String result, Instant finishedAt) {
        if (status != ActionStatus.APPROVED) {
            throw new IllegalStateException("Action %d is %s, not APPROVED".formatted(id, status));
        }
        this.status = outcome;
        this.result =
                result.length() <= MAX_RESULT_LENGTH
                        ? result
                        : result.substring(0, MAX_RESULT_LENGTH);
        this.finishedAt = finishedAt;
    }
}
