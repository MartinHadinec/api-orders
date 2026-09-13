# api-orders

Multi-tenant Orders REST API built with Spring Boot 4 and Java 25. Orders are isolated per tenant
in PostgreSQL, every created order is published to Kafka through a transactional outbox, and the API
is documented with OpenAPI / Swagger UI.

## Run it (Docker only)

You need **Docker Desktop**. No JDK, no Maven.

```bash
git clone
cd api-orders
docker compose --profile app up -d --build
```

The first run builds the application image (downloads Maven dependencies inside Docker, a few minutes)
and starts four containers. Wait until `docker compose ps` shows `app` as `healthy`, then open:

| What         | URL                                        |
|--------------|--------------------------------------------|
| Swagger UI   | http://localhost:8080/swagger-ui.html      |
| OpenAPI JSON | http://localhost:8080/v3/api-docs          |
| Health       | http://localhost:8080/actuator/health      |
| Kafka UI     | http://localhost:8081                      |

Logs: `docker compose logs -f app`. Stop: `docker compose --profile app down`
(add `-v` to also delete the database and Maven cache volumes).

If port 8080 is taken on your machine: `APP_PORT=9090 docker compose --profile app up -d --build`.

## Try it

Every request needs the header `X-Tenant-ID` (lowercase letters, digits, dashes). Use any value,
tenants are created implicitly, e.g. `acme`.

### In Swagger UI

1. Open `POST /api/v1/orders`, click **Try it out**.
2. Fill `X-Tenant-ID` with `acme` and use this body:

```json
{
  "customerEmail": "jan@acme.cz",
  "totalAmount": 499.90,
  "items": [
    { "productName": "Keyboard", "price": 299.90 },
    { "productName": "Mouse", "price": 200.00 }
  ]
}
```

3. **Execute**. You get `201 Created`, a `Location` header and the order with `id` and `createdAt`.
4. `GET /api/v1/orders/{id}` with `acme` returns the order with items.
   The same call with `X-Tenant-ID: globex` returns `404`: other tenants cannot see it.
5. `GET /api/v1/orders` lists the tenant's orders as a page (`content` + `page`).

### With curl

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' -H 'X-Tenant-ID: acme' \
  -d '{"customerEmail":"jan@acme.cz","totalAmount":499.90,"items":[{"productName":"Keyboard","price":299.90}]}'

curl http://localhost:8080/api/v1/orders -H 'X-Tenant-ID: acme'
```

### See the Kafka event

Open Kafka UI (http://localhost:8081), cluster `local`, **Topics** -> `orders.created.v1` -> **Messages**.
Each created order produced one message: key `acme:<orderId>`, headers `x-tenant-id`, `event-type`,
`event-id`, JSON payload with the order data. The event is written to the `outbox_events` table in the
same database transaction as the order and sent to Kafka by a background publisher within ~0.5 s.

### Error responses

Errors follow RFC 7807 (`application/problem+json`):

- missing or invalid `X-Tenant-ID` -> `400`
- invalid body (bad e-mail, `totalAmount` below 0.01) -> `400` with an `errors` list
- order of another tenant -> `404`

## Development

Requirements: JDK 25 and Docker Desktop. `JAVA_HOME` must point to the JDK.

```bash
docker compose up -d           # Postgres, Kafka, Kafka UI only
./mvnw spring-boot:run         # or run the ApiOrders class from the IDE
./mvnw test                    # integration tests with Testcontainers (real Postgres + Kafka)
```

Running from the IDE or Maven starts the infrastructure automatically through Spring Boot's
Docker Compose support; the containers stay up after the application stops.

Alternative: everything in Docker with the source mounted and remote debug on port 5005:
`docker compose --profile dev up`. Slower than the IDE, but no local JDK needed.

- Application logs: console and one file per day in `logs/` (`logs/2026-09-13.log`).
- Database schema: Liquibase, one migration per file in `src/main/resources/db/changelog/changes/`,
  applied on startup.
- Database access: `localhost:5432`, database `api_orders`, user `admin`, password `tenant123`.

## Project layout

```
com.example.orders
  ApiOrders.java      main class
  controllers/        REST controllers, GlobalExceptionHandler
  entity/             JPA entities and DTOs
  repositories/       Spring Data repositories
  services/           business logic, DTO in / DTO out
  events/             Kafka event producer (outbox) and publisher
  tenant/             tenant header filter and Hibernate tenant resolver
  config/             Kafka, ModelMapper, OpenAPI, outbox, web
```

Detailed design notes per feature: [`docs/com.example.orders/`](docs/com.example.orders/).
Conventions for contributors and AI assistants: [`CLAUDE.md`](CLAUDE.md).

## Stack

Spring Boot 4.1.1, Java 25, Spring Data JPA + Hibernate (`@TenantId`), Liquibase, PostgreSQL 17,
Apache Kafka 4.1 (KRaft), springdoc-openapi 3.1, Lombok, ModelMapper, Testcontainers.
