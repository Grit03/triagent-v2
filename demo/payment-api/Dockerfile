# syntax=docker/dockerfile:1
# 데모 결제 서버 이미지. 루트의 Gradle Wrapper를 쓰므로 저장소 루트를 빌드 컨텍스트로 둡니다(compose.yaml 참고).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew ./
COPY gradle gradle
COPY demo/payment-api demo/payment-api
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -p demo/payment-api bootJar

FROM eclipse-temurin:17-jre
WORKDIR /application
COPY --from=build /workspace/demo/payment-api/build/libs/*.jar application.jar
RUN useradd --system --uid 1001 demo
USER demo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
