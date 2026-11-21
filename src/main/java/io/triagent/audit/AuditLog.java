package io.triagent.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * "누가, 언제, 무엇을 했는지" 한 줄. 한 번 쓰면 바꾸지 않으므로 상태 변경 메서드가 없습니다.
 */
@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_log_incident", columnList = "incidentId, occurredAt"))
public class AuditLog {

	private static final int MAX_DETAIL_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Instant occurredAt;

	/** 행동한 사람 또는 시스템(예: Slack 사용자 ID, alertmanager) */
	@Column(nullable = false, updatable = false)
	private String actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 32)
	private AuditEventType type;

	@Column(nullable = false, updatable = false)
	private Long incidentId;

	@Column(updatable = false)
	private Long actionId;

	@Column(updatable = false, length = MAX_DETAIL_LENGTH)
	private String detail;

	protected AuditLog() {
	}

	private AuditLog(Instant occurredAt, String actor, AuditEventType type, Long incidentId, Long actionId,
			String detail) {
		this.occurredAt = occurredAt;
		this.actor = actor;
		this.type = type;
		this.incidentId = incidentId;
		this.actionId = actionId;
		this.detail = detail != null && detail.length() > MAX_DETAIL_LENGTH ? detail.substring(0, MAX_DETAIL_LENGTH)
				: detail;
	}

	public static AuditLog of(Instant occurredAt, String actor, AuditEventType type, Long incidentId, Long actionId,
			String detail) {
		return new AuditLog(occurredAt, actor, type, incidentId, actionId, detail);
	}

	public Long getId() {
		return id;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getActor() {
		return actor;
	}

	public AuditEventType getType() {
		return type;
	}

	public Long getIncidentId() {
		return incidentId;
	}

	public Long getActionId() {
		return actionId;
	}

	public String getDetail() {
		return detail;
	}
}
