package io.triagent.investigation;

import io.triagent.incident.IncidentSnapshot;

/**
 * 장애 조사에 필요한 근거를 외부 도구(배포 이력, 로그, 지표)에서 모으는 전략 인터페이스.
 * <p>
 * 새 도구를 붙일 때는 이 인터페이스의 구현체를 빈으로 등록하기만 하면 됩니다.
 */
public interface ContextCollector {

	String source();

	Evidence collect(IncidentSnapshot incident);
}
