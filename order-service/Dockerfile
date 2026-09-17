FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY product-service product-service
COPY order-service order-service

RUN ./mvnw package -pl order-service -am -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/order-service/target/order-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
