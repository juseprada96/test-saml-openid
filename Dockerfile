# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy dependency descriptors first for better layer caching
COPY pom.xml settings-central.xml ./
RUN mvn dependency:go-offline -s settings-central.xml -q

# Copy source and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -s settings-central.xml -q

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/sample-openid-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
