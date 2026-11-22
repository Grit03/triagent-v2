package io.triagent.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 환경 변수가 YAML을 거쳐 SlackProperties에 들어오는지 확인합니다. YAML 들여쓰기 실수는 컴파일로 잡히지 않습니다.
 */
@SpringBootTest(properties = "SLACK_SIGNING_SECRET=test-signing-secret")
class SlackConfigurationTest {

	@Autowired
	SlackProperties properties;

	@Test
	@DisplayName("환경 변수의 Signing Secret을 바인딩한다")
	void bindSigningSecret() {
		assertThat(properties.signingSecret()).isEqualTo("test-signing-secret");
	}
}
