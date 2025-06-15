# ----------------------------
# 1. Build stage
# ----------------------------
    FROM eclipse-temurin:21-jdk-slim AS builder

    WORKDIR /app
    
    # 빌드 도구 복사
    COPY gradlew build.gradle settings.gradle ./
    COPY gradle/ gradle/
    COPY src/ src/
    
    RUN chmod +x gradlew \
     && ./gradlew build -x test --no-daemon
    
    # ----------------------------
    # 2. Runtime stage
    # ----------------------------
    ARG SCOUTER_VERSION=2.20.0
    FROM eclipse-temurin:21-jre-jammy-slim
    
    # 비-루트 사용자 생성
    RUN useradd --system --create-home --home-dir /home/scouter scouter
    
    WORKDIR /app
    
    # 애플리케이션 JAR 복사
    COPY --from=builder /app/build/libs/*.jar ./app.jar
    
    # Scouter Agent 설치 및 envsubst 포함 툴 설치
    RUN apt-get update \
     && apt-get install -y --no-install-recommends \
          curl \
          tar \
          gettext-base \
     && curl -fsSL \
          https://github.com/scouter-project/scouter/releases/download/v${SCOUTER_VERSION}/scouter-all-${SCOUTER_VERSION}.tar.gz \
          -o scouter.tar.gz \
     && mkdir scouter-temp \
     && tar -xzf scouter.tar.gz -C scouter-temp --strip-components=1 \
     && mkdir -p /opt/scouter-agent \
     && mv scouter-temp/agent.java/* /opt/scouter-agent/ \
     && rm -rf scouter.tar.gz scouter-temp \
     && apt-get purge -y --auto-remove curl tar \
     && rm -rf /var/lib/apt/lists/*
    
    # scouter.conf 템플릿 복사
    COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src
    RUN chown -R scouter:scouter /opt/scouter-agent
    
    # 포트 및 실행
    EXPOSE 8080
    
    USER scouter
    
    ENTRYPOINT ["sh","-c", "\
      export HOSTNAME=$(hostname) && \
      envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
      java -javaagent:/opt/scouter-agent/lib/scouter.agent.jar \
           -Dscouter.config=/opt/scouter-agent/conf/scouter.conf \
           -jar /app/app.jar"]