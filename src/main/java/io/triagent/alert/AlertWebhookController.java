package io.triagent.alert;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertWebhookController {

	private final AlertService alertService;

	@PostMapping("/alertmanager")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public AlertReceiptResponse receive(@Valid @RequestBody AlertmanagerWebhookRequest request) {
		return alertService.receive(request);
	}
}
