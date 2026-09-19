# --- Stage 1: Käännetään sovellus monorepon yhteisestä juuresta ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

# Kopioidaan ylätason yhteinen wrapperi ja ylä-POM kontin sisään
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Kopioidaan molemmat moduulikansiot kokonaisuudessaan
COPY shared-domain shared-domain
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY product-service product-service
COPY order-service order-service
COPY facade facade

# Ajetaan käännös vain terveyspulssille monorepo-lipuilla
RUN ./mvnw package -pl health-pulse-service -am -DskipTests -B

# --- Stage 2: Kevyt JRE-runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/health-pulse-service/target/health-pulse-service-*.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
