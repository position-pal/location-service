FROM eclipse-temurin:21@sha256:0b31d935d2db37bc4428aafddd8af48d8ec75e8f61d2f7546c037d5e4d967fc9

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Akka Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
