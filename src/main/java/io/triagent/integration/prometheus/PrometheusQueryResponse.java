package io.triagent.integration.prometheus;

import java.util.List;
import java.util.Map;

public record PrometheusQueryResponse(String status, Data data) {

    public record Data(String resultType, List<Sample> result) {}

    public record Sample(Map<String, String> metric, List<Object> value) {

        public double numericValue() {
            Object sampleValue = value.get(1);
            return Double.parseDouble(sampleValue.toString());
        }
    }
}
