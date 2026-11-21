package io.triagent.investigation;

import java.util.List;

import io.triagent.incident.IncidentResponse;

public record InvestigationReport(IncidentResponse incident, List<Evidence> evidences) {
}
