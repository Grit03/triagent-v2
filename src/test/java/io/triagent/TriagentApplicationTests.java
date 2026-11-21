package io.triagent;

import static org.assertj.core.api.Assertions.assertThat;

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
	void contextLoads() {
	}

	@Test
	void healthEndpointReturnsUp() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	void livenessProbeIsExposed() {
		assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
	}
}
