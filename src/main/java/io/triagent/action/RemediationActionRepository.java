package io.triagent.action;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemediationActionRepository extends JpaRepository<RemediationAction, Long> {

    List<RemediationAction> findByIncidentIdOrderByIdAsc(Long incidentId);
}
