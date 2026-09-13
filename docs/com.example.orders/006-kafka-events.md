# 006 - Kafka events

Creating an order publishes `OrderCreatedEvent` to the topic `orders.created.v1`
(3 partitions, created by `KafkaAdmin` on startup, see `config/KafkaConfig`).

## Message

- payload: `OrderCreatedEvent` record (`eventId`, `tenantId`, `orderId`, `customerEmail`,
  `totalAmount`, `occurredAt`), serialized to JSON once when written to the outbox
- key: `tenantId:orderId`, events of one order stay ordered on one partition while a large
  tenant is still spread across partitions
- headers `x-tenant-id`, `event-type`, `event-id`: routing, filtering and deduplication
  without deserializing the payload

## Code

- `events/OrderEventProducer.publishOrderCreated(Order)` builds the event (generates
  `eventId` and `occurredAt`, reads the tenant from `TenantContext`) and stores it in the outbox
- `events/OutboxPublisher` sends pending outbox rows to Kafka on a schedule
- `OrdersService` only calls the producer, it does not know the message shape

## Delivery

Events are not sent inside the request. The producer writes them to the transactional
outbox and `OutboxPublisher` sends them to Kafka, see 008. This removes the dual write
between the database and Kafka.

There is no consumer in this service.
