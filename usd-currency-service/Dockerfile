# --- Stage 1: Käännetään sovellus monorepon yhteisestä juuresta ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

# Kopioidaan ylätason yhteinen wrapperi ja ylä-POM kontin sisään
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Kopioidaan molemmat moduulikansiot kokonaisuudessaan
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service

# Ajetaan käännös vain tälle kyseiselle moduulille monorepo-lipuilla
RUN ./mvnw package -pl usd-currency-service -am -DskipTests -B

# --- Stage 2: Kevyt JRE-runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/usd-currency-service/target/usd-currency-service-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
