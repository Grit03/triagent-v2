package io.triagent.investigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.triagent.incident.IncidentSnapshot;
import io.triagent.incident.IncidentService;

/**
 * Spring 컨텍스트 없이 Mockito로 협력 객체를 대신해, 워크플로 로직만 빠르게 검증합니다.
 */
class InvestigationServiceTest {

	IncidentService incidentService = mock(IncidentService.class);

	ContextCollector okCollector = collector("github", () -> Evidence.success("github", "최근 커밋 3건"));

	ContextCollector brokenCollector = collector("loki", () -> {
		throw new IllegalStateException("connection refused");
	});

	Notifier failingNotifier = mock(Notifier.class);

	Notifier notifier = mock(Notifier.class);

	IncidentAnalyzer analyzer = mock(IncidentAnalyzer.class);

	InvestigationService service = new InvestigationService(incidentService,
			List.of(okCollector, brokenCollector), analyzer, List.of(failingNotifier, notifier));

	@Test
	void collectsEvidenceAndKeepsGoingWhenToolsFail() {
		given(incidentService.findSnapshot(1L)).willReturn(Optional.of(incident()));
		willThrow(new IllegalStateException("slack down")).given(failingNotifier).send(any());
		given(analyzer.analyze(any(), any())).willReturn("원인 후보: 최근 배포");

		service.investigate(1L);

		ArgumentCaptor<InvestigationReport> captor = ArgumentCaptor.forClass(InvestigationReport.class);
		verify(notifier).send(captor.capture());
		InvestigationReport report = captor.getValue();
		assertThat(report.incident().service()).isEqualTo("payment-api");
		assertThat(report.evidences()).containsExactly(
				Evidence.success("github", "최근 커밋 3건"),
				Evidence.failure("loki", "조회 실패: connection refused"));
		assertThat(report.analysis()).isEqualTo("원인 후보: 최근 배포");
	}

	@Test
	void skipsWhenIncidentIsMissing() {
		given(incidentService.findSnapshot(1L)).willReturn(Optional.empty());

		service.investigate(1L);

		verify(notifier, never()).send(any());
	}

	private static IncidentSnapshot incident() {
		return new IncidentSnapshot(1L, "HighErrorRate", "payment-api", "critical", "5xx > 5%",
				Instant.parse("2026-11-21T01:00:00Z"));
	}

	private static ContextCollector collector(String source, java.util.function.Supplier<Evidence> result) {
		return new ContextCollector() {
			@Override
			public String source() {
				return source;
			}

			@Override
			public Evidence collect(IncidentSnapshot incident) {
				return result.get();
			}
		};
	}
}
