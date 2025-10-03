FROM eclipse-temurin:25@sha256:c39999d5331eacc2ca5d0e571cdf841e5c18cdd195762e7b0a05bc6fa7c85a96

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Akka Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
