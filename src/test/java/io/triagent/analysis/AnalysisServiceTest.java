package io.triagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.triagent.incident.IncidentResponse;
import io.triagent.incident.IncidentStatus;
import io.triagent.investigation.Evidence;

class AnalysisServiceTest {

	static final IncidentResponse INCIDENT = new IncidentResponse(1L, "HighErrorRate", "payment-api", "critical",
			"5xx > 5%", IncidentStatus.OPEN, Instant.parse("2026-11-21T01:00:00Z"), null);

	static final List<Evidence> EVIDENCES = List.of(
			Evidence.success("github", "acme/payment-api: 장애 전 24시간 동안 커밋 2건\n- a1b2c3d fix: 타임아웃"),
			Evidence.success("loki", "에러 로그 3건 (최신순)\n- ... Connection pool exhausted"),
			Evidence.failure("prometheus", "조회 실패: timeout"));

	ClaudeIncidentAnalyzer claude = mock(ClaudeIncidentAnalyzer.class);

	RuleBasedIncidentAnalyzer ruleBased = new RuleBasedIncidentAnalyzer();

	@Test
	void usesClaudeWhenAvailable() {
		given(claude.analyze(any(), any())).willReturn("*원인 후보*: a1b2c3d 배포");

		assertThat(new AnalysisService(Optional.of(claude), ruleBased).analyze(INCIDENT, EVIDENCES))
				.isEqualTo("*원인 후보*: a1b2c3d 배포");
	}

	@Test
	void fallsBackToRulesWhenClaudeFails() {
		given(claude.analyze(any(), any())).willThrow(new IllegalStateException("overloaded"));

		assertThat(new AnalysisService(Optional.of(claude), ruleBased).analyze(INCIDENT, EVIDENCES))
				.startsWith("(규칙 기반 요약)");
	}

	@Test
	void rulesPointToRecentCommitsLogsAndFailures() {
		assertThat(new AnalysisService(Optional.empty(), ruleBased).analyze(INCIDENT, EVIDENCES))
				.isEqualTo("(규칙 기반 요약) 장애 직전에 커밋이 있습니다. 최근 배포를 먼저 확인하세요. "
						+ "에러 로그가 있습니다. 가장 최근 예외 메시지를 확인하세요. 조회에 실패한 근거가 있습니다: prometheus");
	}
}
