# Triagent

> 장애 알림이 오면 로그, 지표, 최근 배포를 조사해 원인을 요약하고 메신저로 보고하는 개발팀용 장애 대응 에이전트

Triagent는 triage(분류, 응급 처치)와 agent를 합친 이름입니다.

## 상태

1단계(핵심 흐름)를 개발하고 있습니다.

## 로컬 실행

필요한 것: JDK 17 이상. Gradle은 저장소에 포함된 Gradle Wrapper(`./gradlew`)를 사용하므로 따로 설치하지 않아도 됩니다.

```bash
./gradlew bootRun
```

```bash
curl http://localhost:8080/actuator/health
```

테스트 실행:

```bash
./gradlew test
```

## 설정

| 환경 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `SLACK_WEBHOOK_URL` | 조사 결과를 보낼 Slack Incoming Webhook URL. 비워 두면 로그로만 남깁니다. | (없음) |
| `GITHUB_TOKEN` | GitHub Personal Access Token. 비공개 저장소 조회나 요청 한도(비인증 시간당 60회)를 늘릴 때 필요합니다. | (없음) |

서비스와 GitHub 저장소 연결은 `application.yml`의 `triagent.github.repositories`에 적습니다.

```yaml
triagent:
  github:
    repositories:
      payment-api: acme/payment-api   # 알림의 service 라벨: owner/repo
```
