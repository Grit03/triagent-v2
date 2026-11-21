package io.triagent.approval;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 승인, 거절 버튼이 달린 Slack Block Kit 메시지.
 * <p>
 * https://docs.slack.dev/reference/block-kit/blocks/actions-block
 * https://docs.slack.dev/reference/block-kit/block-elements/button-element
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SlackApprovalMessage(String text, List<Block> blocks) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Block(String type, Text text, List<Button> elements) {

		static Block section(String markdown) {
			return new Block("section", new Text("mrkdwn", markdown), null);
		}

		static Block actions(Button... buttons) {
			return new Block("actions", null, List.of(buttons));
		}
	}

	record Text(String type, String text) {
	}

	/**
	 * @param actionId 어떤 버튼인지 구분하는 값. 클릭하면 Slack이 이 값을 그대로 돌려줍니다.
	 * @param value 버튼에 실어 보내는 값. 여기서는 조치 ID입니다.
	 * @param style primary(초록) 또는 danger(빨강)
	 */
	record Button(String type, Text text, @JsonProperty("action_id") String actionId, String value, String style) {

		static Button of(String label, String actionId, Long value, String style) {
			return new Button("button", new Text("plain_text", label), actionId, String.valueOf(value), style);
		}
	}
}
