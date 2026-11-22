package io.triagent.action;

public class ActionAlreadyDecidedException extends RuntimeException {

    public ActionAlreadyDecidedException(Long actionId, ActionStatus status) {
        super("Action %d is already %s".formatted(actionId, status));
    }
}
