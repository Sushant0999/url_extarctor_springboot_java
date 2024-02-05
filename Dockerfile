# STAGE 1

# Use an OpenJDK runtime as the base image
FROM openjdk:latest

# Set the working directory in the container
WORKDIR /app

# Copy the packaged Spring Boot application JAR file into the container
COPY target/*.jar app.jar

# Expose port 8080 to the outside world
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "app.jar"]


# STAGE 2

# Use an official Node.js runtime as the base image
FROM node:14.17 as build-stage

# Set the working directory in the container
WORKDIR /app

# Copy package.json and package-lock.json to the working directory
COPY url_exct_frontend/package*.json ./

CMD ["pwd"]

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY url_exct_frontend/ ./

# Build the React app for production
RUN npm run build

# Use Nginx as a lightweight server to serve the React app
FROM nginx:alpine

# Copy the built React app from the previous stage to the nginx directory
COPY --from=build-stage /app/build /usr/share/nginx/html

# Expose port 80 to the outside world
EXPOSE 80

# Start Nginx server when the container starts
CMD ["nginx", "-g", "daemon off;"]



