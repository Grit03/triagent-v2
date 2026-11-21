package io.triagent.integration.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * application.yml의 지표 쿼리가 {@code triagent.prometheus.*}에 순서대로 바인딩되는지 검증합니다.
 */
@SpringBootTest(properties = "PROMETHEUS_URL=http://prometheus:9090")
class PrometheusConfigurationTest {

	@Autowired
	ApplicationContext context;

	@Test
	void bindsQueriesFromYamlInDeclaredOrder() {
		assertThat(context.getBeansOfType(PrometheusMetricCollector.class)).hasSize(1);
		PrometheusProperties properties = context.getBean(PrometheusProperties.class);
		assertThat(properties.baseUrl()).hasToString("http://prometheus:9090");
		assertThat(properties.queries()).containsOnlyKeys("error-rate", "p95-latency-seconds");
		assertThat(properties.queries().keySet()).first().isEqualTo("error-rate");
		// %1$s가 Spring 플레이스홀더로 오해받지 않고 그대로 남아 있어야 서비스 이름을 넣을 수 있습니다.
		assertThat(properties.queries().get("error-rate")).contains("service=\"%1$s\"");
	}
}
