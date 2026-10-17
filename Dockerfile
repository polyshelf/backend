FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
ARG MODULE

COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew \
    && ./gradlew ":${MODULE}:bootJar" --no-daemon \
    && cp "${MODULE}/build/libs/"*.jar /app/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/app.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
