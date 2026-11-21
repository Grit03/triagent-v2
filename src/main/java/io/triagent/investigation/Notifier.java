package io.triagent.investigation;

/**
 * 조사 결과를 사람에게 전달하는 채널(Slack, Discord 등). 구현은 notification 패키지가 맡습니다.
 */
public interface Notifier {

	void send(InvestigationReport report);
}
