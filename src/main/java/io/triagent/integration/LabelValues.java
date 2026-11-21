package io.triagent.integration;

/**
 * LogQL, PromQL 라벨 매처에 외부 입력을 넣을 때 쓰는 유틸리티.
 */
public final class LabelValues {

	private LabelValues() {
	}

	/** 따옴표와 역슬래시를 이스케이프해 쿼리 문법이 깨지거나 조작되지 않게 합니다. */
	public static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
