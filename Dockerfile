# 1. OpenJDK 21 기반으로 빌드
FROM eclipse-temurin:21 AS build
WORKDIR /app

# Gradle Wrapper 및 프로젝트 파일 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/

# 실행 권한 추가 및 빌드 수행
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# 2. 실행 컨테이너 설정
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Scouter Agent 설치
RUN apt-get update && \
    apt-get install -y curl tar ca-certificates && \
    curl -fSL -o /tmp/scouter-agent.tar.gz https://github.com/scouter-project/scouter/releases/download/v2.7.0/scouter.agent.java-2.7.0.tar.gz && \
    mkdir -p /opt/scouter-agent && \
    tar -xzf /tmp/scouter-agent.tar.gz -C /opt/scouter-agent --strip-components=1 && \
    rm /tmp/scouter-agent.tar.gz


# Scouter 설정 파일 작성
RUN mkdir -p /opt/scouter-agent/conf
COPY scouter.conf /opt/scouter-agent/conf/scouter.conf

# 3. 환경 변수는 Dockerfile에서 직접 설정하지 않고, 외부에서 제공
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/scouter-agent/lib/scouter.agent.jar -Dscouter.config=/opt/scouter-agent/conf/scouter.conf"

ENTRYPOINT ["java", "-jar", "app.jar"]
