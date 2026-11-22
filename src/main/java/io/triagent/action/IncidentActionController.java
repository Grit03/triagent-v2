package io.triagent.action;

import java.net.URI;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/actions")
@RequiredArgsConstructor
public class IncidentActionController {

	static final String API_ACTOR = "api";

	private final ActionService actionService;

	@PostMapping
	public ResponseEntity<ActionResponse> requestAction(@PathVariable Long incidentId,
			@Valid @RequestBody CreateActionRequest request) {
		ActionResponse action = actionService.request(incidentId, request, API_ACTOR);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/actions/{id}")
				.buildAndExpand(action.id())
				.toUri();
		return ResponseEntity.created(location).body(action);
	}
}
