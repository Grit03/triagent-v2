package io.triagent.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.triagent.action.ActionService;
import io.triagent.action.ActionType;
import io.triagent.action.CreateActionRequest;
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

	@Test
	void incidentLifecycleAndActionRequestAreAudited() {
		String fingerprint = "audit-" + System.nanoTime();
		Long incidentId = incidentService.openIfAbsent(new OpenIncidentCommand(fingerprint, "HighErrorRate",
				"payment-api", "critical", null, Instant.parse("2026-11-21T01:00:00Z"))).orElseThrow();
		actionService.request(incidentId, new CreateActionRequest(ActionType.ROLLBACK, "U_ONCALL", "5xx 증가"));
		incidentService.resolveIfOpen(fingerprint, Instant.parse("2026-11-21T01:30:00Z"));

		var logs = assertThat(mvc.get().uri("/api/v1/audit-logs?incidentId={id}", incidentId)).hasStatusOk().bodyJson();
		logs.extractingPath("$[*].type").asArray()
				.containsExactly("INCIDENT_OPENED", "ACTION_REQUESTED", "INCIDENT_RESOLVED");
		logs.extractingPath("$[*].actor").asArray().containsExactly("alertmanager", "U_ONCALL", "alertmanager");
		logs.extractingPath("$[0].detail").isEqualTo("HighErrorRate (payment-api, critical)");
		logs.extractingPath("$[1].detail").isEqualTo("ROLLBACK payment-api: 5xx 증가");
	}

	@Test
	void auditLogsCannotBeDeletedThroughApi() {
		assertThat(mvc.delete().uri("/api/v1/audit-logs?incidentId=1")).hasStatus4xxClientError();
	}
}
