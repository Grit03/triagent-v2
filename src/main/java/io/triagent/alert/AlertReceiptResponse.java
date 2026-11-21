package io.triagent.alert;

import java.util.List;

/**
 * 웹훅 처리 결과. 새로 열린 장애와 해소된 장애의 ID를 돌려줍니다.
 */
public record AlertReceiptResponse(List<Long> openedIncidentIds, List<Long> resolvedIncidentIds) {
}
