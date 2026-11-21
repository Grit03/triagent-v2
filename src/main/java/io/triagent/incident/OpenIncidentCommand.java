package io.triagent.incident;

import java.time.Instant;

/**
 * 장애를 여는 데 필요한 값. 알림 형식(Alertmanager 등)과 상관없이 incident 패키지가 이해하는 입력입니다.
 *
 * @param fingerprint 같은 알림의 재전송을 구분하는 식별자
 */
public record OpenIncidentCommand(String fingerprint, String alertName, String service, String severity,
		String summary, Instant startedAt) {
}
