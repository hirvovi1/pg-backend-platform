# --- Stage 1: Build the facade from the monorepo root ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

# Copy the shared Maven wrapper and parent POM.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy the reactor modules so Maven can resolve the facade dependencies.
COPY shared-domain shared-domain
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY product-service product-service
COPY order-service order-service
COPY facade facade

# Build only the facade and its required modules.
RUN ./mvnw package -pl facade -am -DskipTests -Dmicronaut.aot.enabled=false -B

# --- Stage 2: Lightweight JRE runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/facade/target/pg-api-facade-*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
