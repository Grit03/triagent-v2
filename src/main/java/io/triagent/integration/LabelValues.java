package io.triagent.integration;

public final class LabelValues {

    private LabelValues() {}

    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
