package io.triagent.investigation;

import java.util.List;

import io.triagent.incident.IncidentSnapshot;

/**
 * 장애 정보와 수집한 근거로 원인 후보와 권장 조치를 정리합니다.
 * <p>
 * 조사 워크플로가 필요로 하는 기능이므로 investigation 패키지가 인터페이스를 갖고, 구현은 analysis 패키지가 맡습니다.
 */
public interface IncidentAnalyzer {

	String analyze(IncidentSnapshot incident, List<Evidence> evidences);
}
