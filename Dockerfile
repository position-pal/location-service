FROM eclipse-temurin:25@sha256:e787e08ef76f4c16866108cd7f9fcd96a68eef3ac6cc76866897d4d02d5a2262

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Akka Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
