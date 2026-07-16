# ─── STAGE 1: BUILD ───────────────────────────────────────────
# Use full JDK with Maven to compile the application
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper and pom.xml first
# Docker caches this layer — if pom.xml hasn't changed,
# dependencies are not re-downloaded on next build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download all dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application, skip tests
# Tests run in CI pipeline separately
RUN ./mvnw clean package -DskipTests -B

# ─── STAGE 2: RUNTIME ─────────────────────────────────────────
# Use JRE only — much smaller than JDK
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
# Never run containers as root in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy ONLY the jar from builder stage
# Everything else (Maven, source, JDK) is discarded
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# JVM tuning for containers
# -XX:+UseContainerSupport: JVM respects Docker memory limits
# -XX:MaxRAMPercentage=75: Use max 75% of container memory
# -Djava.security.egd: Faster startup on Linux
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

# Health check — Docker monitors this endpoint
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]