package io.triagent.incident;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncidentService {

	private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

	private final IncidentRepository incidentRepository;

	private final ApplicationEventPublisher eventPublisher;

	public IncidentService(IncidentRepository incidentRepository, ApplicationEventPublisher eventPublisher) {
		this.incidentRepository = incidentRepository;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 같은 fingerprint로 열린 장애가 없을 때만 새 장애를 엽니다.
	 *
	 * @return 새로 연 장애의 ID. 이미 열린 장애가 있으면 비어 있습니다.
	 */
	@Transactional
	public Optional<Long> openIfAbsent(OpenIncidentCommand command) {
		if (findOpen(command.fingerprint()).isPresent()) {
			log.debug("Duplicate firing alert ignored: fingerprint={}", command.fingerprint());
			return Optional.empty();
		}
		Incident incident = Incident.open(command.fingerprint(), command.alertName(), command.service(),
				command.severity(), command.summary(), command.startedAt());
		Long incidentId = incidentRepository.save(incident).getId();
		log.info("Incident opened: id={}, alert={}, service={}", incidentId, command.alertName(), command.service());
		// 조사 시작은 이벤트로 알리기만 합니다. IncidentService는 누가 조사하는지 알 필요가 없습니다.
		eventPublisher.publishEvent(new IncidentOpenedEvent(incidentId));
		return Optional.of(incidentId);
	}

	/**
	 * @return 해소한 장애의 ID. 열린 장애가 없으면 비어 있습니다.
	 */
	@Transactional
	public Optional<Long> resolveIfOpen(String fingerprint, Instant resolvedAt) {
		return findOpen(fingerprint).map(incident -> {
			// 변경 감지(dirty checking)로 커밋 시점에 UPDATE되므로 save()를 다시 부르지 않습니다.
			incident.resolve(resolvedAt);
			log.info("Incident resolved: id={}", incident.getId());
			return incident.getId();
		});
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
		return incidentRepository.findFirstByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN);
	}
}
