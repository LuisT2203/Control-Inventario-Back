# Etapa 1: compila el jar (con tests, nada de -DskipTests para prod)
FROM maven:3.9-eclipse-temurin-17 AS build
# Neutraliza args inyectados por el builder (rompian el build con "/root/.m2")
ENV MAVEN_ARGS=
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q package

# Etapa 2: imagen minima solo para correr
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/inventario-api-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
