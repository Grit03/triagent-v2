package io.triagent.incident;

import io.triagent.common.NotFoundException;

public class IncidentNotFoundException extends NotFoundException {

    public IncidentNotFoundException(Long id) {
        super("Incident not found: " + id);
    }
}
