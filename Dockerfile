# 1. Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew && ./gradlew build -x test --no-daemon

# 2. Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar ./app.jar

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tar gettext-base \
 && curl -fsSL \
      https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-all-2.20.0.tar.gz \
      -o scouter.tar.gz \
 && mkdir scouter-temp \
 && tar -xzf scouter.tar.gz -C scouter-temp --strip-components=1 \
 && mkdir -p /opt/scouter-agent \
 && cp -r scouter-temp/agent.java/* /opt/scouter-agent/ \
 && rm -rf scouter.tar.gz scouter-temp \
 && apt-get purge -y --auto-remove curl tar \
 && rm -rf /var/lib/apt/lists/*

COPY scouter.conf /opt/scouter-agent/conf/scouter.conf.src
RUN useradd --system --home /home/scouter scouter \
 && chown -R scouter:scouter /opt/scouter-agent

EXPOSE 8080
USER scouter

ENTRYPOINT ["sh", "-c", "\
  export HOSTNAME=$(hostname) && \
  envsubst < /opt/scouter-agent/conf/scouter.conf.src > /opt/scouter-agent/conf/scouter.conf && \
  java -javaagent:/opt/scouter-agent/scouter.agent.jar \
       -Dscouter.config=/opt/scouter-agent/conf/scouter.conf \
       -jar /app/app.jar"]