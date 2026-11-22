package io.triagent.alert;

import java.util.List;

public record AlertReceiptResponse(List<Long> openedIncidentIds, List<Long> resolvedIncidentIds) {}
