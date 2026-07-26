# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -N install
COPY common-library ./common-library
RUN mvn -f common-library/pom.xml clean install -DskipTests
COPY ingestion-service ./ingestion-service
RUN mvn -f ingestion-service/pom.xml clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ingestion-service/target/*.jar app.jar
EXPOSE 5555
ENTRYPOINT ["java", "-jar", "app.jar"]
