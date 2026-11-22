package io.triagent.notification;

import java.util.List;

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
