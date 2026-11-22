package io.triagent.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("조치를 요청하면 승인 대기로 저장한다")
	void request() {
		MvcTestResult result = requestAction(incidentId, """
				{ "type": "ROLLBACK", "reason": "a1b2c3d 배포 후 5xx 증가" }
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result.getResponse().getHeader("Location")).matches(".*/api/v1/actions/\\d+");
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PENDING");
		assertThat(result).bodyJson().extractingPath("$.service").isEqualTo("payment-api");
		assertThat(result).bodyJson().extractingPath("$.requestedBy").isEqualTo("api");

		String location = result.getResponse().getHeader("Location");
		assertThat(mvc.get().uri(location)).hasStatusOk().bodyJson().extractingPath("$.type").isEqualTo("ROLLBACK");
		assertThat(events.stream(ActionRequestedEvent.class)).hasSize(1);
	}

	/** 남의 이름으로 요청하고 자기가 승인하는 우회를 막기 위해서입니다. */
	@Test
	@DisplayName("본문에 적은 요청자는 무시하고 api로 기록한다")
	void ignoreRequester() {
		MvcTestResult result = requestAction(incidentId, """
				{ "type": "RESTART", "requestedBy": "triagent-agent" }
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.requestedBy").isEqualTo("api");
	}

	@Test
	@DisplayName("없는 장애에 조치를 요청하면 404를 응답한다")
	void incidentNotFound() {
		assertThat(requestAction(9999L, """
				{ "type": "RESTART" }
				""")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	@DisplayName("알 수 없는 조치 종류는 400으로 거부한다")
	void unknownType() {
		assertThat(requestAction(incidentId, """
				{ "type": "DELETE_DATABASE" }
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private MvcTestResult requestAction(Long incidentId, String body) {
		return mvc.post().uri("/api/v1/incidents/{id}/actions", incidentId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}
}
