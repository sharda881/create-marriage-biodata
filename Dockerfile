# --- Stage 1: Build Stage ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copy only the pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Run Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# ✅ Install required Chromium runtime dependencies (Alpine)
RUN apk add --no-cache \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    ttf-freefont \
    libstdc++

# ✅ Restrict Playwright to Chromium only
ENV PLAYWRIGHT_BROWSERS=chromium

# 3. Create the uploads folder so the app doesn't crash when saving photos
RUN mkdir -p uploads/photos

# 4. Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# 5. Render uses a dynamic port. This command tells Spring Boot to listen
# to the port Render assigns, or default to 8080.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]