package io.triagent.action;

public class DecisionNotAllowedException extends RuntimeException {

    public DecisionNotAllowedException(String message) {
        super(message);
    }
}
