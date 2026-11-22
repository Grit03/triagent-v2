package io.triagent.analysis;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.triagent.action.ActionType;

/** Claude가 호출하는 도구의 입력. 모델에게 실행 권한을 주지 않도록 조치를 실행하지 않고 승인 대기 요청만 만듭니다. */
@JsonClassDescription("""
		장애를 해결하기 위한 조치(롤백, 재배포, 재시작)를 제안합니다. 조치는 바로 실행되지 않고, 담당 팀이 Slack에서 \
		승인해야 실행됩니다. 근거(커밋, 로그, 지표)가 그 조치를 분명히 뒷받침할 때만 호출하고, 같은 종류는 한 번만 제안하세요.""")
public class ProposeRemediation {

	@JsonPropertyDescription("ROLLBACK: 직전 버전으로 되돌리기, REDEPLOY: 현재 버전 다시 배포, RESTART: 서버(파드) 재시작")
	public ActionType type;

	@JsonPropertyDescription("이 조치가 필요한 근거. 커밋 SHA, 로그 문장, 지표 변화를 인용해 한두 문장으로 씁니다.")
	public String reason;
}
