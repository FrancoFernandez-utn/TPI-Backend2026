# syntax=docker/dockerfile:1
# Build multi-stage. El contexto es la raiz del repositorio porque la imagen necesita el JAR comun:
# primero se instala agrodrones-commons en el repositorio Maven de la etapa de build y despues se
# empaqueta el servicio, que lo consume como dependencia (igual que en una maquina de desarrollo).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY agrodrones-commons/pom.xml agrodrones-commons/pom.xml
COPY agrodrones-commons/src agrodrones-commons/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f agrodrones-commons/pom.xml install -DskipTests
COPY ms-campos/pom.xml ms-campos/pom.xml
COPY ms-campos/src ms-campos/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f ms-campos/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /src/ms-campos/target/ms-campos-1.0.0.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
