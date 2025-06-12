# Use an official lightweight OpenJDK image
FROM openjdk:24-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy your built JAR file into the container
COPY target/PE-StadiumBookingBE-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (the default for Spring Boot)
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
