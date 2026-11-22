package io.triagent.incident;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
public class IncidentService {

	private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

	private final IncidentRepository incidentRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final TransactionTemplate transactionTemplate;

	public IncidentService(IncidentRepository incidentRepository, ApplicationEventPublisher eventPublisher,
			PlatformTransactionManager transactionManager) {
		this.incidentRepository = incidentRepository;
		this.eventPublisher = eventPublisher;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * 같은 fingerprint로 열린 장애가 없을 때만 새 장애를 엽니다.
	 * <p>
	 * 같은 알림이 동시에 두 번 들어오면 두 요청 모두 "열린 장애 없음"을 보고 저장하려 합니다. 이때는
	 * {@code open_fingerprint} unique 제약이 나중 INSERT를 거부합니다. 거부된 트랜잭션은 롤백 전용이 되어
	 * 안에서 예외를 잡아도 커밋할 수 없으므로, 트랜잭션을 이 메서드 안에서 직접 열고 바깥에서 예외를 처리합니다.
	 *
	 * @return 새로 연 장애의 ID. 이미 열린 장애가 있으면 비어 있습니다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public Optional<Long> openIfAbsent(OpenIncidentCommand command) {
		try {
			return transactionTemplate.execute(status -> open(command));
		}
		catch (DataIntegrityViolationException ex) {
			// 다른 제약(예: 컬럼 길이) 위반까지 중복으로 삼키지 않도록, 실제로 열린 장애가 생겼는지 다시 확인합니다.
			if (findOpen(command.fingerprint()).isEmpty()) {
				throw ex;
			}
			log.info("Concurrent duplicate firing alert ignored: fingerprint={}", command.fingerprint());
			return Optional.empty();
		}
	}

	private Optional<Long> open(OpenIncidentCommand command) {
		if (findOpen(command.fingerprint()).isPresent()) {
			log.debug("Duplicate firing alert ignored: fingerprint={}", command.fingerprint());
			return Optional.empty();
		}
		Incident incident = Incident.open(command.fingerprint(), command.alertName(), command.service(),
				command.severity(), command.summary(), command.startedAt());
		Long incidentId = incidentRepository.save(incident).getId();
		log.info("Incident opened: id={}, alert={}, service={}", incidentId, command.alertName(), command.service());
		// 조사 시작은 이벤트로 알리기만 합니다. IncidentService는 누가 조사하는지 알 필요가 없습니다.
		eventPublisher.publishEvent(new IncidentOpenedEvent(incidentId, Instant.now()));
		return Optional.of(incidentId);
	}

	/**
	 * 같은 fingerprint로 열린 장애를 모두 해소합니다. unique 제약을 추가하기 전에 중복으로 열린 장애가 남아 있어도
	 * 여기서 함께 닫힙니다.
	 *
	 * @return 해소한 장애의 ID. 열린 장애가 없으면 비어 있습니다.
	 */
	@Transactional
	public List<Long> resolveIfOpen(String fingerprint, Instant resolvedAt) {
		return incidentRepository.findAllByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN).stream()
				.map(incident -> {
					// 변경 감지(dirty checking)로 커밋 시점에 UPDATE되므로 save()를 다시 부르지 않습니다.
					incident.resolve(resolvedAt);
					log.info("Incident resolved: id={}", incident.getId());
					eventPublisher.publishEvent(new IncidentResolvedEvent(incident.getId(), Instant.now()));
					return incident.getId();
				})
				.toList();
	}

	public IncidentResponse getIncident(Long id) {
		return incidentRepository.findById(id)
				.map(IncidentResponse::from)
				.orElseThrow(() -> new IncidentNotFoundException(id));
	}

	/** 다른 패키지에서 장애 정보가 필요할 때 엔티티 대신 읽기 전용 복사본을 돌려줍니다. */
	public Optional<IncidentSnapshot> findSnapshot(Long id) {
		return incidentRepository.findById(id).map(IncidentSnapshot::from);
	}

	private Optional<Incident> findOpen(String fingerprint) {
		return incidentRepository.findByOpenFingerprint(fingerprint);
	}
}
