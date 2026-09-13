# 005 - Orders REST API

Base path `/api/v1/orders`, every request needs the `X-Tenant-ID` header.

| Method | Path        | Response                                    |
|--------|-------------|---------------------------------------------|
| POST   | `/`         | `201` + `Location` + created `OrderDTO`     |
| GET    | `/`         | `200` paged `OrderSummaryDTO`: `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`, query params `page`, `size`, `sort` |
| GET    | `/{id}`     | `200` `OrderDTO` with items, `404` if not found for this tenant |

Validation failure and a missing tenant header return `400` with an RFC 7807 `ProblemDetail`.

## Layers

- `controllers/OrdersController`: HTTP only, returns `ResponseEntity`, builds `Location`
  with `ServletUriComponentsBuilder`
- `services/OrdersService`: accepts and returns DTOs, maps with ModelMapper
  (`STRICT`, `skipNull`), the entity never leaves the service
- `controllers/GlobalExceptionHandler`: exception to `ProblemDetail` mapping

## DTOs

One `OrderDTO` is used for request and response. Server-filled fields (`id`, `createdAt`) are
`@JsonProperty(access = READ_ONLY)`, so clients cannot send them and OpenAPI marks them read-only.
Validation lives on the DTO fields. `OrderItemDTO` follows the same pattern.

## Example

```bash
curl -X POST localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' -H 'X-Tenant-ID: acme' \
  -d '{"customerEmail":"jan@acme.cz","totalAmount":499.90,
       "items":[{"productName":"Keyboard","price":299.90}]}'
```
