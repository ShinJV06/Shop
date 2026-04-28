FROM eclipse-temurin:21-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar demo.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "demo.war"]
