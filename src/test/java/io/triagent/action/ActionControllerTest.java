package io.triagent.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ActionControllerTest {

	@Autowired
	ApplicationEvents events;

	@Autowired
	MockMvcTester mvc;

	@Autowired
	IncidentService incidentService;

	Long incidentId;

	@BeforeEach
	void openIncident() {
		String fingerprint = "action-test-" + System.nanoTime();
		incidentId = incidentService.openIfAbsent(new OpenIncidentCommand(fingerprint, "HighErrorRate", "payment-api",
				"critical", "5xx > 5%", Instant.parse("2026-11-21T01:00:00Z"))).orElseThrow();
	}

	@Test
	void requestedActionIsSavedAsPending() {
		MvcTestResult result = requestAction(incidentId, """
				{ "type": "ROLLBACK", "requestedBy": "U_ONCALL", "reason": "a1b2c3d 배포 후 5xx 증가" }
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result.getResponse().getHeader("Location")).matches(".*/api/v1/actions/\\d+");
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PENDING");
		assertThat(result).bodyJson().extractingPath("$.service").isEqualTo("payment-api");

		String location = result.getResponse().getHeader("Location");
		assertThat(mvc.get().uri(location)).hasStatusOk().bodyJson().extractingPath("$.type").isEqualTo("ROLLBACK");
		assertThat(events.stream(ActionRequestedEvent.class)).hasSize(1);
	}

	@Test
	void requestForUnknownIncidentReturnsNotFound() {
		assertThat(requestAction(9999L, """
				{ "type": "RESTART", "requestedBy": "U_ONCALL" }
				""")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void requestWithUnknownTypeIsRejected() {
		assertThat(requestAction(incidentId, """
				{ "type": "DELETE_DATABASE", "requestedBy": "U_ONCALL" }
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private MvcTestResult requestAction(Long incidentId, String body) {
		return mvc.post().uri("/api/v1/incidents/{id}/actions", incidentId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}
}
