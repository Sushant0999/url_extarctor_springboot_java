# STAGE 1: Build Spring Boot application
FROM maven:3.8.4-openjdk-17 AS spring-build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# STAGE 2: Create a minimal runtime image for Spring Boot
FROM openjdk:17-jdk-alpine AS spring-runtime

WORKDIR /app

COPY --from=spring-build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

# STAGE 3: Build React application
FROM node:14.17 as react-build

WORKDIR /app/url_exct_frontend

COPY url_exct_frontend/package*.json ./
RUN npm install

COPY url_exct_frontend/ ./
RUN npm run build

# STAGE 4: Run Nginx server to serve React application
FROM nginx:alpine as nginx-server

COPY --from=react-build /app/url_exct_frontend/build /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
