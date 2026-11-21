package io.triagent.integration.loki;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import io.triagent.incident.Incident;
import io.triagent.integration.LabelValues;
import io.triagent.integration.loki.LokiClient.LogLine;
import io.triagent.investigation.ContextCollector;
import io.triagent.investigation.Evidence;

/**
 * 장애 시작 전후의 에러 로그를 근거로 수집합니다.
 */
@Component
@ConditionalOnExpression("'${triagent.loki.base-url:}' != ''")
public class LokiLogCollector implements ContextCollector {

	static final String SOURCE = "loki";

	/** Slack 메시지가 너무 길어지지 않도록 로그 한 줄은 이 길이에서 자릅니다. */
	private static final int MAX_LINE_LENGTH = 200;

	private final LokiClient lokiClient;

	private final LokiProperties properties;

	public LokiLogCollector(LokiClient lokiClient, LokiProperties properties) {
		this.lokiClient = lokiClient;
		this.properties = properties;
	}

	@Override
	public String source() {
		return SOURCE;
	}

	@Override
	public Evidence collect(Incident incident) {
		String logQl = properties.query().formatted(LabelValues.escape(incident.getService()));
		Instant start = incident.getStartedAt().minus(properties.lookback());
		// 알림은 보통 문제가 생기고 조금 뒤에 오므로, 장애 시작 이후 로그도 지금까지 함께 봅니다.
		Instant end = Instant.now();
		List<LogLine> lines = lokiClient.queryRange(logQl, start, end, properties.limit());
		if (lines.isEmpty()) {
			return Evidence.success(SOURCE, "에러 로그가 없습니다. (`%s`)".formatted(logQl));
		}
		String body = lines.stream()
				.map(line -> "- %s %s".formatted(line.timestamp(), truncate(line.line())))
				.collect(Collectors.joining("\n"));
		return Evidence.success(SOURCE, "에러 로그 %d건 (최신순)\n%s".formatted(lines.size(), body));
	}


	private static String truncate(String line) {
		String singleLine = line.replace('\n', ' ');
		return singleLine.length() <= MAX_LINE_LENGTH ? singleLine : singleLine.substring(0, MAX_LINE_LENGTH) + "...";
	}
}
