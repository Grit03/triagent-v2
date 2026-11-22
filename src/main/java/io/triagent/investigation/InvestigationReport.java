package io.triagent.investigation;

import io.triagent.action.ActionResponse;
import io.triagent.incident.IncidentSnapshot;
import java.util.List;

/**
 * @param requestedActions 분석이 끝난 시점에 DB에 저장된 이 장애의 조치 요청. 분석 텍스트는 모델이 쓴 것이라 "승인을 요청했다"고 적혀 있어도 실제
 *     요청이 없을 수 있으므로, 무엇이 요청됐는지는 이 값으로 알립니다. 조회에 실패하면 null입니다.
 */
public record InvestigationReport(
        IncidentSnapshot incident,
        List<Evidence> evidences,
        String analysis,
        List<ActionResponse> requestedActions) {}
