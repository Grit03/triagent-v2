package io.triagent.investigation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.triagent.incident.Incident;
import io.triagent.incident.IncidentRepository;
import io.triagent.incident.IncidentResponse;
import io.triagent.notification.Notifier;

/**
 * 장애 하나를 조사하고 보고하는 워크플로: 근거 수집 → 보고서 작성 → 메신저 전송.
 * <p>
 * 외부 API 호출이 길어질 수 있으므로 DB 트랜잭션을 걸지 않습니다.
 */
@Service
public class InvestigationService {

	private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

	private final IncidentRepository incidentRepository;

	private final List<ContextCollector> collectors;

	private final List<Notifier> notifiers;

	/** 같은 타입의 빈이 여러 개면 List로 모두 주입받을 수 있습니다. */
	public InvestigationService(IncidentRepository incidentRepository, List<ContextCollector> collectors,
			List<Notifier> notifiers) {
		this.incidentRepository = incidentRepository;
		this.collectors = collectors;
		this.notifiers = notifiers;
	}

	public void investigate(Long incidentId) {
		Incident incident = incidentRepository.findById(incidentId).orElse(null);
		if (incident == null) {
			log.warn("Incident disappeared before investigation: id={}", incidentId);
			return;
		}
		List<Evidence> evidences = collectors.stream().map(collector -> collectSafely(collector, incident)).toList();
		InvestigationReport report = new InvestigationReport(IncidentResponse.from(incident), evidences);
		notifiers.forEach(notifier -> notifySafely(notifier, report));
	}

	/** 도구 하나가 실패해도 나머지 조사는 계속합니다. */
	private Evidence collectSafely(ContextCollector collector, Incident incident) {
		try {
			return collector.collect(incident);
		}
		catch (RuntimeException ex) {
			log.warn("Collector failed: source={}, incidentId={}", collector.source(), incident.getId(), ex);
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
