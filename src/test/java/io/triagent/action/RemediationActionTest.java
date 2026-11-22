package io.triagent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RemediationActionTest {

    static final Instant NOW = Instant.parse("2026-11-21T02:00:00Z");

    @Test
    @DisplayName("새 조치는 승인 대기 상태다")
    void request() {
        assertThat(pendingAction().getStatus()).isEqualTo(ActionStatus.PENDING);
    }

    @Test
    @DisplayName("승인하면 누가 언제 결정했는지 기록한다")
    void approve() {
        RemediationAction action = pendingAction();

        action.approve("U_LEAD", NOW);

        assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
        assertThat(action.getDecidedBy()).isEqualTo("U_LEAD");
        assertThat(action.getDecidedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("승인, 거절은 한 번만 할 수 있다")
    void decideOnce() {
        RemediationAction action = pendingAction();
        action.reject("U_LEAD", NOW);

        assertThatThrownBy(() -> action.approve("U_OTHER", NOW))
                .isInstanceOf(ActionAlreadyDecidedException.class)
                .hasMessageContaining("REJECTED");
        assertThat(action.getStatus()).isEqualTo(ActionStatus.REJECTED);
    }

    @Test
    @DisplayName("실행 결과는 승인된 조치에만 기록할 수 있다")
    void finishAfterApproval() {
        RemediationAction action = pendingAction();

        assertThatThrownBy(() -> action.markExecuted("done", NOW))
                .isInstanceOf(IllegalStateException.class);

        action.approve("U_LEAD", NOW);
        action.markExecuted("done", NOW);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.EXECUTED);
        assertThat(action.getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("요청자는 자기 요청을 승인할 수 없다")
    void selfApproval() {
        RemediationAction action = pendingAction();

        assertThatThrownBy(() -> action.approve("U_ONCALL", NOW))
                .isInstanceOf(DecisionNotAllowedException.class);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING);
    }

    private static RemediationAction pendingAction() {
        return RemediationAction.request(
                1L, "payment-api", ActionType.ROLLBACK, "a1b2c3d 배포 후 5xx 증가", "U_ONCALL", NOW);
    }
}
