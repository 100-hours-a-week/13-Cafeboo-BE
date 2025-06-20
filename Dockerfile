# ─────────────────── 1. Build stage ───────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew \
 && ./gradlew build -x test --no-daemon

# ───────────────── 2. Scouter Agent stage ────────────────
ARG SCOUTER_VER=2.20.0
FROM ubuntu:22.04 AS scouter-agent
RUN set -eux; \
    apt-get update && \
    apt-get install -y --no-install-recommends curl tar gettext-base && \
    rm -rf /var/lib/apt/lists/*

# agent.java 디렉토리만 추출
RUN set -eux; \
    mkdir -p /opt/scouter-agent; \
    curl -fsSL \
      https://github.com/scouter-project/scouter/releases/download/v${SCOUTER_VER}/scouter-all-${SCOUTER_VER}.tar.gz \
    | tar xz --strip-components=1 -C /opt/scouter-agent agent.java

# ──────────────── 3. Runtime stage ─────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1) 앱 바이너리 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 2) scouter-agent 바이너리 복사
COPY --from=scouter-agent /opt/scouter-agent /opt/scouter-agent

# 3) 사용자, 권한 설정 및 기본 conf 준비
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src
RUN set -eux; \
    useradd --system --home /home/scouter scouter && \
    chown -R scouter:scouter /opt/scouter-agent

# 4) 컨테이너 포트
EXPOSE 8080

# 5) 비루팅 환경에서 hostname 치환 후 실행
USER scouter
ENTRYPOINT ["sh", "-c", "\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java \
    -javaagent:/opt/scouter-agent/scouter.agent.jar \
    -Dscouter.config=/opt/scouter-agent/conf/scouter.conf \
    -jar /app/app.jar \
"]