FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

EXPOSE 8080

COPY --from=build /app/target/digital-bank-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
