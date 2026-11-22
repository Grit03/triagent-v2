package io.triagent.investigation;

import io.triagent.incident.IncidentSnapshot;

/** 새 도구(배포 이력, 로그, 지표 등)를 붙일 때는 이 인터페이스의 구현체를 빈으로 등록하기만 하면 됩니다. */
public interface ContextCollector {

    String source();

    Evidence collect(IncidentSnapshot incident);
}
