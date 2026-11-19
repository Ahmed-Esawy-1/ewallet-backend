# Stage 1: Build the Spring Boot JAR with Maven & JDK 25
FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Copy pom.xml first and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight Java 25 Runtime Image
FROM  eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-root user for container security
RUN addgroup -S spring && adduser -S spring -G spring 
USER spring:spring 

# Copy the built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar 

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "app.jar" ]
