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
}
