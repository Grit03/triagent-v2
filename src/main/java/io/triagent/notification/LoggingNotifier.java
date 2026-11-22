package io.triagent.notification;

import io.triagent.investigation.InvestigationReport;
import io.triagent.investigation.Notifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingNotifier implements Notifier {

    @Override
    public void send(InvestigationReport report) {
        log.info(
                "Investigation report: incident={}, analysis={}, evidences={}",
                report.incident(),
                report.analysis(),
                report.evidences());
    }
}
