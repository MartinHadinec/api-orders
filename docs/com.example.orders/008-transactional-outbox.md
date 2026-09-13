# 008 - Transactional outbox

Replaces the dual write from 006. Events are no longer sent to Kafka inside the request;
they are stored in the database together with the order and published afterwards.

## Flow

1. `OrdersService.create` saves the order and calls `OrderEventProducer.publishOrderCreated`.
2. The producer builds `OrderCreatedEvent`, serializes it to JSON and saves an `OutboxEvent`
   row in the same transaction. Either both the order and the row commit, or neither.
3. `OutboxPublisher` runs every `outbox.publish-interval` (500 ms). It locks up to
   `outbox.batch-size` pending rows with `FOR UPDATE SKIP LOCKED`, sends each to Kafka,
   waits for the acknowledgement and sets `published_at`.
4. If a send fails, the batch stops and the remaining rows are retried on the next run.
   This keeps per-key ordering.

## Table `outbox_events` (migration 004)

`id` (= event id), `tenant_id`, `aggregate_type`, `aggregate_id`, `event_type`, `topic`,
`message_key`, `payload` (jsonb), `created_at`, `published_at`.
Partial index on `created_at where published_at is null`, so the publisher scans only pending rows.

The entity has no `@TenantId`. The publisher runs outside a request and must see all tenants.
The tenant is a plain column and goes into the Kafka header.

## Kafka message

- value: the JSON payload as stored, sent with `StringSerializer`
- key: `tenantId:orderId`
- headers: `x-tenant-id`, `event-type` (`OrderCreated`), `event-id`

## Guarantees

At-least-once. A crash between the Kafka acknowledgement and the `published_at` update
sends the event again. Consumers deduplicate by `event-id`.

`SKIP LOCKED` allows several application instances to publish in parallel.

## Configuration

```yaml
outbox:
  publish-interval: 500ms
  batch-size: 100
  send-timeout: 10s
```