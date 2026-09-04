# syntax=docker/dockerfile:1

# --- Stage 1: build the jar ---
# NOT alpine: Playwright's bundled Node/driver is a glibc binary and won't
# exec on musl. Use the Debian (jammy) based Maven image.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Download the exact Chromium build Playwright needs into an image path we copy later.
# Non-fatal: if it fails here, Playwright downloads it on first use at runtime.
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN mkdir -p /ms-playwright && \
    mvn -B -q org.codehaus.mojo:exec-maven-plugin:3.1.1:java \
      -Dexec.mainClass=com.microsoft.playwright.CLI \
      -Dexec.classpathScope=runtime \
      -Dexec.args="install chromium" || echo "browser preinstall skipped"

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Runs the default (dev) profile: H2 file DB + seed admin/demo users, works with
# zero setup. Set SPRING_PROFILES_ACTIVE=prod + DATABASE_URL for PostgreSQL.
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# System libraries the bundled Chromium needs on Ubuntu 22.04, plus fonts for the
# PDF/cards. This is Playwright's full Chromium dependency set for jammy — a
# partial list makes Playwright's host-requirement check fail at launch.
RUN apt-get update && apt-get install -y --no-install-recommends \
      libnss3 libnspr4 libdbus-1-3 libglib2.0-0 libatk1.0-0 libatk-bridge2.0-0 \
      libatspi2.0-0 libcups2 libdrm2 libgbm1 libxkbcommon0 libxcomposite1 \
      libxdamage1 libxfixes3 libxrandr2 libx11-6 libxcb1 libxext6 \
      libpango-1.0-0 libpangocairo-1.0-0 libcairo2 libcairo-gobject2 libasound2 \
      libgtk-3-0 libgdk-pixbuf-2.0-0 libx11-xcb1 libxcursor1 libxi6 libxtst6 \
      libxss1 libxrender1 libfontconfig1 libdbus-glib-1-2 libvulkan1 \
      fonts-liberation fonts-noto-cjk fonts-noto-color-emoji fonts-unifont \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p uploads/photos data

COPY --from=build /ms-playwright /ms-playwright
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# shell form so ${PORT} (set by Render) is expanded; prod profile also reads it.
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
