package io.triagent.investigation;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvestigationService {

	private final IncidentService incidentService;

	private final List<ContextCollector> collectors;

	private final IncidentAnalyzer analyzer;

	private final List<Notifier> notifiers;

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
