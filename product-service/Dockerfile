# --- Stage 1: Build the productRow service from the monorepo root ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

# Copy the shared Maven wrapper and parent POM.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy the reactor modules so Maven can read the parent project.
COPY shared-domain shared-domain
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY order-service order-service
COPY product-service product-service

# Build only the productRow service and its parent.
RUN ./mvnw package -pl product-service -am -DskipTests -B

# --- Stage 2: Lightweight JRE runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/product-service/target/product-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
