FROM eclipse-temurin:21-jre
WORKDIR /app

COPY services/identity-bootstrap-service/target/identity-bootstrap-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
