package io.triagent.investigation;

public record Evidence(String source, boolean success, String content) {

	public static Evidence success(String source, String content) {
		return new Evidence(source, true, content);
	}

	public static Evidence failure(String source, String reason) {
		return new Evidence(source, false, reason);
	}
}
