package io.triagent.investigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.triagent.notification.LoggingNotifier;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** 웹훅 수신부터 보고까지 실제 빈으로 연결된 전체 흐름을 검증합니다. */
@SpringBootTest
@AutoConfigureMockMvc
class InvestigationFlowTest {

    @Autowired MockMvcTester mvc;

    @MockitoSpyBean LoggingNotifier loggingNotifier;

    @Test
    @DisplayName("장애가 열리면 비동기로 조사하고 보고한다")
    void investigate() throws Exception {
        String body =
                new ClassPathResource("alertmanager/firing.json")
                        .getContentAsString(StandardCharsets.UTF_8)
                        .replace("c8a1f2d3e4b5a6f7", "flow-test-fingerprint");

        assertThat(
                        mvc.post()
                                .uri("/api/v1/alerts/alertmanager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .hasStatus(HttpStatus.ACCEPTED);

        // 조사는 다른 스레드에서 진행되므로, 최대 5초까지 보고가 오기를 기다립니다.
        verify(loggingNotifier, timeout(5000))
                .send(argThat(report -> report.incident().service().equals("payment-api")));
    }
}
