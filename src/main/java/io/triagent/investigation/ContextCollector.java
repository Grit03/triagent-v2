package io.triagent.investigation;

import io.triagent.incident.IncidentSnapshot;

public interface ContextCollector {

	String source();

	Evidence collect(IncidentSnapshot incident);
}
