package io.triagent.audit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 로그 조회 API. 수정, 삭제 API는 두지 않습니다.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	public List<AuditLogResponse> findByIncident(@RequestParam Long incidentId) {
		return auditLogService.findByIncident(incidentId);
	}
}
