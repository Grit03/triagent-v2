package io.triagent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class TriagentApplicationTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	@DisplayName("애플리케이션 컨텍스트가 뜬다")
	void contextLoads() {
	}

	@Test
	@DisplayName("/actuator/health가 UP을 응답한다")
	void health() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	@DisplayName("liveness probe 엔드포인트가 열려 있다")
	void livenessProbe() {
		assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
	}
}
