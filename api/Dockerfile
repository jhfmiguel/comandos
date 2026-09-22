FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 comandos
COPY --from=build /workspace/target/*.jar /app/comandos-api.jar
USER comandos
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/comandos-api.jar"]
