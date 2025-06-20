# ─────────────────── 1. Build stage ───────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew \
 && ./gradlew build -x test --no-daemon

# ──────────────── 2. Scouter Agent stage ────────────────
FROM ubuntu:22.04 AS scouter-agent

RUN set -eux; \
    # 1) curl, tar 설치  
    apt-get update && \
    aapt-get install -y --no-install-recommends \
    curl \
    tar \
    ca-certificates && \
    rm -rf /var/lib/apt/lists/*; \
    \
    # 2) 전체 tarball 다운로드 (버전 하드코딩)  
    curl -fsSL \
      https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-all-2.20.0.tar.gz \
      -o /tmp/scouter.tar.gz; \
    \
    # 3) 임시 디렉토리에 모두 풀기  
    mkdir -p /tmp/scouter; \
    tar -xzf /tmp/scouter.tar.gz -C /tmp/scouter; \
    \
    # 4) agent.java 안의 내용만 복사  
    mkdir -p /opt/scouter-agent; \
    cp -r /tmp/scouter/agent.java/* /opt/scouter-agent; \
    \
    # 5) 정리  
    rm -rf /tmp/scouter /tmp/scouter.tar.gz; \
    apt-get purge -y --auto-remove curl tar && \
    rm -rf /var/lib/apt/lists/*

# ──────────────── 3. Runtime stage ─────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1) 앱 바이너리 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 2) Scouter Agent 복사
COPY --from=scouter-agent /opt/scouter-agent /opt/scouter-agent

# 3) Scouter 설정 복사
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src

RUN useradd --system --home /home/scouter scouter \
 && chown -R scouter:scouter /opt/scouter-agent

EXPOSE 8080
USER scouter

ENTRYPOINT ["sh","-c", "\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java -javaagent:/opt/scouter-agent/scouter.agent.jar \
       -Dscouter.config=/opt/scouter-agent/conf/scouter.conf \
       -jar /app/app.jar"]