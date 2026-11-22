package io.triagent.alert;

import static org.assertj.core.api.Assertions.assertThat;

import io.triagent.incident.IncidentRepository;
import io.triagent.incident.IncidentStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
class AlertWebhookControllerTest {

    @Autowired MockMvcTester mvc;

    @Autowired IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
    }

    @Test
    @DisplayName("firing 알림을 받으면 장애를 연다")
    void openIncident() throws IOException {
        assertThat(post(fixture("firing.json")))
                .hasStatus(HttpStatus.ACCEPTED)
                .bodyJson()
                .extractingPath("$.openedIncidentIds.length()")
                .isEqualTo(1);

        assertThat(incidentRepository.findAll())
                .singleElement()
                .satisfies(
                        incident -> {
                            assertThat(incident.getAlertName()).isEqualTo("HighErrorRate");
                            assertThat(incident.getService()).isEqualTo("payment-api");
                            assertThat(incident.getSeverity()).isEqualTo("critical");
                            assertThat(incident.getSummary())
                                    .isEqualTo("5xx error rate is above 5%");
                            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
                        });
    }

    @Test
    @DisplayName("같은 firing 알림이 다시 와도 장애를 중복 생성하지 않는다")
    void ignoreDuplicate() throws IOException {
        post(fixture("firing.json"));

        assertThat(post(fixture("firing.json")))
                .hasStatus(HttpStatus.ACCEPTED)
                .bodyJson()
                .extractingPath("$.openedIncidentIds.length()")
                .isEqualTo(0);
        assertThat(incidentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("resolved 알림을 받으면 열린 장애를 해소한다")
    void resolveIncident() throws IOException {
        post(fixture("firing.json"));

        assertThat(post(fixture("resolved.json")))
                .hasStatus(HttpStatus.ACCEPTED)
                .bodyJson()
                .extractingPath("$.resolvedIncidentIds.length()")
                .isEqualTo(1);

        assertThat(incidentRepository.findAll())
                .singleElement()
                .satisfies(
                        incident -> {
                            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
                            assertThat(incident.getResolvedAt())
                                    .hasToString("2026-11-21T01:30:00Z");
                        });
    }

    @Test
    @DisplayName("알림이 없는 요청은 400으로 거부한다")
    void emptyAlerts() {
        assertThat(
                        post(
                                """
				{ "status": "firing", "alerts": [] }
				"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(incidentRepository.count()).isZero();
    }

    @Test
    @DisplayName("열린 장애를 조회한다")
    void getIncident() throws IOException {
        post(fixture("firing.json"));
        Long id = incidentRepository.findAll().get(0).getId();

        assertThat(mvc.get().uri("/api/v1/incidents/{id}", id))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.service")
                .isEqualTo("payment-api");
    }

    @Test
    @DisplayName("없는 장애를 조회하면 404 Problem Details를 응답한다")
    void incidentNotFound() {
        assertThat(mvc.get().uri("/api/v1/incidents/{id}", 9999))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Incident not found: 9999");
    }

    private MvcTestResult post(String body) {
        return mvc.post()
                .uri("/api/v1/alerts/alertmanager")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String fixture(String name) throws IOException {
        return new ClassPathResource("alertmanager/" + name)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
