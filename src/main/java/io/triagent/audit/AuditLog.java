package io.triagent.audit;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_log_incident", columnList = "incidentId, occurredAt"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

	private static final int MAX_DETAIL_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Instant occurredAt;

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
}
