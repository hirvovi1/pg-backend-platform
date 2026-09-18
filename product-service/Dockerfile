# --- Stage 1: Build the productRow service from the monorepo root ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

# Copy the shared Maven wrapper and parent POM.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy the reactor modules so Maven can read the parent project.
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY orderRow-service orderRow-service
COPY productRow-service productRow-service

# Build only the productRow service and its parent.
RUN ./mvnw package -pl productRow-service -am -DskipTests -B

# --- Stage 2: Lightweight JRE runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/productRow-service/target/productRow-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
