package io.triagent.incident;

import java.time.Instant;

public record OpenIncidentCommand(
        String fingerprint,
        String alertName,
        String service,
        String severity,
        String summary,
        Instant startedAt) {}
