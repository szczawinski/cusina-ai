# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/cusina-ai-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV AI_PROVIDER=anthropic
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud

ENTRYPOINT ["sh", "-c", "if [ \"$AI_PROVIDER\" = \"anthropic\" ] && [ -z \"$ANTHROPIC_API_KEY\" ]; then echo 'ANTHROPIC_API_KEY is required when AI_PROVIDER=anthropic' >&2; exit 1; fi; exec java -jar /app/app.jar"]

