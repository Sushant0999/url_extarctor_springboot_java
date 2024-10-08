# # Stage 1: Build the Spring Boot application
# FROM maven:3.8.1-openjdk-17 as builder

# # Set the working directory for the build
# WORKDIR /build

# # Copy the Maven project files
# COPY ../pom.xml .
# COPY ../src ./src

# # Build the project
# RUN mvn clean package

# # Stage 2: Build the React application
# FROM node:18 as react-builder

# # Set the working directory for the React build
# WORKDIR /react-app

# # Copy the React project files
# COPY ../url_exct_frontend/package*.json ./
# RUN npm install

# COPY ../url_exct_frontend/ ./
# RUN npm run build

# # Stage 3: Setup Nginx and the Spring Boot application
# FROM nginx:alpine

# # Update package repository and install required packages
# RUN apk update && \
#     apk add --no-cache openjdk17 supervisor

# # Set the working directory for the application
# WORKDIR /app

# # Copy the build files from React build stage
# COPY --from=react-builder /react-app/build /usr/share/nginx/html

# # Copy the JAR file from the Spring Boot builder stage
# COPY --from=builder /build/target/*.jar app.jar

# # Allow executable permissions to the JAR file
# RUN chmod +x app.jar

# # Copy supervisord configuration file
# COPY supervisord.conf /etc/supervisord.conf

# # Expose the required ports
# EXPOSE 80 8080

# # Start supervisord to manage Java application and Nginx server
# CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]

#Stage 2  Use a base image with JDK and Maven
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy only the necessary files to leverage Docker layer caching
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create a minimal runtime image
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Copy the JAR file from the build stage to the current directory
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]
