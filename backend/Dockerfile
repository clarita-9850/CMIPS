## Multi-stage build to avoid relying on mvnw/.mvn in context

# Build stage - use standard Maven image with JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first to leverage Docker cache for dependencies
COPY pom.xml .

# Copy libs folder for local dependencies
COPY libs ./libs

# Download all dependencies including Lombok annotation processor
RUN mvn -B dependency:resolve dependency:resolve-plugins

# Copy source code
COPY src ./src

# Build the application - skip tests entirely
RUN mvn -B -DskipTests -Dmaven.test.skip=true clean package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built jar from the build stage
COPY --from=build /app/target/cmips-mvp-1.0.0.jar /app/app.jar

# Create reports directory
RUN mkdir -p /app/reports && chmod 755 /app/reports

EXPOSE 8081

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]


