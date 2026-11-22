package io.triagent.approval;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record SlackInteractionPayload(String type, User user, List<Action> actions,
		@JsonProperty("response_url") String responseUrl) {

	record User(String id) {
	}

	record Action(@JsonProperty("action_id") String actionId, String value) {
	}
}
