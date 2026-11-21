package io.triagent.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import io.triagent.investigation.IncidentAnalyzer;

/**
 * LLM을 쓸 수 없을 때(키 없음, 장애, 거절) 사용하는 규칙 기반 요약.
 * 원인을 추론하지는 않고, 당직자가 먼저 볼 곳만 짚어 줍니다.
 */
@Component
public class RuleBasedIncidentAnalyzer implements IncidentAnalyzer {

	@Override
	public String analyze(IncidentSnapshot incident, List<Evidence> evidences) {
		List<String> hints = new ArrayList<>();
		for (Evidence evidence : evidences) {
			if (!evidence.success()) {
				continue;
			}
			if ("github".equals(evidence.source()) && evidence.content().contains("커밋 ")) {
				hints.add("장애 직전에 커밋이 있습니다. 최근 배포를 먼저 확인하세요.");
			}
			if ("loki".equals(evidence.source()) && evidence.content().startsWith("에러 로그 ")) {
				hints.add("에러 로그가 있습니다. 가장 최근 예외 메시지를 확인하세요.");
			}
		}
		String failedSources = evidences.stream()
				.filter(evidence -> !evidence.success())
				.map(Evidence::source)
				.collect(Collectors.joining(", "));
		if (!failedSources.isEmpty()) {
			hints.add("조회에 실패한 근거가 있습니다: " + failedSources);
		}
		if (hints.isEmpty()) {
			hints.add("자동으로 짚을 만한 근거가 없습니다. 대시보드에서 직접 확인이 필요합니다.");
		}
		return "(규칙 기반 요약) " + String.join(" ", hints);
	}
}
