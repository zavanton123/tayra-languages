# The backend as one container. Build the jar first: ./gradlew :backend:bootJar
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 tayra
USER tayra
COPY build/libs/backend.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/backend.jar"]
