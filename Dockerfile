# =======================================================
# Multi-Stage Build Dockerfile for Dynamic MCP Gateway
# =======================================================

# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy Maven POM and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Container with minimal JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Set timezone to Asia/Shanghai
RUN apk add --no-tzdata tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# Copy built JAR from builder stage
COPY --from=builder /build/target/McpGateWay-0.0.1.jar app.jar

# Expose HTTP port
EXPOSE 8080

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
