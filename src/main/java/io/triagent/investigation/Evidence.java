package io.triagent.investigation;

/**
 * 조사 중 외부 도구에서 모은 근거 하나.
 *
 * @param source 근거를 가져온 곳(예: github, loki)
 * @param success 조회 성공 여부
 * @param content 성공 시 조회 결과, 실패 시 실패 사유
 */
public record Evidence(String source, boolean success, String content) {

	public static Evidence success(String source, String content) {
		return new Evidence(source, true, content);
	}

	public static Evidence failure(String source, String reason) {
		return new Evidence(source, false, reason);
	}
}
