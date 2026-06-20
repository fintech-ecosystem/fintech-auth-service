FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY fintech-common-java/pom.xml fintech-common-java/pom.xml
COPY fintech-auth-service/pom.xml fintech-auth-service/pom.xml

COPY fintech-common-java/src fintech-common-java/src
RUN mvn -f fintech-common-java/pom.xml install -DskipTests -B

COPY fintech-auth-service/src fintech-auth-service/src
RUN mvn -f fintech-auth-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/fintech-auth-service/target/fintech-auth-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
