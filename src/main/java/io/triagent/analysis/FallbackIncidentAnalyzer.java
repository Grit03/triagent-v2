package io.triagent.analysis;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.IncidentAnalyzer;

/**
 * Claude 분석을 먼저 시도하고, 쓸 수 없거나 실패하면 규칙 기반 요약으로 대체합니다.
 * 장애 대응 도구는 LLM이 멈춰도 보고는 나가야 하기 때문입니다.
 * <p>
 * IncidentAnalyzer 빈이 여러 개이므로 {@code @Primary}로 이 조합 구현체를 기본 주입 대상으로 지정합니다.
 */
@Primary
@Component
public class FallbackIncidentAnalyzer implements IncidentAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(FallbackIncidentAnalyzer.class);

	private final Optional<ClaudeIncidentAnalyzer> claudeAnalyzer;

	private final RuleBasedIncidentAnalyzer ruleBasedAnalyzer;

	public FallbackIncidentAnalyzer(Optional<ClaudeIncidentAnalyzer> claudeAnalyzer,
			RuleBasedIncidentAnalyzer ruleBasedAnalyzer) {
		this.claudeAnalyzer = claudeAnalyzer;
		this.ruleBasedAnalyzer = ruleBasedAnalyzer;
	}

	@Override
	public String analyze(IncidentSnapshot incident, List<Evidence> evidences) {
		if (claudeAnalyzer.isPresent()) {
			try {
				return claudeAnalyzer.get().analyze(incident, evidences);
			}
			catch (RuntimeException ex) {
				log.warn("LLM analysis failed, falling back to rule-based summary: incidentId={}", incident.id(), ex);
			}
		}
		return ruleBasedAnalyzer.analyze(incident, evidences);
	}
}
