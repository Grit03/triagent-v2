# Triagent

> 장애 알림이 오면 로그, 지표, 최근 배포를 조사해 원인을 요약하고 메신저로 보고하는 개발팀용 장애 대응 에이전트

Triagent는 triage(분류, 응급 처치)와 agent를 합친 이름입니다.

## 상태

1단계(핵심 흐름)와 2단계(조치 승인, 팀별 권한, 감사 로그, 에이전트의 조치 제안)를 마쳤습니다.

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

`PostgresMigrationTest`는 Testcontainers로 PostgreSQL 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

## Docker Compose로 실행

JDK 없이 Docker만 있으면 Triagent와 PostgreSQL을 함께 띄울 수 있습니다. 이미지 빌드(jar 빌드 포함)도 Docker 안에서 진행합니다.

```bash
docker compose up -d --build
```

Slack, Claude, GitHub 연동을 켜려면 저장소 루트의 `.env` 파일이나 셸 환경 변수로 아래 [설정](#설정)의 값을 넣습니다. 넣지 않으면 해당 연동 없이 동작합니다.

테스트 알림을 보내고 장애가 열렸는지 확인합니다.

```bash
curl -H 'Content-Type: application/json' --data @src/test/resources/alertmanager/firing.json http://localhost:8080/api/v1/alerts/alertmanager
```

```bash
curl http://localhost:8080/api/v1/incidents/1
```

데이터는 `postgres-data` 볼륨에 남으므로 컨테이너를 다시 만들어도 유지됩니다. 볼륨까지 지우려면 `docker compose down -v`를 실행합니다.

### 데모 장애 재현

Compose에는 장애를 재현하기 위한 구성이 함께 들어 있습니다.

| 서비스 | 역할 | 주소 |
| --- | --- | --- |
| `payment-api` | 일부러 에러를 낼 수 있는 데모 결제 서버([demo/payment-api](demo/payment-api)) | http://localhost:8081 |
| `traffic` | 결제 요청을 0.2초마다 보내 지표와 로그가 쌓이게 합니다 | |
| `prometheus` | `payment-api`의 지표를 `service="payment-api"` 라벨을 붙여 수집합니다 | http://localhost:9090 |
| `loki` | `payment-api`가 보내는 로그를 같은 라벨로 저장합니다 | http://localhost:3100 |
| `alertmanager` | Prometheus 알림을 Triagent 웹훅으로 전달합니다([알림 규칙](demo/alert-rules.yml)) | http://localhost:9093 |

결제 요청의 절반이 실패하도록 장애를 일으킵니다. `rate=0`으로 다시 보내면 정상으로 돌아옵니다.

```bash
curl -X PUT "http://localhost:8081/chaos/error-rate?rate=0.5"
```

그 뒤로는 사람이 알림을 보내지 않아도 아래 순서로 이어집니다. 괄호 안은 로컬에서 직접 잰 시간입니다.

1. Prometheus가 최근 1분 에러율이 5%를 넘는 상태가 30초 이어진 것을 보고 `HighErrorRate` 알림을 냅니다.
2. Alertmanager가 알림을 Triagent로 보내고, Triagent가 장애를 엽니다(장애를 일으킨 지 약 50초).
3. Triagent가 Prometheus에서 장애 30분 전과 현재의 에러율을, Loki에서 `Connection pool exhausted` 에러 로그를 근거로 모아 보고합니다. 스택을 띄운 지 30분이 안 됐다면 장애 전 값은 `데이터 없음`으로 나옵니다.
4. `rate=0`으로 되돌리면 Alertmanager가 해소 알림을 보내고, Triagent가 장애를 `RESOLVED`로 닫습니다(되돌린 지 약 90초).

보고는 Slack을 설정했다면 Slack으로, 아니면 `docker compose logs triagent`에 남습니다. `ANTHROPIC_API_KEY`를 넣으면 Claude가 근거를 분석하고, 근거가 충분할 때 조치를 제안합니다.

데모는 API 비용을 줄이기 위해 `claude-haiku-5-5`, effort `low`, 최대 4000토큰으로 실행합니다. 운영 기본값(`claude-opus-5-5`, effort `medium`)으로 돌려 보려면 `.env`에 `ANTHROPIC_MODEL`, `ANTHROPIC_EFFORT`, `ANTHROPIC_MAX_TOKENS`를 넣습니다. 장애 하나에 분석은 한 번만 실행됩니다. 같은 장애의 알림이 반복돼도 새 장애를 만들지 않기 때문입니다.

실제 Slack과 Claude(Haiku 5.5)로 돌린 결과는 다음과 같습니다.

- 장애를 일으키고 44초 뒤 장애가 열렸고, 약 11초 뒤 분석이 담긴 보고가 Slack 채널에 도착했습니다.
- Claude는 Loki 에러 로그 20건이 모두 `Connection pool exhausted: payment-db`인 점을 들어 커넥션 풀 고갈을 가장 유력한 원인으로 꼽았습니다. GitHub 저장소가 연결되지 않았고 장애 전 지표가 없다는 점도 함께 적었습니다.
- 롤백이나 재시작이 해결책이라는 근거가 없다며 조치는 제안하지 않았습니다. 그래서 승인 버튼 메시지도 나가지 않았습니다.

장애와 감사 로그는 API로 확인합니다.

```bash
curl http://localhost:8080/api/v1/incidents/1
```

```bash
curl "http://localhost:8080/api/v1/audit-logs?incidentId=1"
```

## 설정

| 환경 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL 주소(예: `jdbc:postgresql://localhost:5432/triagent`). `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`와 함께 씁니다. 비워 두면 메모리 DB(H2)를 써서 재시작하면 데이터가 사라집니다. | (없음, H2) |
| `SLACK_WEBHOOK_URL` | 조사 결과를 보낼 Slack Incoming Webhook URL. 비워 두면 로그로만 남깁니다. | (없음) |
| `LOKI_URL` | Loki 주소(예: `http://localhost:3100`). 비워 두면 로그 수집을 끕니다. | (없음) |
| `PROMETHEUS_URL` | Prometheus 주소(예: `http://localhost:9090`). 비워 두면 지표 수집을 끕니다. | (없음) |
| `ANTHROPIC_API_KEY` | Claude API 키. 있으면 Claude가 원인 후보와 권장 조치를 정리하고, 없으면 규칙 기반 요약을 씁니다. | (없음) |
| `GITHUB_TOKEN` | GitHub Personal Access Token. 비공개 저장소 조회나 요청 한도(비인증 시간당 60회)를 늘릴 때 필요합니다. | (없음) |
| `SLACK_SIGNING_SECRET` | Slack 앱의 Signing Secret. 승인, 거절 버튼 요청이 Slack에서 왔는지 검증합니다. 비워 두면 모든 버튼 요청을 거부합니다. | (없음) |

서비스와 GitHub 저장소 연결은 `application.yml`의 `triagent.github.repositories`에 적습니다.

```yaml
triagent:
  github:
    repositories:
      payment-api: acme/payment-api   # 알림의 service 라벨: owner/repo
```

## Slack 승인 버튼 설정

조치(롤백, 재배포, 재시작)를 Slack 버튼으로 승인하려면 Slack 앱이 필요합니다.

1. [Slack API](https://api.slack.com/apps)에서 앱을 만들고 Incoming Webhooks를 켠 뒤, 채널용 웹훅 URL을 `SLACK_WEBHOOK_URL`에 넣습니다.
2. Basic Information > App Credentials의 Signing Secret을 `SLACK_SIGNING_SECRET`에 넣습니다.
3. Interactivity & Shortcuts를 켜고 Request URL을 `https://<서버 주소>/api/v1/slack/interactions`로 지정합니다. 로컬에서 시험할 때는 ngrok 같은 터널로 외부 주소를 만들어야 합니다.
4. 조치를 요청하면 채널에 승인, 거절 버튼이 달린 메시지가 오고, 버튼을 누르면 메시지가 결과 문구로 바뀝니다.
5. 승인할 사람을 팀에 등록합니다. 서비스를 담당하는 팀의 구성원만 승인, 거절할 수 있고, 요청한 사람은 자기 요청을 승인할 수 없습니다. 담당 팀이 없는 서비스는 아무도 승인할 수 없습니다.

```yaml
triagent:
  access:
    teams:
      payments:
        members: [U012ABCDEF, U034GHIJKL]   # Slack 사용자 ID(프로필 > 멤버 ID 복사)
        services: [payment-api]
```

```bash
curl -X POST localhost:8080/api/v1/incidents/1/actions \
  -H 'Content-Type: application/json' \
  -d '{"type":"ROLLBACK","requestedBy":"U_ONCALL","reason":"a1b2c3d 배포 후 5xx 증가"}'
```

