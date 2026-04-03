FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY target/booking-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]