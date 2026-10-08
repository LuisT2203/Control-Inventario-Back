# Etapa 1: compila el jar (con tests, nada de -DskipTests para prod)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./
RUN chmod +x mvnw
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package

# Etapa 2: imagen minima solo para correr
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/inventario-api-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
