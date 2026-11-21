package io.triagent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.triagent.audit.AuditLogService;
import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

/**
 * 실제 PostgreSQL 컨테이너에서 Flyway 마이그레이션을 적용하고, Hibernate 스키마 검증(ddl-auto: validate)과
 * 저장, 조회가 통과하는지 확인합니다. Docker가 필요합니다(GitHub Actions의 ubuntu 러너에는 기본으로 있습니다).
 */
@SpringBootTest
@Testcontainers
class PostgresMigrationTest {

	/** {@code @ServiceConnection}이 컨테이너의 접속 정보로 spring.datasource.* 를 자동 설정합니다. */
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	Flyway flyway;

	@Autowired
	IncidentService incidentService;

	@Autowired
	AuditLogService auditLogService;

	@Test
	void migrationsApplyAndEntitiesMatchSchema() {
		assertThat(flyway.info().applied()).extracting(migration -> migration.getVersion().getVersion())
				.containsExactly("1");

		Long incidentId = incidentService.openIfAbsent(new OpenIncidentCommand("pg-fingerprint", "HighErrorRate",
				"payment-api", "critical", "5xx > 5%", Instant.parse("2026-11-21T01:00:00Z"))).orElseThrow();

		assertThat(incidentService.findSnapshot(incidentId)).hasValueSatisfying(
				incident -> assertThat(incident.startedAt()).isEqualTo(Instant.parse("2026-11-21T01:00:00Z")));
		assertThat(auditLogService.findByIncident(incidentId)).isNotEmpty();
	}
}
