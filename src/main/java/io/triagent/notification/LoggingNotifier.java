package io.triagent.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.triagent.investigation.InvestigationReport;

/**
 * 조사 결과를 애플리케이션 로그로 남깁니다. 메신저 설정 없이도 로컬에서 흐름을 확인할 수 있습니다.
 */
@Component
public class LoggingNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

	@Override
	public void send(InvestigationReport report) {
		log.info("Investigation report: incident={}, analysis={}, evidences={}", report.incident(), report.analysis(),
				report.evidences());
	}
}
