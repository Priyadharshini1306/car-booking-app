# =====================================================
# FIX: Stage 2 was using eclipse-temurin:21-jdk
# which is a full JDK — very large image (~700MB).
# Your app only needs the JRE (Java Runtime) to RUN,
# not the full JDK (which is for compiling).
# The alpine variant is a smaller Linux base.
# → Changed to eclipse-temurin:21-jre-alpine (~200MB)
# This makes your Docker image faster to build and
# faster to deploy on Render.
# =====================================================

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]