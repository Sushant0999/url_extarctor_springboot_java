# Stage 1: Setup Nginx and Node.js
FROM nginx:alpine

# Update package repository and install required packages
RUN apk update && \
    apk add --no-cache openjdk17 supervisor

# Set the working directory for the application
WORKDIR /app

# Copy build files to nginx directory
COPY /url_exct_frontend/build /usr/share/nginx/html

# Copy jar file to app directory
COPY target/*.jar app.jar

# Allow executable permissions to the jar file
RUN chmod +x app.jar

# Copy supervisord configuration file
COPY supervisord.conf /etc/supervisord.conf

# Expose port 80
EXPOSE 80 8080

# Start supervisord to manage Java application and Nginx server
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
