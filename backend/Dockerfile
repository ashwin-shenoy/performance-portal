FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for healthcheck, fonts for chart rendering, tesseract for OCR
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl fontconfig fonts-dejavu-core \
        tesseract-ocr tesseract-ocr-eng \
        libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

# Create data directories
RUN mkdir -p /app/data/uploads /app/data/diagrams /app/data/reports /app/data/templates

# Copy built JAR
COPY --from=build /app/target/performance-portal-backend.jar app.jar

# Copy templates from source (relative to backend directory context)
COPY src/main/resources/templates /app/data/templates/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
