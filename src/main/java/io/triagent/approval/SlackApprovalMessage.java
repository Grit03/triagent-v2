package io.triagent.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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

    record Text(String type, String text) {}

    record Button(
            String type,
            Text text,
            @JsonProperty("action_id") String actionId,
            String value,
            String style) {

        static Button of(String label, String actionId, Long value, String style) {
            return new Button(
                    "button",
                    new Text("plain_text", label),
                    actionId,
                    String.valueOf(value),
                    style);
        }
    }
}
