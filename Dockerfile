##Stage 2  Use a base image with JDK and Maven
#FROM maven:3.8.4-openjdk-17 AS build
#
## Set the working directory in the container
#WORKDIR /app
#
## Copy only the necessary files to leverage Docker layer caching
#COPY pom.xml .
#COPY src ./src
#
## Build the application
#RUN mvn clean package -DskipTests
#
## Create a minimal runtime image
#FROM openjdk:17-jdk-alpine
#WORKDIR /app
#
## Copy the JAR file from the build stage to the current directory
#COPY --from=build /app/target/*.jar app.jar
#
#
## Run Nginx server to serve React application
#FROM nginx:alpine as nginx-server
#
## Update the package repository and install required packages
#RUN apk update
#RUN apk add --no-cache maven
#RUN apk add --no-cache openjdk17
#RUN apk add --no-cache nginx
#RUN apk add --no-cache git
#RUN apk add --no-cache nodejs
#RUN apk add --no-cache curl
#
#RUN curl -fsSL https://nodejs.org/dist/v14.17.5/node-v14.17.5-linux-x64.tar.xz | tar -xJf - -C /usr/local --strip-components=1
#RUN node -v
#RUN npm -v
#
## Set the working directory in the container
##WORKDIR /app
#
## Cloning repo
#RUN git clone https://github.com/Sushant0999/url_extarctor_springboot_java.git
#
#
## Copy only the necessary files to leverage Docker layer caching
#COPY pom.xml .
#COPY src ./src
#
## Build the application
#RUN mvn clean package -DskipTests
#
## Copy the JAR file from the build stage to the current directory
#COPY /app/target/*.jar /app/app.jar
#
## Expose the application port
#EXPOSE 8080
#
## Command to run the application
#CMD ["java", "-jar", "app.jar"]
#
## Changing Directory
#RUN cd /app/url_extarctor_springboot_java/url_exct_frontend
#
## Installing Node Modules
#RUN npm i
#
## Creating Build
#RUN npm run build
#
## Copy Build folder from frontend to nginx
#COPY /app/url_extarctor_springboot_java/url_exct_frontend/build/* /usr/share/nginx/html
#
## Exposing Port
#EXPOSE 80
#
## Nginx Command
#CMD ["nginx", "-g", "daemon off;"]


# Stage 1: Setup Nginx and Node.js
FROM nginx:alpine

# Update package repository and install required packages
RUN apk update && \
    apk add --no-cache maven openjdk17

# Set the working directory for the application
WORKDIR /app

# Copy only the necessary files to leverage Docker layer caching
COPY pom.xml .
COPY src ./src

# Copy build files to nginx directory
COPY /url_exct_frontend/build /usr/share/nginx/html

# Build the Java Spring Boot application
RUN mvn clean package -DskipTests

# Copy jar file to app directory
COPY target/*.jar app.jar

RUN chmod +X app.jar

# Expose ports
EXPOSE 80

# Start both the Java application and Nginx server
CMD ["sh", "-c", "java -jar app.jar & nginx -g 'daemon off;'"]

