package io.triagent.incident;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

	/** 메서드 이름으로 쿼리를 만들어 주는 Spring Data의 Query Method입니다. */
	Optional<Incident> findFirstByFingerprintAndStatus(String fingerprint, IncidentStatus status);
}
