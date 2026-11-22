package io.triagent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.triagent.action.ActionDecisionDeniedEvent;
import io.triagent.action.ActionService;
import io.triagent.action.ActionType;
import io.triagent.action.CreateActionRequest;
import io.triagent.action.DecisionNotAllowedException;
import io.triagent.incident.IncidentOpenedEvent;
import io.triagent.incident.IncidentResolvedEvent;
import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

/**
 * 업무 코드가 이벤트만 발행해도 감사 로그가 순서대로 쌓이는지 실제 빈으로 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	IncidentService incidentService;

	@Autowired
	ActionService actionService;

	@Autowired
	AuditLogService auditLogService;

	@Autowired
	AuditEventListener auditEventListener;

	@Test
	@DisplayName("장애 열림, 해소와 조치 요청을 감사 로그에 남긴다")
	void incidentLifecycle() {
		String fingerprint = "audit-" + System.nanoTime();
		Long incidentId = incidentService.openIfAbsent(new OpenIncidentCommand(fingerprint, "HighErrorRate",
				"payment-api", "critical", null, Instant.parse("2026-11-21T01:00:00Z"))).orElseThrow();
		actionService.request(incidentId, new CreateActionRequest(ActionType.ROLLBACK, "5xx 증가"), "U_ONCALL");
		incidentService.resolveIfOpen(fingerprint, Instant.parse("2026-11-21T01:30:00Z"));

		var logs = assertThat(mvc.get().uri("/api/v1/audit-logs?incidentId={id}", incidentId)).hasStatusOk().bodyJson();
		logs.extractingPath("$[*].type").asArray()
				.containsExactly("INCIDENT_OPENED", "ACTION_REQUESTED", "INCIDENT_RESOLVED");
		logs.extractingPath("$[*].actor").asArray().containsExactly("alertmanager", "U_ONCALL", "alertmanager");
		logs.extractingPath("$[0].detail").isEqualTo("HighErrorRate (payment-api, critical)");
		logs.extractingPath("$[1].detail").isEqualTo("ROLLBACK payment-api: 5xx 증가");
	}

	@Test
	@DisplayName("승인, 권한 거부, 실행 결과를 감사 로그에 남긴다")
	void decisionAndExecution() throws InterruptedException {
		Long incidentId = openIncident();
		Long actionId = actionService.request(incidentId,
				new CreateActionRequest(ActionType.RESTART, null), "U_ONCALL").id();

		assertThatThrownBy(() -> actionService.approve(actionId, "U_STRANGER"))
				.isInstanceOf(DecisionNotAllowedException.class);
		actionService.approve(actionId, "U_LEAD");
		awaitAuditCount(incidentId, 5);

		var logs = assertThat(mvc.get().uri("/api/v1/audit-logs?incidentId={id}", incidentId)).bodyJson();
		logs.extractingPath("$[*].type").asArray()
				.containsExactly("INCIDENT_OPENED", "ACTION_REQUESTED", "ACTION_DENIED", "ACTION_APPROVED",
						"ACTION_EXECUTED");
		logs.extractingPath("$[*].actor").asArray()
				.containsExactly("alertmanager", "U_ONCALL", "U_STRANGER", "U_LEAD", "triagent");
		logs.extractingPath("$[2].detail").asString().contains("not a member");
	}

	@Test
	@DisplayName("거절을 감사 로그에 남긴다")
	void rejection() {
		Long incidentId = openIncident();
		Long actionId = actionService.request(incidentId,
				new CreateActionRequest(ActionType.REDEPLOY, null), "U_ONCALL").id();

		actionService.reject(actionId, "U_ONCALL");

		assertThat(auditLogService.findByIncident(incidentId)).last()
				.satisfies(log -> assertThat(log.type()).isEqualTo(AuditEventType.ACTION_REJECTED))
				.satisfies(log -> assertThat(log.actor()).isEqualTo("U_ONCALL"));
	}

	@Test
	@DisplayName("장애 열림, 해소, 권한 거부는 리스너 실행 시각이 아니라 이벤트에 담긴 업무 시각으로 남긴다")
	void usesEventTimestamp() {
		Long incidentId = openIncident();
		Long actionId = actionService.request(incidentId,
				new CreateActionRequest(ActionType.RESTART, null), "U_ONCALL").id();
		Instant openedAt = Instant.parse("2000-01-01T00:00:00Z");
		Instant deniedAt = Instant.parse("2000-01-01T00:10:00Z");
		Instant resolvedAt = Instant.parse("2000-01-01T00:20:00Z");

		// 리스너를 직접 불러, 커밋 이후 실행되는 리스너가 스스로 시각을 잡지 않는지 확인합니다.
		// 실제 흐름에서 남은 로그와 섞이지 않도록 먼 과거 시각을 씁니다.
		auditEventListener.onIncidentOpened(new IncidentOpenedEvent(incidentId, openedAt));
		auditEventListener.onDecisionDenied(new ActionDecisionDeniedEvent(actionId, "U_STRANGER", "denied", deniedAt));
		auditEventListener.onIncidentResolved(new IncidentResolvedEvent(incidentId, resolvedAt));

		assertThat(auditLogService.findByIncident(incidentId))
				.filteredOn(log -> log.occurredAt().isBefore(Instant.parse("2001-01-01T00:00:00Z")))
				.extracting(AuditLogResponse::type, AuditLogResponse::occurredAt)
				.containsExactly(tuple(AuditEventType.INCIDENT_OPENED, openedAt),
						tuple(AuditEventType.ACTION_DENIED, deniedAt),
						tuple(AuditEventType.INCIDENT_RESOLVED, resolvedAt));
	}

	private Long openIncident() {
		return incidentService.openIfAbsent(new OpenIncidentCommand("audit-" + System.nanoTime(), "HighErrorRate",
				"payment-api", "critical", null, Instant.parse("2026-11-21T01:00:00Z"))).orElseThrow();
	}

	/** 실행 결과는 다른 스레드에서 기록되므로 최대 5초 기다립니다. */
	private void awaitAuditCount(Long incidentId, int expected) throws InterruptedException {
		Instant deadline = Instant.now().plusSeconds(5);
		while (auditLogService.findByIncident(incidentId).size() < expected && Instant.now().isBefore(deadline)) {
			Thread.sleep(50);
		}
	}

	@Test
	@DisplayName("API로 감사 로그를 삭제할 수 없다")
	void noDeleteApi() {
		assertThat(mvc.delete().uri("/api/v1/audit-logs?incidentId=1")).hasStatus4xxClientError();
	}
}
