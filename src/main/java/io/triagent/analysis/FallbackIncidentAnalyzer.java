package io.triagent.analysis;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.IncidentAnalyzer;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Claude 분석이 불가능하거나 실패하면 규칙 기반 요약으로 대체합니다. LLM이 멈춰도 장애 보고는 나가야 하기 때문입니다. IncidentAnalyzer 빈이 여러
 * 개이므로 {@code @Primary}로 이 구현체를 기본 주입 대상으로 지정합니다.
 */
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
            } catch (RuntimeException ex) {
                log.warn(
                        "LLM analysis failed, falling back to rule-based summary: incidentId={}",
                        incident.id(),
                        ex);
            }
        }
        return ruleBasedAnalyzer.analyze(incident, evidences);
    }
}
