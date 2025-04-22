FROM maven:3.8.6-openjdk-11 as builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dmanagement.metrics.enable.process.cpu=false", "-Dmanagement.metrics.enable.system.cpu=false", "-Dmanagement.metrics.enable.process=false", "-Dmanagement.metrics.enable.all=false", "-jar", "app.jar"] 