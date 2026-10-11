FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY settings.gradle.kts ./
COPY gradle ./gradle
COPY core/build.gradle.kts core/
COPY gateway/build.gradle.kts gateway/
COPY gateway/src gateway/src

RUN --mount=type=cache,target=/root/.gradle \
    java -jar gradle/wrapper/gradle-wrapper.jar :gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/gateway/build/libs/*-SNAPSHOT.jar app.jar
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
