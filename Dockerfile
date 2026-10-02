# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 radar && mkdir /data && chown radar /data
USER radar
WORKDIR /app
COPY --from=build /src/target/radar-vagas-*.jar app.jar
ENV DB_URL=jdbc:h2:file:/data/radar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
