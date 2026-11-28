# Stage 1: Build JAR with Maven and Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lean Temurin Alpine JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S cinepass && adduser -S cinepass -G cinepass
COPY --from=builder /app/target/*.jar app.jar
USER cinepass:cinepass
EXPOSE 8081
ENV JAVA_OPTS="-Xms128m -Xmx220m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
