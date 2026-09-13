# 002 - Docker and Kafka infrastructure

Local development stack in `compose.yaml`, application image in `Dockerfile`.

## Services

| Service    | Image                     | Port | Notes                                   |
|------------|---------------------------|------|-----------------------------------------|
| postgres   | postgres:17               | 5432 | db `api_orders`, user `admin`, password `tenant123` |
| kafka      | apache/kafka:4.1.0        | 9092 | KRaft, no ZooKeeper                     |
| kafka-ui   | provectuslabs/kafka-ui    | 8081 | browse topics and messages              |
| app        | Dockerfile `runtime` stage (JAR) | 8080 | only with `--profile app`, healthcheck on readiness |
| app-dev    | Dockerfile `dev` stage    | 8080, 5005 | only with `--profile dev`, source mounted, remote debug |

Kafka has two listeners: `localhost:9092` for the host (IDE) and `kafka:29092` for other containers.

## Usage

```bash
docker compose up -d                       # infra only, app runs from the IDE
docker compose --profile app up -d --build # app as a JAR image, the default way to run it
docker compose --profile dev up            # app with mounted source, remote debug on 5005
APP_PORT=9090 docker compose --profile app up -d   # if 8080 is taken
```

When the app starts from the IDE, `spring-boot-docker-compose` starts the infra automatically
and wires the Postgres connection. Kafka is not supported by that integration, so
`spring.kafka.bootstrap-servers` is set explicitly in `application.yaml`.

## Dockerfile stages

- `dev`: JDK 25 + Maven wrapper, source mounted, JDWP agent for the app JVM only
- `build`: `./mvnw package`, dependencies cached in a BuildKit cache mount
- `runtime`: `eclipse-temurin:25-jre-alpine`, non-root user, `-XX:MaxRAMPercentage=75.0`, used by the `app` service
