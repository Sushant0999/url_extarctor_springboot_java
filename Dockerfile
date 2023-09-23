FROM openjdk
VOLUME /tmp
COPY target/*.jar urlExtractor-0.0.1-SNAPSHOT.jar
ENTRYPOINT [ "java", "-jar", "urlExtractor-0.0.1-SNAPSHOT.jar" ]
EXPOSE 8080