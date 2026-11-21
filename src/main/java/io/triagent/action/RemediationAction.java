package io.triagent.action;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 장애를 해결하기 위해 요청된 조치 한 건(롤백, 재배포, 재시작).
 * <p>
 * 장애는 {@code @ManyToOne} 연관관계 대신 ID로만 참조합니다. incident 패키지의 엔티티에 직접 의존하지 않기 위해서입니다.
 */
@Entity
@Table(name = "remediation_action")
public class RemediationAction {

	private static final int MAX_RESULT_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long incidentId;

	/** 조치 대상 서비스. 요청 시점의 장애 서비스를 복사해 둡니다. */
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

	/** 실행 결과 메시지(성공 시 실행 내용, 실패 시 실패 사유) */
	@Column(length = MAX_RESULT_LENGTH)
	private String result;

	private Instant finishedAt;


	protected RemediationAction() {
	}

	private RemediationAction(Long incidentId, String service, ActionType type, String reason, String requestedBy,
			Instant requestedAt) {
		this.incidentId = incidentId;
		this.service = service;
		this.type = type;
		this.reason = reason;
		this.requestedBy = requestedBy;
		this.requestedAt = requestedAt;
		this.status = ActionStatus.PENDING;
	}

	public static RemediationAction request(Long incidentId, String service, ActionType type, String reason,
			String requestedBy, Instant requestedAt) {
		return new RemediationAction(incidentId, service, type, reason, requestedBy, requestedAt);
	}

	/** 요청한 사람은 자기 요청을 승인할 수 없습니다(두 사람 확인 원칙). 거절, 즉 요청 철회는 할 수 있습니다. */
	public void approve(String approver, Instant decidedAt) {
		if (approver.equals(requestedBy)) {
			throw new DecisionNotAllowedException("Requester %s cannot approve their own action".formatted(approver));
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

	/** 승인과 거절은 승인 대기 상태에서 한 번만 할 수 있습니다. */
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
		this.result = result.length() <= MAX_RESULT_LENGTH ? result : result.substring(0, MAX_RESULT_LENGTH);
		this.finishedAt = finishedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getIncidentId() {
		return incidentId;
	}

	public String getService() {
		return service;
	}

	public ActionType getType() {
		return type;
	}

	public String getReason() {
		return reason;
	}

	public ActionStatus getStatus() {
		return status;
	}

	public String getRequestedBy() {
		return requestedBy;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public String getDecidedBy() {
		return decidedBy;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public String getResult() {
		return result;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}
}
