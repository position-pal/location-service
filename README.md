# Location Service

PositionPal service for location tracking.

## Pre-requisites

For the correct operation of the service, it is necessary the following environment variables are set and available at startup: 

| Variable Name             | Description                                                                                                                                                                                                                                    |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MAPBOX_API_KEY`          | [Mapbox access token](https://docs.mapbox.com/help/getting-started/access-tokens/) used to authenticate API requests to the Mapbox services. Ensure the token is stored and used securely, for example, by using Docker or Kubernetes secrets. |
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

Moreover, the service requires a Cassandra database instance running and accessible at port `9042`. Before starting the service, make sure the database is properly configured with all the necessary tables.
The scripts to create the tables can be found in the following CQL scripts:
- [`./tracking-actors/src/main/resources/db-scripts/create-tables.cql`](./tracking-actors/src/main/resources/db-scripts/create-tables.cql)
- [`./storage/src/main/resources/db-scripts/create-tables.cql`](./storage/src/main/resources/db-scripts/create-tables.cql)

## Documentation

The scaladoc can be found [here](https://position-pal.github.io/location-service/).

Refer to the [project documentation](https://position-pal.github.io/docs/design/location-service/) for more details on the service implementation.
