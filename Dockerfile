# 1. OpenJDK 21 기반으로 빌드
FROM eclipse-temurin:21 AS build
WORKDIR /app

# Gradle Wrapper 및 프로젝트 파일 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/

# 실행 권한 추가 및 빌드 수행
RUN chmod +x gradlew
RUN ./gradlew build -x test

# 2. 실행 컨테이너 설정
FROM eclipse-temurin:21
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 3. 환경 변수는 Dockerfile에서 직접 설정하지 않고, 외부에서 제공
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
