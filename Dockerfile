# Multi-stage build for Amazon Corretto 17
FROM amazoncorretto:17-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM amazoncorretto:17-alpine

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

WORKDIR /app

# Install required packages
RUN apk add --no-cache curl

# Copy built application
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory and change ownership (before switching to spring user)
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app && \
    chown spring:spring app.jar

USER spring:spring

# Add container labels for ELK log parsing
LABEL service.name="custom-api-svc" \
      service.version="1.0.0" \
      service.type="microservice" \
      logging.format="json" \
      logging.driver="json-file"

# Health check for container orchestration (K8s, Docker Swarm)
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8083/api/v1/health || exit 1

EXPOSE 8083

# Environment variables for logging and monitoring
ENV LOG_PATH=/app/logs
ENV LOG_FILE=custom-api-svc

# ELK Stack integration environment variables
ENV LOGSTASH_HOST=localhost
ENV LOGSTASH_PORT=5044
ENV ELASTICSEARCH_HOST=localhost
ENV ELASTICSEARCH_PORT=9200
ENV SPRING_APPLICATION_NAME=custom-api-svc

# JVM tuning for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:G1HeapRegionSize=16m \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]