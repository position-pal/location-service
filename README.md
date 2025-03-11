# Location Service

PositionPal service for location tracking.

## Development

> [!WARNING]
> This repository depends on the [`shared-kernel`](https://github.com/orgs/position-pal/packages?repo_name=shared-kernel) package published on GitHub Packages, which requires authentication to be successfully resolved.
> In CI environments, credentials are automatically inherited from the context of the GitHub Actions workflow.
> However, to correctly build and run the project locally, you need to make sure to have configured your GitHub username and a valid personal access token either in the `gradle.properties` file or as environment variables:
>
> **Credential setup**:
>
> - **`gradle.properties`**:
>   Add your credentials to the `gradle.properties` file located in `GRADLE_USER_HOME` (`~/.gradle` on Unix and `C:\Users\YourUser\.gradle` on Windows) as follows:
>     ```properties
>     gpr.user=<USERNAME>
>     gpr.key=<TOKEN>
>     ```
>   For more information about `gradle.properties` file refer to the [Gradle documentation](https://docs.gradle.org/current/userguide/build_environment.html).
>
> - **Environment variables**:
>   Setup the following environment variables:
>     ```scala
>     export GPR_USER=<USERNAME>
>     export GPR_KEY=<TOKEN>
>     ```
> For more information about how to create a personal access token, refer to the [GitHub documentation](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens).
>
> In case you encounter any problem, please open a new issue in the repository.

## Pre-requisites

In order for the service to function properly, the following environment variables must be set and available at startup.
**However, they are not required for testing.**

| Variable Name             | Description                                                                                                                                                                                                                                    |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MAPBOX_API_KEY`          | [Mapbox access token](https://docs.mapbox.com/help/getting-started/access-tokens/) used to authenticate API requests to the Mapbox services. Ensure the token is stored and used securely, for example, by using Docker or Kubernetes secrets. |
| `AKKA_LICENSE_KEY`        | [Akka license token](https://akka.io/blog/akka-license-keys-and-no-spam-promise) used to running the akka system in production. Ensure that the license is stored securely, for example, by using Dokcer or Kubernetes secrets.                |
| `RABBITMQ_HOST`           | The host address of the RabbitMQ server where the message broker is running (e.g. `localhost`)                                                                                                                                                 |
| `RABBITMQ_VIRTUAL_HOST`   | The [virtual host](https://www.rabbitmq.com/docs/vhosts) in RabbitMQ used for logical separation of resources (e.g. `/`).                                                                                                                      |
| `RABBITMQ_PORT`           | The port on which the RabbitMQ server is listening for incoming connections.                                                                                                                                                                   |
| `RABBITMQ_USERNAME`       | The username used to authenticate with the RabbitMQ server. This should be an account with sufficient permissions to interact with the necessary queues and exchanges in the virtual host.                                                     |
| `RABBITMQ_PASSWORD`       | The password associated with the RABBITMQ_USERNAME. This password is used in conjunction with the username for authentication purposes. Ensure the password is stored and used securely, for example, by using Docker or Kubernetes secrets.   |
| `CASSANDRA_CONTACT_POINT` | The URL (hostname:port) of Cassandra service used by the service                                                                                                                                                                               |
| `CASSANDRA_USERNAME`      | The username used to authenticate with the Cassandra server                                                                                                                                                                                    |
| `CASSANDRA_PASSWORD`      | The password associated with the CASSANDRA_USERNAME. This password is used in conjunction with the username for authentication purposes. Ensure the password is stored and used securely, for example, by using Docker or Kubernetes secrets.  |
| `GRPC_PORT`               | The port on which the gRPC server listens for incoming requests.                                                                                                                                                                               |
| `HTTP_PORT`               | The port on which the HTTP server listens for incoming HTTP requests, including WebSocket connections for real-time communications.                                                                                                            |
| `PRODUCTION`              | A boolean flag indicating whether the service is running in production mode. Default: `true`.                                                                                                                                                  |

An example of valid environment setup is shown below:

```env
MAPBOX_API_KEY=pk.jhyf11wexxdc9lgdlfhfsgyfasmòd885798478jwdhdjioojhe...
RABBITMQ_HOST=localhost
RABBITMQ_VIRTUAL_HOST=/
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=admin
CASSANDRA_CONTACT_POINT=localhost:9042
CASSANDRA_USERNAME=admin
CASSANDRA_PASSWORD=password
GRPC_PORT=50051
HTTP_PORT=8080
```

Moreover, the service requires a Cassandra database that is accessible based on the environment variables specified above. 
Before starting the service, ensure the database is correctly configured with all the required tables. 
The necessary CQL scripts for creating these tables can be found here:
- [`./tracking-actors/src/main/resources/db-scripts/create-tables.cql`](./tracking-actors/src/main/resources/db-scripts/create-tables.cql)
- [`./storage/src/main/resources/db-scripts/create-tables.cql`](./storage/src/main/resources/db-scripts/create-tables.cql)

## Documentation

The scaladoc can be found [here](https://position-pal.github.io/location-service/aggregated-scaladoc).

The Async API documentation can be found [here](https://position-pal.github.io/location-service/asyncapi).

Refer to the [project documentation](https://position-pal.github.io/docs/) for more details on the service implementation.
