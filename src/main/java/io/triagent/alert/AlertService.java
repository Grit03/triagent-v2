package io.triagent.alert;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.triagent.alert.AlertmanagerWebhookRequest.Alert;
import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

/**
 * Alertmanager 알림을 장애 도메인의 명령으로 바꿉니다.
 * <p>
 * 외부 알림 형식은 이 패키지 안에서만 다룹니다. 나중에 Grafana나 Datadog 알림을 받더라도
 * 새 Controller와 Service만 추가하면 되고 incident 패키지는 바뀌지 않습니다.
 */
@Service
public class AlertService {

	private final IncidentService incidentService;

	public AlertService(IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	/**
	 * 한 요청 안의 알림은 하나의 트랜잭션으로 처리합니다. IncidentService의 트랜잭션은 이 트랜잭션에 합류합니다.
	 */
	@Transactional
	public AlertReceiptResponse receive(AlertmanagerWebhookRequest request) {
		List<Long> openedIncidentIds = new ArrayList<>();
		List<Long> resolvedIncidentIds = new ArrayList<>();
		for (Alert alert : request.alerts()) {
			if (alert.isFiring()) {
				incidentService.openIfAbsent(toOpenCommand(alert)).ifPresent(openedIncidentIds::add);
			}
			else {
				incidentService.resolveIfOpen(alert.fingerprint(), alert.resolvedAt())
						.ifPresent(resolvedIncidentIds::add);
			}
		}
		return new AlertReceiptResponse(openedIncidentIds, resolvedIncidentIds);
	}

	private static OpenIncidentCommand toOpenCommand(Alert alert) {
		return new OpenIncidentCommand(alert.fingerprint(), alert.alertName(), alert.service(), alert.severity(),
				alert.summary(), alert.startsAt());
	}
}
