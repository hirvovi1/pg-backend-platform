FROM eclipse-temurin:25-jdk AS build
WORKDIR /home/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY shared-domain shared-domain
COPY usd-currency-service usd-currency-service
COPY health-pulse-service health-pulse-service
COPY product-service product-service
COPY pg-api-facade pg-api-facade
COPY order-service order-service
COPY cart-service cart-service

RUN ./mvnw package -pl cart-service -am -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /home/app
COPY --from=build /home/app/cart-service/target/cart-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
