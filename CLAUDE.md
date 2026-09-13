# api-orders

Multi-tenant Orders REST API. Spring Boot 4.1.1, Java 25, Maven wrapper, PostgreSQL, Liquibase, Kafka.
Detailed notes per feature live in `docs/com.example.orders/` (numbered, chronological). Read the relevant one before changing that area.

## Commands

```bash
./mvnw compile                 # build
./mvnw test                    # all tests, needs Docker (Testcontainers: Postgres 17 + Kafka 4.1.0)
./mvnw spring-boot:run         # run; starts compose.yaml infra automatically
docker compose up -d           # infra only: Postgres 5432, Kafka 9092, Kafka UI 8081
docker compose --profile app up -d --build   # app as a JAR image in Docker (README quick start)
```

Local URLs:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Kafka UI: `http://localhost:8081`


## Structure (`com.example.orders`)

- `ApiOrders` main class
- `controllers/` REST controllers + `GlobalExceptionHandler` (RFC 7807 ProblemDetail)
- `entity/` JPA entities and DTOs side by side (`Order`, `OrderDTO`, `OrderSummaryDTO`, `OutboxEvent`)
- `repositories/`, `services/` (plural names: `OrdersService`, `OrdersRepository`)
- `events/` Kafka: `OrderEventProducer` writes to the transactional outbox, `OutboxPublisher` polls and sends
- `tenant/` `TenantFilter` -> `TenantContext` (ThreadLocal) -> Hibernate `@TenantId`
- `config/` Kafka, ModelMapper, OpenAPI, outbox, web
- `src/main/resources/db/changelog/changes/NNN-*.yaml` one Liquibase migration per file, `includeAll`
- `logs/` daily log files, contents git-ignored via `logs/.gitignore`

## Conventions

- One DTO per aggregate for request and response; server-filled fields are `@JsonProperty(access = READ_ONLY)` + `@Schema(accessMode = READ_ONLY)`.
- Lombok: `@RequiredArgsConstructor`, `@Slf4j`, `@Data` on DTOs, `@Getter`/`@Setter` on entities (never `@Data` on entities).
- Controllers return `ResponseEntity`, carry springdoc `@Operation`/`@ApiResponses`, build `Location` with `ServletUriComponentsBuilder`. Not found = empty `Optional` from the service -> 404. Validation errors = 400.
- API paths are versioned: `/api/v1/...`.
- Schema is owned by Liquibase, Hibernate only validates. New migration = new file `changes/00N-name.yaml` with `rollback`.
- Kafka events go through the outbox (`outbox_events`), never sent directly from a request. Key `tenantId:orderId`, headers `x-tenant-id`, `event-type`, `event-id`. No Debezium/CDC, the in-app poller is the chosen design.
- Tests read HTTP responses as Jackson `JsonNode`, not `OrderDTO` (READ_ONLY fields would be dropped).
- Add a numbered doc in `docs/com.example.orders/` for every new feature.

## Git

- Never commit or push without being asked.
- Commit messages use a bracketed tag prefix, e.g. `[ADD] Skeleton project`.
