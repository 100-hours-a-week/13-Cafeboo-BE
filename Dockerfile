# 1. OpenJDK 21 기반으로 빌드
FROM eclipse-temurin:21 AS build
WORKDIR /app

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/

RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# 2. 실행 컨테이너 설정
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Scouter Agent 설치 및 envsubst 포함 유틸 설치
RUN apt-get update && \
    apt-get install -y curl tar ca-certificates gettext-base && \
    curl -fSL -o /tmp/scouter-all.tar.gz \
     https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-all-2.20.0.tar.gz && \
    mkdir -p /tmp/scouter && \
    tar -xzf /tmp/scouter-all.tar.gz -C /tmp/scouter --strip-components=1 && \
    mkdir -p /opt/scouter-agent && \
    mv /tmp/scouter/agent.java/* /opt/scouter-agent/ && \
    rm -rf /tmp/scouter /tmp/scouter-all.tar.gz

# scouter.conf 템플릿 복사
RUN mkdir -p /opt/scouter-agent/conf
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src

EXPOSE 8080

ENTRYPOINT [ "sh", "-c", "\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java -javaagent:/opt/scouter-agent/lib/scouter.agent.jar -Dscouter.config=/opt/scouter-agent/conf/scouter.conf -jar app.jar" ]