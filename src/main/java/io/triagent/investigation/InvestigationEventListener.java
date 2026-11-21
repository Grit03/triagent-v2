package io.triagent.investigation;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import io.triagent.incident.IncidentOpenedEvent;

@Component
public class InvestigationEventListener {

	private final InvestigationService investigationService;

	public InvestigationEventListener(InvestigationService investigationService) {
		this.investigationService = investigationService;
	}

	/**
	 * 장애 저장 트랜잭션이 커밋된 뒤(AFTER_COMMIT, 기본값)에만 실행합니다.
	 * 커밋 전에 조사를 시작하면 다른 스레드에서 아직 저장되지 않은 장애를 조회하게 될 수 있습니다.
	 * {@code @Async} 덕분에 웹훅 요청 스레드는 조사가 끝나기를 기다리지 않습니다.
	 */
	@Async
	@TransactionalEventListener
	public void onIncidentOpened(IncidentOpenedEvent event) {
		investigationService.investigate(event.incidentId());
	}
}
