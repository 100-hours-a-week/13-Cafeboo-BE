# ─────────────────── 1. Build stage ───────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1) Gradle 빌드
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew \
 && ./gradlew build -x test --no-daemon

# ──────────────── 2. Scouter Agent stage ────────────────
FROM ubuntu:22.04 AS scouter-agent

RUN set -eux; \
    # 1) curl, tar, ca-certificates 설치  
    apt-get update && \
    apt-get install -y --no-install-recommends \
      curl \
      tar \
      ca-certificates && \
    rm -rf /var/lib/apt/lists/*; \
    \
    # 2) tarball 다운로드 및 추출  
    mkdir -p /tmp/extract; \
    curl -fsSL \
      https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-all-2.20.0.tar.gz \
      -o /tmp/extract/scouter.tar.gz; \
    tar -xzf /tmp/extract/scouter.tar.gz -C /tmp/extract; \
    \
    # 3) agent.java 안의 파일만 복사  
    mkdir -p /opt/scouter-agent; \
    cp -r /tmp/extract/scouter/agent.java/* /opt/scouter-agent; \
    \
    # 4) 정리 (tar는 dpkg가 필요로 하므로 purge 대상에서 제외)  
    rm -rf /tmp/extract; \
    apt-get purge -y --auto-remove curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# ──────────────── 3. Runtime stage ────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends gettext-base && \
    rm -rf /var/lib/apt/lists/*

# 1) 애플리케이션 JAR 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 2) Scouter Agent 복사
COPY --from=scouter-agent /opt/scouter-agent /opt/scouter-agent

# 3) Scouter 설정 템플릿 복사
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src

# 4) 비루팅 사용자 생성 및 권한 설정
RUN useradd --system --home /home/scouter scouter \
 && chown -R scouter:scouter /opt/scouter-agent

EXPOSE 8080
USER scouter

# 5) ENTRYPOINT: hostname 치환 + Java 에이전트 실행
ENTRYPOINT ["sh","-c","\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -javaagent:/opt/scouter-agent/scouter.agent.jar \
    -Dscouter.config=/opt/scouter-agent/conf/scouter.conf \
    -jar /app/app.jar \
"]