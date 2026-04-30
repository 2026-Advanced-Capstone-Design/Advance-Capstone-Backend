# ============ Build Stage ============
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 먼저 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ============ Run Stage ============
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Tesseract OCR 설치 (한국어 + 영어 학습 데이터 포함)
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-kor tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*



EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]