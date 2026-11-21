package io.triagent.notification;

import io.triagent.investigation.InvestigationReport;

/**
 * 조사 결과를 사람에게 전달하는 채널(Slack, Discord 등).
 */
public interface Notifier {

	void send(InvestigationReport report);
}
