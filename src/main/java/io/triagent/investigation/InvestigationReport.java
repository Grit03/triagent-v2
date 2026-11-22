package io.triagent.investigation;

import io.triagent.incident.IncidentSnapshot;
import java.util.List;

public record InvestigationReport(
        IncidentSnapshot incident, List<Evidence> evidences, String analysis) {}
