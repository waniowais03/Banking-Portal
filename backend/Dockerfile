
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src

WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/backend/target/bankingportal-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8180

ENTRYPOINT ["java", "-jar", "app.jar"]
