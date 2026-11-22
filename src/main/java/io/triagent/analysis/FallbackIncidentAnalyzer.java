package io.triagent.analysis;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.IncidentAnalyzer;

@Primary
@Component
@Slf4j
@RequiredArgsConstructor
public class FallbackIncidentAnalyzer implements IncidentAnalyzer {

	private final Optional<ClaudeIncidentAnalyzer> claudeAnalyzer;

	private final RuleBasedIncidentAnalyzer ruleBasedAnalyzer;

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
