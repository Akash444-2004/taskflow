# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system taskflow \
    && useradd --system --gid taskflow --create-home taskflow

COPY --from=build /app/target/taskflow-api-*.jar /app/taskflow-api.jar

RUN chown -R taskflow:taskflow /app
USER taskflow

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080'

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/taskflow-api.jar"]
