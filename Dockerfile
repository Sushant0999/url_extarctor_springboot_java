# STAGE 1: Build Spring Boot application
FROM maven:latest as spring-build

WORKDIR /app

COPY pom.xml .

COPY . .

RUN mvn clean package

# STAGE 2: Run Spring Boot application
FROM openjdk:17-jre-slim as spring-runtime

WORKDIR /app

COPY --from=spring-build /app/target/app.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

# STAGE 3: Build React application
FROM node:14.17 as react-build

WORKDIR /app

COPY url_exct_frontend/package*.json ./

RUN npm install

COPY url_exct_frontend/ ./

RUN npm run build

# STAGE 4: Run Nginx server to serve React application
FROM nginx:alpine as nginx-server

COPY --from=react-build /app/build /usr/share/nginx/html

EXPOSE 80

#
## STAGE 1: Build Spring Boot application
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
## Expose the application port
#EXPOSE 8080
#
## Command to run the application
#CMD ["java", "-jar", "app.jar"]
#
## STAGE 2: Build React application
#FROM node:14.17 as react-build
#
#WORKDIR /app
#
#COPY url_exct_frontend/package*.json ./
#RUN npm install
#COPY url_exct_frontend/ ./
#RUN npm run build
#
## STAGE 3: Serve React application with Nginx
#FROM nginx:alpine
#
#COPY --from=react-build /app/build /usr/share/nginx/html
#
#EXPOSE 80
#
#
#
