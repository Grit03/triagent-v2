package io.triagent.integration.loki;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * application.yml의 설정이 실제로 {@code triagent.loki.*}에 바인딩되는지 검증합니다.
 *
 * <p>단위 테스트는 LokiProperties를 직접 만들기 때문에, YAML 들여쓰기 실수로 설정이 다른 위치에 들어가도 잡아내지 못합니다.
 */
class LokiConfigurationTest {

    @Nested
    @SpringBootTest(properties = "LOKI_URL=http://loki:3100")
    @DisplayName("Loki 주소가 있으면")
    class WhenLokiUrlIsSet {

        @Autowired ApplicationContext context;

        @Test
        @DisplayName("YAML 설정으로 collector를 등록한다")
        void registerCollector() {
            assertThat(context.getBeansOfType(LokiLogCollector.class)).hasSize(1);
            LokiProperties properties = context.getBean(LokiProperties.class);
            assertThat(properties.baseUrl()).hasToString("http://loki:3100");
            assertThat(properties.lookback()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.limit()).isEqualTo(20);
        }
    }

    @Nested
    @SpringBootTest
    @DisplayName("Loki 주소가 없으면")
    class WhenLokiUrlIsMissing {

        @Autowired ApplicationContext context;

        @Test
        @DisplayName("collector를 등록하지 않는다")
        void skipCollector() {
            assertThat(context.getBeansOfType(LokiLogCollector.class)).isEmpty();
        }
    }
}
