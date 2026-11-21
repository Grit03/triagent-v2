package io.triagent.analysis;

import java.util.List;

import io.triagent.incident.IncidentResponse;
import io.triagent.investigation.Evidence;

/**
 * 장애 정보와 수집한 근거로 원인 후보와 권장 조치를 정리합니다.
 */
public interface IncidentAnalyzer {

	String analyze(IncidentResponse incident, List<Evidence> evidences);
}
