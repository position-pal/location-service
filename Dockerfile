FROM eclipse-temurin:21@sha256:6e59a560f69682a49bf3e4805ff8e1f68269945585c704db6568ddc8b294f36a

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Akka Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
