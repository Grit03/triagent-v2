package io.triagent.investigation;

import io.triagent.incident.IncidentOpenedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InvestigationEventListener {

    private final InvestigationService investigationService;

    /** 커밋 전에 조사를 시작하면 다른 스레드가 아직 저장되지 않은 장애를 조회할 수 있어 AFTER_COMMIT(기본값)에서 실행합니다. */
    @Async
    @TransactionalEventListener
    public void onIncidentOpened(IncidentOpenedEvent event) {
        investigationService.investigate(event.incidentId());
    }
}
