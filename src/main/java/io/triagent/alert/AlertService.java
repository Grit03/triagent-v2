package io.triagent.alert;

import io.triagent.alert.AlertmanagerWebhookRequest.Alert;
import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final IncidentService incidentService;

    /** 트랜잭션을 걸지 않습니다. 한 알림이 실패해도 앞서 처리한 알림은 남고, 재전송돼도 결과가 같습니다. */
    public AlertReceiptResponse receive(AlertmanagerWebhookRequest request) {
        List<Long> openedIncidentIds = new ArrayList<>();
        List<Long> resolvedIncidentIds = new ArrayList<>();
        for (Alert alert : request.alerts()) {
            if (alert.isFiring()) {
                incidentService
                        .openIfAbsent(toOpenCommand(alert))
                        .ifPresent(openedIncidentIds::add);
            } else {
                incidentService
                        .resolveIfOpen(alert.fingerprint(), alert.resolvedAt())
                        .forEach(resolvedIncidentIds::add);
            }
        }
        return new AlertReceiptResponse(openedIncidentIds, resolvedIncidentIds);
    }

    private static OpenIncidentCommand toOpenCommand(Alert alert) {
        return new OpenIncidentCommand(
                alert.fingerprint(),
                alert.alertName(),
                alert.service(),
                alert.severity(),
                alert.summary(),
                alert.startsAt());
    }
}
