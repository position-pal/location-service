FROM eclipse-temurin:21@sha256:d59ca4960a17035592a5c928343ba56862ea6067929da4e776d7a0f4ec26aa44

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
