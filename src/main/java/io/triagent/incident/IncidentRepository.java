package io.triagent.incident;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

	Optional<Incident> findFirstByFingerprintAndStatus(String fingerprint, IncidentStatus status);
}
