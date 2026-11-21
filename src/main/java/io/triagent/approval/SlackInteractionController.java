package io.triagent.approval;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Slack 앱의 Interactivity Request URL로 등록하는 엔드포인트. 버튼을 누르면 Slack이 이 주소로 요청을 보냅니다.
 * <p>
 * 서명은 Slack이 보낸 바이트 그대로의 본문으로 계산해야 합니다. {@code @RequestParam}이나 {@code @RequestBody}로 받으면
 * Spring이 폼 파라미터를 다시 인코딩한 본문을 줄 수 있어, 서블릿 입력 스트림에서 원본을 직접 읽습니다.
 */
@RestController
public class SlackInteractionController {

	private static final String PAYLOAD_FIELD = "payload=";

	private final SlackSignatureVerifier signatureVerifier;

	private final SlackInteractionService interactionService;

	private final JsonMapper jsonMapper;

	public SlackInteractionController(SlackSignatureVerifier signatureVerifier,
			SlackInteractionService interactionService, JsonMapper jsonMapper) {
		this.signatureVerifier = signatureVerifier;
		this.interactionService = interactionService;
		this.jsonMapper = jsonMapper;
	}

	@PostMapping(path = "/api/v1/slack/interactions", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Void> receive(
			@RequestHeader(name = "X-Slack-Request-Timestamp", required = false) String timestamp,
			@RequestHeader(name = "X-Slack-Signature", required = false) String signature,
			HttpServletRequest request) throws IOException {
		String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!signatureVerifier.isValid(timestamp, signature, rawBody)) {
			return ResponseEntity.status(401).build();
		}
		if (!rawBody.startsWith(PAYLOAD_FIELD)) {
			return ResponseEntity.badRequest().build();
		}
		String payloadJson = URLDecoder.decode(rawBody.substring(PAYLOAD_FIELD.length()), StandardCharsets.UTF_8);
		interactionService.handle(jsonMapper.readValue(payloadJson, SlackInteractionPayload.class));
		return ResponseEntity.ok().build();
	}
}
