package io.triagent.investigation;

import java.util.List;

import io.triagent.incident.IncidentSnapshot;

/**
 * @param analysis 원인 후보와 권장 조치(LLM 또는 규칙 기반)
 */
public record InvestigationReport(IncidentSnapshot incident, List<Evidence> evidences, String analysis) {
}
