package io.triagent.investigation;

import java.util.List;

import io.triagent.incident.IncidentSnapshot;

public record InvestigationReport(IncidentSnapshot incident, List<Evidence> evidences, String analysis) {
}
