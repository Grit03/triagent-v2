package io.triagent.action;

import io.triagent.common.NotFoundException;

public class ActionNotFoundException extends NotFoundException {

	public ActionNotFoundException(Long actionId) {
		super("Action not found: " + actionId);
	}
}
