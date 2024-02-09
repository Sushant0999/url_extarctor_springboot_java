# STAGE 1: Build Spring Boot application
FROM maven:latest as spring-build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

# STAGE 2: Run Spring Boot application
FROM openjdk:17-jre-slim as spring-runtime

WORKDIR /app

COPY --from=spring-build /app/target/*.jar app.jar

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
