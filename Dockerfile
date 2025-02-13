FROM eclipse-temurin:21@sha256:99c7f17d049d2091e612c56383cc2b65f4bfaf929a7065d2e9c4a624199e5561

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Akka Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
