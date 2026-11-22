package io.triagent.alert;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 외부 모니터링 시스템이 보내는 장애 알림을 받는 웹훅 엔드포인트.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertWebhookController {

	private final AlertService alertService;

	/**
	 * 알림을 접수만 하고 바로 응답합니다. 조사는 이후 단계에서 비동기로 진행하므로 202 Accepted를 돌려줍니다.
	 */
	@PostMapping("/alertmanager")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public AlertReceiptResponse receive(@Valid @RequestBody AlertmanagerWebhookRequest request) {
		return alertService.receive(request);
	}
}
