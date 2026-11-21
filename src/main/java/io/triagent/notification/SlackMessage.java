package io.triagent.notification;

import java.util.List;

/**
 * Slack Block Kit 메시지. record 구조가 그대로 JSON 구조가 됩니다.
 * <p>
 * https://docs.slack.dev/reference/block-kit/blocks
 *
 * @param text 알림 미리보기와 blocks를 표시하지 못하는 클라이언트에서 쓰는 대체 문구
 */
record SlackMessage(String text, List<Block> blocks) {

	record Block(String type, Text text) {

		static Block header(String plainText) {
			return new Block("header", new Text("plain_text", plainText));
		}

		static Block section(String markdown) {
			return new Block("section", new Text("mrkdwn", markdown));
		}
	}

	record Text(String type, String text) {
	}
}
