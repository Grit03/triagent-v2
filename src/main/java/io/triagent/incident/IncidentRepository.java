package io.triagent.incident;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByOpenFingerprint(String openFingerprint);

    List<Incident> findAllByFingerprintAndStatus(String fingerprint, IncidentStatus status);
}
