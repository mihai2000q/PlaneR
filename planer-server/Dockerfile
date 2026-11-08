# Build context is the repository root:  docker build -f planer-server/Dockerfile .

# ---- Build -----------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY planer-shared ./planer-shared
COPY planer-server ./planer-server

# The desktop and Android modules are not part of the server image (and the Android one would need an SDK),
# so drop them from the build.
RUN sed -i '/planer-desktop\|planer-android/d' settings.gradle.kts \
 && chmod +x gradlew \
 && ./gradlew :planer-server:buildFatJar --no-daemon

# ---- Runtime ---------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home planer
COPY --from=build /src/planer-server/build/libs/planer-server-all.jar app.jar
USER planer

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
