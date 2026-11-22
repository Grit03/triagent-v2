package io.triagent.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelValuesTest {

	@Test
	@DisplayName("따옴표와 역슬래시를 이스케이프한다")
	void escape() {
		// 이스케이프하지 않으면 service 값으로 쿼리 조건을 끼워 넣을 수 있습니다.
		assertThat(LabelValues.escape("pay\"} or {x=\"1")).isEqualTo("pay\\\"} or {x=\\\"1");
		assertThat(LabelValues.escape("a\\b")).isEqualTo("a\\\\b");
	}
}
