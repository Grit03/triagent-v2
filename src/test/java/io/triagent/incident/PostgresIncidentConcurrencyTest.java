package io.triagent.incident;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * {@link IncidentConcurrencyTest}를 실제 PostgreSQL에서 실행합니다. 제약 위반 뒤 트랜잭션이 중단되는 방식이 H2와 달라서 운영 DB에서도
 * 따로 확인합니다. Docker가 필요합니다.
 */
@Testcontainers
class PostgresIncidentConcurrencyTest extends IncidentConcurrencyTest {

    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
}
