FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean package
FROM eclipse-temurin:21-jre-jammy
RUN groupadd -r banking && useradd -r -g banking -d /app banking && mkdir -p /app /app/statements && chown -R banking:banking /app
WORKDIR /app
COPY --from=build --chown=banking:banking /app/target/banking-services-platform-1.0.0.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
