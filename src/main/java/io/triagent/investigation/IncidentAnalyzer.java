package io.triagent.investigation;

import io.triagent.incident.IncidentSnapshot;
import java.util.List;

public interface IncidentAnalyzer {

    String analyze(IncidentSnapshot incident, List<Evidence> evidences);
}
