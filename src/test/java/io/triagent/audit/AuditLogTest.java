package io.triagent.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.triagent.action.ActionService;
import io.triagent.action.ActionType;
import io.triagent.action.CreateActionRequest;
import io.triagent.action.DecisionNotAllowedException;
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

	@Test
	void incidentLifecycleAndActionRequestAreAudited() {
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
	void decisionsDenialsAndExecutionResultAreAudited() throws InterruptedException {
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
	void rejectionIsAudited() {
		Long incidentId = openIncident();
		Long actionId = actionService.request(incidentId,
				new CreateActionRequest(ActionType.REDEPLOY, null), "U_ONCALL").id();

		actionService.reject(actionId, "U_ONCALL");

		assertThat(auditLogService.findByIncident(incidentId)).last()
				.satisfies(log -> assertThat(log.type()).isEqualTo(AuditEventType.ACTION_REJECTED))
				.satisfies(log -> assertThat(log.actor()).isEqualTo("U_ONCALL"));
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
	void auditLogsCannotBeDeletedThroughApi() {
		assertThat(mvc.delete().uri("/api/v1/audit-logs?incidentId=1")).hasStatus4xxClientError();
	}
}
