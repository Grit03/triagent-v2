package io.triagent.analysis;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.triagent.incident.IncidentResponse;
import io.triagent.investigation.Evidence;

/**
 * Claude 분석을 먼저 시도하고, 쓸 수 없거나 실패하면 규칙 기반 요약으로 대체합니다.
 * 장애 대응 도구는 LLM이 멈춰도 보고는 나가야 하기 때문입니다.
 */
@Service
public class AnalysisService {

	private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

	private final Optional<ClaudeIncidentAnalyzer> claudeAnalyzer;

	private final RuleBasedIncidentAnalyzer ruleBasedAnalyzer;

	/** 빈이 없을 수도 있는 의존성은 Optional로 주입받습니다. */
	public AnalysisService(Optional<ClaudeIncidentAnalyzer> claudeAnalyzer,
			RuleBasedIncidentAnalyzer ruleBasedAnalyzer) {
		this.claudeAnalyzer = claudeAnalyzer;
		this.ruleBasedAnalyzer = ruleBasedAnalyzer;
	}

	public String analyze(IncidentResponse incident, List<Evidence> evidences) {
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
