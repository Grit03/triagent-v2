package io.triagent.alert;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class AlertService {

	private final IncidentService incidentService;

	/**
	 * 알림마다 IncidentService가 따로 트랜잭션을 엽니다. 한 알림이 실패해도 앞서 처리한 알림은 그대로 남고,
	 * Alertmanager가 실패한 요청을 다시 보내도 장애 열기와 해소는 같은 결과를 내므로 안전합니다.
	 */
	public AlertReceiptResponse receive(AlertmanagerWebhookRequest request) {
		List<Long> openedIncidentIds = new ArrayList<>();
		List<Long> resolvedIncidentIds = new ArrayList<>();
		for (Alert alert : request.alerts()) {
			if (alert.isFiring()) {
				incidentService.openIfAbsent(toOpenCommand(alert)).ifPresent(openedIncidentIds::add);
			}
			else {
				incidentService.resolveIfOpen(alert.fingerprint(), alert.resolvedAt())
						.forEach(resolvedIncidentIds::add);
			}
		}
		return new AlertReceiptResponse(openedIncidentIds, resolvedIncidentIds);
	}

	private static OpenIncidentCommand toOpenCommand(Alert alert) {
		return new OpenIncidentCommand(alert.fingerprint(), alert.alertName(), alert.service(), alert.severity(),
				alert.summary(), alert.startsAt());
	}
}
