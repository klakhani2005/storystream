# Build stage: compile and package the Spring Boot jar.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Run stage: small JRE image, non-root user.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 storystream
COPY --from=build /app/target/storystream-api-1.0.0.jar app.jar
USER storystream
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
