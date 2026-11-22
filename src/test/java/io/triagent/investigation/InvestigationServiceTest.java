package io.triagent.investigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.triagent.action.ActionResponse;
import io.triagent.action.ActionService;
import io.triagent.action.ActionStatus;
import io.triagent.action.ActionType;
import io.triagent.incident.IncidentService;
import io.triagent.incident.IncidentSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Spring 컨텍스트 없이 Mockito로 협력 객체를 대신해, 워크플로 로직만 빠르게 검증합니다. */
class InvestigationServiceTest {

    IncidentService incidentService = mock(IncidentService.class);

    ContextCollector okCollector =
            collector("github", () -> Evidence.success("github", "최근 커밋 3건"));

    ContextCollector brokenCollector =
            collector(
                    "loki",
                    () -> {
                        throw new IllegalStateException("connection refused");
                    });

    Notifier failingNotifier = mock(Notifier.class);

    Notifier notifier = mock(Notifier.class);

    IncidentAnalyzer analyzer = mock(IncidentAnalyzer.class);

    ActionService actionService = mock(ActionService.class);

    InvestigationService service =
            new InvestigationService(
                    incidentService,
                    List.of(okCollector, brokenCollector),
                    analyzer,
                    actionService,
                    List.of(failingNotifier, notifier));

    @Test
    @DisplayName("일부 도구가 실패해도 나머지 근거를 모아 보고한다")
    void collectorFailure() {
        given(incidentService.findSnapshot(1L)).willReturn(Optional.of(incident()));
        willThrow(new IllegalStateException("slack down")).given(failingNotifier).send(any());
        given(analyzer.analyze(any(), any())).willReturn("원인 후보: 최근 배포");

        service.investigate(1L);

        ArgumentCaptor<InvestigationReport> captor =
                ArgumentCaptor.forClass(InvestigationReport.class);
        verify(notifier).send(captor.capture());
        InvestigationReport report = captor.getValue();
        assertThat(report.incident().service()).isEqualTo("payment-api");
        assertThat(report.evidences())
                .containsExactly(
                        Evidence.success("github", "최근 커밋 3건"),
                        Evidence.failure("loki", "조회 실패: connection refused"));
        assertThat(report.analysis()).isEqualTo("원인 후보: 최근 배포");
    }

    @Test
    @DisplayName("분석 텍스트와 별개로, DB에 저장된 조치 요청을 보고에 담는다")
    void requestedActions() {
        ActionResponse rollback = pendingAction(ActionType.ROLLBACK);
        given(incidentService.findSnapshot(1L)).willReturn(Optional.of(incident()));
        given(analyzer.analyze(any(), any())).willReturn("1.8.0 롤백을 승인 요청했습니다");
        given(actionService.getActions(1L)).willReturn(List.of(rollback));

        service.investigate(1L);

        assertThat(sentReport().requestedActions()).containsExactly(rollback);
    }

    @Test
    @DisplayName("조치 요청 조회에 실패해도 보고는 보내고, 확인하지 못했다는 뜻으로 null을 담는다")
    void requestedActionsLookupFailure() {
        given(incidentService.findSnapshot(1L)).willReturn(Optional.of(incident()));
        given(analyzer.analyze(any(), any())).willReturn("원인 후보: 최근 배포");
        given(actionService.getActions(1L)).willThrow(new IllegalStateException("db down"));

        service.investigate(1L);

        assertThat(sentReport().requestedActions()).isNull();
    }

    private InvestigationReport sentReport() {
        ArgumentCaptor<InvestigationReport> captor =
                ArgumentCaptor.forClass(InvestigationReport.class);
        verify(notifier).send(captor.capture());
        return captor.getValue();
    }

    private static ActionResponse pendingAction(ActionType type) {
        return new ActionResponse(
                1L,
                1L,
                "payment-api",
                type,
                "배포 직후 NPE",
                ActionStatus.PENDING,
                "triagent-agent",
                Instant.parse("2026-11-21T01:01:00Z"),
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("장애가 없으면 조사를 건너뛴다")
    void incidentMissing() {
        given(incidentService.findSnapshot(1L)).willReturn(Optional.empty());

        service.investigate(1L);

        verify(notifier, never()).send(any());
    }

    private static IncidentSnapshot incident() {
        return new IncidentSnapshot(
                1L,
                "HighErrorRate",
                "payment-api",
                "critical",
                "5xx > 5%",
                Instant.parse("2026-11-21T01:00:00Z"));
    }

    private static ContextCollector collector(
            String source, java.util.function.Supplier<Evidence> result) {
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
