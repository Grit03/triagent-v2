package io.triagent.incident;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final TransactionTemplate transactionTemplate;

    public IncidentService(
            IncidentRepository incidentRepository,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.incidentRepository = incidentRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 같은 알림이 동시에 들어오면 {@code open_fingerprint} unique 제약이 나중 INSERT를 거부합니다. 거부된 트랜잭션은 롤백 전용이 되어 안에서
     * 예외를 잡아도 커밋할 수 없으므로, 트랜잭션을 직접 열고 바깥에서 예외를 처리합니다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<Long> openIfAbsent(OpenIncidentCommand command) {
        try {
            return transactionTemplate.execute(status -> open(command));
        } catch (DataIntegrityViolationException ex) {
            // 다른 제약(예: 컬럼 길이) 위반까지 중복으로 삼키지 않도록, 실제로 열린 장애가 생겼는지 다시 확인합니다.
            if (findOpen(command.fingerprint()).isEmpty()) {
                throw ex;
            }
            log.info(
                    "Concurrent duplicate firing alert ignored: fingerprint={}",
                    command.fingerprint());
            return Optional.empty();
        }
    }

    private Optional<Long> open(OpenIncidentCommand command) {
        if (findOpen(command.fingerprint()).isPresent()) {
            log.debug("Duplicate firing alert ignored: fingerprint={}", command.fingerprint());
            return Optional.empty();
        }
        Incident incident =
                Incident.open(
                        command.fingerprint(),
                        command.alertName(),
                        command.service(),
                        command.severity(),
                        command.summary(),
                        command.startedAt());
        Long incidentId = incidentRepository.save(incident).getId();
        log.info(
                "Incident opened: id={}, alert={}, service={}",
                incidentId,
                command.alertName(),
                command.service());
        eventPublisher.publishEvent(new IncidentOpenedEvent(incidentId, Instant.now()));
        return Optional.of(incidentId);
    }

    /** unique 제약을 추가하기 전에 중복으로 열린 장애가 남아 있을 수 있어, 하나만이 아니라 모두 해소합니다. */
    @Transactional
    public List<Long> resolveIfOpen(String fingerprint, Instant resolvedAt) {
        return incidentRepository
                .findAllByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN)
                .stream()
                .map(
                        incident -> {
                            incident.resolve(resolvedAt);
                            log.info("Incident resolved: id={}", incident.getId());
                            eventPublisher.publishEvent(
                                    new IncidentResolvedEvent(incident.getId(), Instant.now()));
                            return incident.getId();
                        })
                .toList();
    }

    public IncidentResponse getIncident(Long id) {
        return incidentRepository
                .findById(id)
                .map(IncidentResponse::from)
                .orElseThrow(() -> new IncidentNotFoundException(id));
    }

    public Optional<IncidentSnapshot> findSnapshot(Long id) {
        return incidentRepository.findById(id).map(IncidentSnapshot::from);
    }

    private Optional<Incident> findOpen(String fingerprint) {
        return incidentRepository.findByOpenFingerprint(fingerprint);
    }
}
