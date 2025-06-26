# Use lightweight Java 21 runtime
FROM openjdk:21-jdk-slim

# Create working directory
WORKDIR /app

# Copy JAR file
COPY build/libs/please-all.jar app.jar

# Copy data and rate JSON files into the container
COPY src/main/resources/data ./src/main/resources/data
COPY src/main/resources/rates ./src/main/resources/rates

# Run the JAR
CMD ["java", "-jar", "app.jar"]
