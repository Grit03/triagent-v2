package io.triagent.approval;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 버튼을 눌렀을 때 Slack이 보내는 block_actions 페이로드 중 필요한 부분.
 * <p>
 * https://docs.slack.dev/reference/interaction-payloads/block_actions-payload
 *
 * @param responseUrl 원래 메시지를 바꿀 때 쓰는 일회성 웹훅 주소
 */
record SlackInteractionPayload(String type, User user, List<Action> actions,
		@JsonProperty("response_url") String responseUrl) {

	record User(String id) {
	}

	record Action(@JsonProperty("action_id") String actionId, String value) {
	}
}
