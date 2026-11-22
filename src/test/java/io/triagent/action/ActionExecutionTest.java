package io.triagent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import io.triagent.incident.IncidentService;
import io.triagent.incident.OpenIncidentCommand;

/**
 * 승인 → 커밋 → 별도 스레드에서 실행 → 결과 기록까지 실제 빈으로 확인합니다.
 */
@SpringBootTest
class ActionExecutionTest {

	@Autowired
	ActionService actionService;

	@Autowired
	IncidentService incidentService;

	@Autowired
	RemediationActionRepository actionRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@MockitoSpyBean
	DryRunActionExecutor executor;

	Long actionId;

	@BeforeEach
	void requestAction() {
		Long incidentId = incidentService.openIfAbsent(new OpenIncidentCommand("exec-" + System.nanoTime(),
				"HighErrorRate", "payment-api", "critical", null, Instant.parse("2026-11-21T01:00:00Z")))
				.orElseThrow();
		actionId = actionService.request(incidentId, new CreateActionRequest(ActionType.ROLLBACK, "U_ONCALL", null))
				.id();
	}

	@Test
	void approvedActionIsExecuted() throws InterruptedException {
		actionService.approve(actionId, "U_LEAD");

		ActionResponse action = awaitStatusChangeFrom(ActionStatus.APPROVED);
		assertThat(action.status()).isEqualTo(ActionStatus.EXECUTED);
		assertThat(action.result()).startsWith("드라이런: payment-api에 ROLLBACK");
	}

	@Test
	void executorFailureIsRecorded() throws InterruptedException {
		willThrow(new IllegalStateException("rollout timed out")).given(executor)
				.execute(ActionType.ROLLBACK, "payment-api");

		actionService.approve(actionId, "U_LEAD");

		ActionResponse action = awaitStatusChangeFrom(ActionStatus.APPROVED);
		assertThat(action.status()).isEqualTo(ActionStatus.FAILED);
		assertThat(action.result()).isEqualTo("실행 실패: rollout timed out");
	}

	@Test
	void rejectedActionIsNeverExecuted() {
		actionService.reject(actionId, "U_LEAD");

		verify(executor, after(500).never()).execute(ActionType.ROLLBACK, "payment-api");
		assertThat(actionService.getAction(actionId).status()).isEqualTo(ActionStatus.REJECTED);
	}

	/**
	 * 두 사람이 거의 동시에 결정하는 상황을 순서를 고정해 재현합니다.
	 * 승인 쪽이 PENDING을 읽은 뒤, 다른 스레드의 거절이 먼저 커밋되면 승인 쪽 커밋은 실패하고 실행되지 않아야 합니다.
	 */
	@Test
	void concurrentDecisionThatCommitsLaterFailsAndIsNeverExecuted() {
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			RemediationAction action = actionRepository.findById(actionId).orElseThrow();
			CompletableFuture.runAsync(() -> actionService.reject(actionId, "U_ONCALL")).join();
			action.approve("U_LEAD", Instant.now());
		})).isInstanceOf(ObjectOptimisticLockingFailureException.class);

		assertThat(actionService.getAction(actionId).status()).isEqualTo(ActionStatus.REJECTED);
		verify(executor, after(500).never()).execute(ActionType.ROLLBACK, "payment-api");
	}

	/** 실행은 다른 스레드에서 진행되므로, 상태가 바뀔 때까지 최대 5초 기다립니다. */
	private ActionResponse awaitStatusChangeFrom(ActionStatus status) throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		ActionResponse action = actionService.getAction(actionId);
		while (action.status() == status && Instant.now().isBefore(deadline)) {
			Thread.sleep(50);
			action = actionService.getAction(actionId);
		}
		return action;
	}
}
