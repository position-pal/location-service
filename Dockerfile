FROM eclipse-temurin:21@sha256:5aa48efcb1c530c50bf4f4d9728a64702959f2dbbc176c1a2ecbec5442dcaab0

WORKDIR /app

COPY ./entrypoint/build/libs/*-all.jar app.jar

# WS
EXPOSE 8080

# GRPC
EXPOSE 5052

# Management
EXPOSE 8558

ENTRYPOINT ["java", "-jar", "app.jar"]
