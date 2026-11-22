package io.triagent.action;

public interface ActionExecutor {

	String execute(ActionType type, String service);
}
