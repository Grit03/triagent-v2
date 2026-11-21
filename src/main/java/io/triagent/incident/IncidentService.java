package io.triagent.incident;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.triagent.alert.AlertReceiptResponse;
import io.triagent.alert.AlertmanagerWebhookRequest;
import io.triagent.alert.AlertmanagerWebhookRequest.Alert;

@Service
@Transactional(readOnly = true)
public class IncidentService {

	private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

	private final IncidentRepository incidentRepository;

	public IncidentService(IncidentRepository incidentRepository) {
		this.incidentRepository = incidentRepository;
	}

	/**
	 * 알림 묶음을 장애로 반영합니다. 한 요청 안의 알림은 하나의 트랜잭션으로 처리합니다.
	 */
	@Transactional
	public AlertReceiptResponse handle(AlertmanagerWebhookRequest request) {
		List<Long> opened = new ArrayList<>();
		List<Long> resolved = new ArrayList<>();
		for (Alert alert : request.alerts()) {
			if (alert.isFiring()) {
				openIfAbsent(alert, opened);
			}
			else {
				resolveIfOpen(alert, resolved);
			}
		}
		return new AlertReceiptResponse(opened, resolved);
	}

	public IncidentResponse getIncident(Long id) {
		return incidentRepository.findById(id)
				.map(IncidentResponse::from)
				.orElseThrow(() -> new IncidentNotFoundException(id));
	}

	/**
	 * Alertmanager는 알림이 해소될 때까지 같은 알림을 주기적으로 다시 보냅니다(repeat_interval).
	 * 이미 열린 장애가 있으면 새로 만들지 않아 중복 조사를 막습니다.
	 */
	private void openIfAbsent(Alert alert, List<Long> opened) {
		if (incidentRepository.findFirstByFingerprintAndStatus(alert.fingerprint(), IncidentStatus.OPEN).isPresent()) {
			log.debug("Duplicate firing alert ignored: fingerprint={}", alert.fingerprint());
			return;
		}
		String summary = alert.annotation("summary") != null ? alert.annotation("summary")
				: alert.annotation("description");
		Incident incident = Incident.open(alert.fingerprint(), alert.label("alertname", "unknown"),
				alert.label("service", alert.label("job", "unknown")), alert.label("severity", "none"), summary,
				alert.startsAt());
		Long id = incidentRepository.save(incident).getId();
		log.info("Incident opened: id={}, alert={}, service={}", id, incident.getAlertName(), incident.getService());
		opened.add(id);
	}

	private void resolveIfOpen(Alert alert, List<Long> resolved) {
		incidentRepository.findFirstByFingerprintAndStatus(alert.fingerprint(), IncidentStatus.OPEN)
				.ifPresent(incident -> {
					// 변경 감지(dirty checking): 트랜잭션이 끝날 때 바뀐 필드가 자동으로 UPDATE 됩니다.
					incident.resolve(alert.endsAt() != null ? alert.endsAt() : Instant.now());
					log.info("Incident resolved: id={}", incident.getId());
					resolved.add(incident.getId());
				});
	}
}
