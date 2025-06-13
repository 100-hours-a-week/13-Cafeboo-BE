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

# Scouter Agent 설치
RUN apt-get update && \
    apt-get install -y curl tar ca-certificates gettext-base && \
    curl -fSL -o /tmp/scouter-agent.tar.gz https://github.com/scouter-project/scouter/releases/download/v2.7.0/scouter.agent.java-2.7.0.tar.gz && \
    mkdir -p /opt/scouter-agent && \
    tar -xzf /tmp/scouter-agent.tar.gz -C /opt/scouter-agent --strip-components=1 && \
    rm /tmp/scouter-agent.tar.gz

# scouter.conf 템플릿 복사
RUN mkdir -p /opt/scouter-agent/conf
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src

# 3. 환경 변수는 Dockerfile에서 직접 설정하지 않고, 외부에서 제공
EXPOSE 8080

# 실제 scouter.conf를 동적으로 생성 후 애플리케이션 실행
ENTRYPOINT [ "sh", "-c", "\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java -javaagent:/opt/scouter-agent/lib/scouter.agent.jar -Dscouter.config=/opt/scouter-agent/conf/scouter.conf -jar app.jar" ]