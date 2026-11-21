package io.triagent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RemediationActionTest {

	static final Instant NOW = Instant.parse("2026-11-21T02:00:00Z");

	@Test
	void newActionWaitsForApproval() {
		assertThat(pendingAction().getStatus()).isEqualTo(ActionStatus.PENDING);
	}

	@Test
	void approveRecordsWhoDecided() {
		RemediationAction action = pendingAction();

		action.approve("U_LEAD", NOW);

		assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
		assertThat(action.getDecidedBy()).isEqualTo("U_LEAD");
		assertThat(action.getDecidedAt()).isEqualTo(NOW);
	}

	@Test
	void decisionCanBeMadeOnlyOnce() {
		RemediationAction action = pendingAction();
		action.reject("U_LEAD", NOW);

		assertThatThrownBy(() -> action.approve("U_OTHER", NOW))
				.isInstanceOf(ActionAlreadyDecidedException.class)
				.hasMessageContaining("REJECTED");
		assertThat(action.getStatus()).isEqualTo(ActionStatus.REJECTED);
	}

	@Test
	void resultCanBeRecordedOnlyAfterApproval() {
		RemediationAction action = pendingAction();

		assertThatThrownBy(() -> action.markExecuted("done", NOW)).isInstanceOf(IllegalStateException.class);

		action.approve("U_LEAD", NOW);
		action.markExecuted("done", NOW);
		assertThat(action.getStatus()).isEqualTo(ActionStatus.EXECUTED);
		assertThat(action.getFinishedAt()).isEqualTo(NOW);
	}

	@Test
	void requesterCannotApproveOwnAction() {
		RemediationAction action = pendingAction();

		assertThatThrownBy(() -> action.approve("U_ONCALL", NOW)).isInstanceOf(DecisionNotAllowedException.class);
		assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING);
	}

	private static RemediationAction pendingAction() {
		return RemediationAction.request(1L, "payment-api", ActionType.ROLLBACK, "a1b2c3d 배포 후 5xx 증가", "U_ONCALL",
				NOW);
	}
}
