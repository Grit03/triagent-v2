package io.triagent.investigation;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;

/**
 * 장애 하나를 조사하고 보고하는 워크플로: 근거 수집 → 원인 분석 → 메신저 전송.
 * <p>
 * 외부 API 호출이 길어질 수 있으므로 DB 트랜잭션을 걸지 않습니다.
 * 수집, 분석, 전송은 이 패키지의 인터페이스로만 호출하므로 investigation은 구현 패키지를 알지 못합니다.
 */
@Service
public class InvestigationService {

	private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

	private final IncidentService incidentService;

	private final List<ContextCollector> collectors;

	private final IncidentAnalyzer analyzer;

	private final List<Notifier> notifiers;

	public InvestigationService(IncidentService incidentService, List<ContextCollector> collectors,
			IncidentAnalyzer analyzer, List<Notifier> notifiers) {
		this.incidentService = incidentService;
		this.collectors = collectors;
		this.analyzer = analyzer;
		this.notifiers = notifiers;
	}

	public void investigate(Long incidentId) {
		Optional<IncidentSnapshot> found = incidentService.findSnapshot(incidentId);
		if (found.isEmpty()) {
			log.warn("Incident disappeared before investigation: id={}", incidentId);
			return;
		}
		IncidentSnapshot incident = found.get();
		List<Evidence> evidences = collectors.stream().map(collector -> collectSafely(collector, incident)).toList();
		String analysis = analyzer.analyze(incident, evidences);
		InvestigationReport report = new InvestigationReport(incident, evidences, analysis);
		notifiers.forEach(notifier -> notifySafely(notifier, report));
	}

	/** 도구 하나가 실패해도 나머지 조사는 계속합니다. */
	private Evidence collectSafely(ContextCollector collector, IncidentSnapshot incident) {
		try {
			return collector.collect(incident);
		}
		catch (RuntimeException ex) {
			log.warn("Collector failed: source={}, incidentId={}", collector.source(), incident.id(), ex);
			return Evidence.failure(collector.source(), "조회 실패: " + ex.getMessage());
		}
	}

	private void notifySafely(Notifier notifier, InvestigationReport report) {
		try {
			notifier.send(report);
		}
		catch (RuntimeException ex) {
			log.error("Notifier failed: notifier={}, incidentId={}", notifier.getClass().getSimpleName(),
					report.incident().id(), ex);
		}
	}
}
