package io.triagent.action;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 조치 조회 API. 조치 요청은 {@link IncidentActionController}로 받습니다.
 */
@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {

	private final ActionService actionService;

	public ActionController(ActionService actionService) {
		this.actionService = actionService;
	}

	@GetMapping("/{actionId}")
	public ActionResponse getAction(@PathVariable Long actionId) {
		return actionService.getAction(actionId);
	}
}
