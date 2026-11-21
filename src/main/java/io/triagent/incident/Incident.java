package io.triagent.incident;

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
 * 장애 한 건. Alertmanager 알림 하나(fingerprint 기준)가 장애 하나가 됩니다.
 */
@Entity
@Table(name = "incident", indexes = @Index(name = "idx_incident_fingerprint_status", columnList = "fingerprint, status"))
public class Incident {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Alertmanager가 라벨 조합으로 계산한 알림 식별자. 같은 알림의 재전송을 구분하는 데 씁니다. */
	@Column(nullable = false, length = 64)
	private String fingerprint;

	@Column(nullable = false)
	private String alertName;

	@Column(nullable = false)
	private String service;

	@Column(nullable = false, length = 32)
	private String severity;

	@Column(length = 1000)
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private IncidentStatus status;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant resolvedAt;

	/** JPA 스펙상 기본 생성자가 필요합니다. 외부에서 쓰지 못하게 protected로 둡니다. */
	protected Incident() {
	}

	private Incident(String fingerprint, String alertName, String service, String severity, String summary,
			Instant startedAt) {
		this.fingerprint = fingerprint;
		this.alertName = alertName;
		this.service = service;
		this.severity = severity;
		this.summary = summary;
		this.startedAt = startedAt;
		this.status = IncidentStatus.OPEN;
	}

	public static Incident open(String fingerprint, String alertName, String service, String severity,
			String summary, Instant startedAt) {
		return new Incident(fingerprint, alertName, service, severity, summary, startedAt);
	}

	/** 상태 변경은 setter 대신 의미 있는 메서드로만 허용합니다. */
	public void resolve(Instant resolvedAt) {
		if (status == IncidentStatus.RESOLVED) {
			return;
		}
		this.status = IncidentStatus.RESOLVED;
		this.resolvedAt = resolvedAt;
	}

	public Long getId() { return id; }
	public String getFingerprint() { return fingerprint; }
	public String getAlertName() { return alertName; }
	public String getService() { return service; }
	public String getSeverity() { return severity; }
	public String getSummary() { return summary; }
	public IncidentStatus getStatus() { return status; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getResolvedAt() { return resolvedAt; }
}
