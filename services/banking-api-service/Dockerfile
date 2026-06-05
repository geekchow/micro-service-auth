FROM eclipse-temurin:21-jre
WORKDIR /app

COPY services/banking-api-service/target/banking-api-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
