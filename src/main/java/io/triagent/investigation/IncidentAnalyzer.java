package io.triagent.investigation;

import java.util.List;

import io.triagent.incident.IncidentSnapshot;

public interface IncidentAnalyzer {

	String analyze(IncidentSnapshot incident, List<Evidence> evidences);
}
