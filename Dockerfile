FROM maven:3.9.16-eclipse-temurin-25 AS construcao
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home forcamente
COPY --from=construcao /app/target/*.jar app.jar
USER forcamente
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
