package io.triagent.action;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * 조치 요청과 조회 API.
 * <p>
 * 승인과 거절은 여기서 받지 않습니다. 승인자의 신원을 검증할 수 있는 Slack 버튼(F8)으로만 받습니다.
 */
@RestController
@RequestMapping("/api/v1")
public class ActionController {

	private final ActionService actionService;

	public ActionController(ActionService actionService) {
		this.actionService = actionService;
	}

	/** 새 리소스를 만들면 201 Created와 함께 조회 주소를 Location 헤더로 알려 줍니다. */
	@PostMapping("/incidents/{incidentId}/actions")
	public ResponseEntity<ActionResponse> requestAction(@PathVariable Long incidentId,
			@Valid @RequestBody CreateActionRequest request) {
		ActionResponse action = actionService.request(incidentId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/actions/{id}")
				.buildAndExpand(action.id())
				.toUri();
		return ResponseEntity.created(location).body(action);
	}

	@GetMapping("/actions/{actionId}")
	public ActionResponse getAction(@PathVariable Long actionId) {
		return actionService.getAction(actionId);
	}
}
