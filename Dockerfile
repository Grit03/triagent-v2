# syntax=docker/dockerfile:1
# Triagent 이미지. 저장소만 있으면 jar 빌드부터 이미지 생성까지 이 파일 하나로 끝납니다.
# 실행 단계 구성은 Spring Boot 공식 문서의 권장 방식(계층별 jar 추출)을 따릅니다.
# https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html

# 1) jar 빌드: Gradle Wrapper로 bootJar만 만듭니다(테스트는 CI에서 실행).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# 의존성을 먼저 받아 두면, 소스만 바뀌었을 때 이 단계를 캐시에서 재사용합니다.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

# 2) jar를 의존성, 로더, 애플리케이션 계층으로 풀기
FROM eclipse-temurin:17-jre AS extract
WORKDIR /builder
COPY --from=build /workspace/build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 3) 실행 이미지: 자주 바뀌지 않는 계층부터 복사해, 코드만 바뀌면 마지막 계층만 새로 받게 합니다.
FROM eclipse-temurin:17-jre
WORKDIR /application
COPY --from=extract /builder/extracted/dependencies/ ./
COPY --from=extract /builder/extracted/spring-boot-loader/ ./
COPY --from=extract /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract /builder/extracted/application/ ./
# 컨테이너 안에서 root 권한으로 실행하지 않습니다.
RUN useradd --system --uid 1001 triagent
USER triagent
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
