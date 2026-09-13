# 007 - OpenAPI documentation

`springdoc-openapi-starter-webmvc-ui` 3.1.1 generates the API description from the controllers.

## URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Both are reachable without the tenant header (see 003).

## Annotations

Controller methods carry `@Operation` and `@ApiResponses` with the status codes they return.
Paging parameters are documented through `@ParameterObject`.

## Global tenant header

`config/OpenApiConfig` registers an `OperationCustomizer` that adds the required
`X-Tenant-ID` header parameter to every operation, with pattern and example, so it can be
filled directly in Swagger UI.
