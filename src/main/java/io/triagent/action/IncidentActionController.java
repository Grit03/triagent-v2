package io.triagent.action;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** 승인과 거절은 여기서 받지 않습니다. 승인자의 신원을 검증할 수 있는 Slack 버튼으로만 받습니다. */
@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/actions")
@RequiredArgsConstructor
public class IncidentActionController {

    /**
     * 인증이 없어 호출한 사람을 알 수 없으므로, 누구인지 지어내지 않고 경로만 기록합니다. Slack 사용자 ID가 아니므로 담당 팀원 누구나 승인할 수 있습니다(에이전트
     * 요청과 같은 규칙).
     */
    static final String API_ACTOR = "api";

    private final ActionService actionService;

    @PostMapping
    public ResponseEntity<ActionResponse> requestAction(
            @PathVariable Long incidentId, @Valid @RequestBody CreateActionRequest request) {
        ActionResponse action = actionService.request(incidentId, request, API_ACTOR);
        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/actions/{id}")
                        .buildAndExpand(action.id())
                        .toUri();
        return ResponseEntity.created(location).body(action);
    }
}
