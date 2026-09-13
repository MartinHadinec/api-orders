# 003 - Multi-tenancy

Shared-table multi-tenancy: all tenants live in the same tables, isolated by `tenant_id`.

## Request flow

1. `TenantFilter` reads the `X-Tenant-ID` header. Missing or invalid (`[a-z0-9-]{1,64}`) means `400`.
2. The value is stored in `TenantContext` (a `ThreadLocal`) and cleared in `finally`.
3. `TenantIdentifierResolver` hands the value to Hibernate.
4. Entities carry `@TenantId` on `tenant_id`, so Hibernate fills it on insert and adds
   `where tenant_id = ?` to every query. Application code never touches `tenant_id`.

## Outside a request

Spring Data validates repository queries at startup and Hibernate asks for the tenant.
The resolver then returns `TenantContext.NONE` (`__none__`), a value no real tenant can
have, so nothing is returned. Fail closed.

## Exempt paths

`/actuator/**`, `/v3/api-docs/**` and `/swagger-ui/**` do not require the header.

## Consequence for the API

An order of another tenant does not exist for the caller. The API answers `404`, not `403`,
so it never confirms that the resource exists.
