FROM eclipse-temurin:21@sha256:4e8ddd3ff4c8990c665eb87daac3a34609d224b9e2a7b7707909437dabc2e2f6

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
