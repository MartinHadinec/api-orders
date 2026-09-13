package com.example.orders;

import java.util.List;
import java.util.Map;

import com.example.orders.tenant.TenantFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/** Tenant B must never see tenant A data. Real Postgres and Kafka via Testcontainers. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TenantIsolationIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired
    TestRestTemplate rest;

    @Test
    void tenantCannotReadForeignOrder() {
        ResponseEntity<JsonNode> created = exchange("acme", HttpMethod.POST, ORDERS,
                Map.of("customerEmail", "jan@acme.cz", "totalAmount", 499.90,
                        "items", List.of(
                                Map.of("productName", "Keyboard", "price", 299.90),
                                Map.of("productName", "Mouse", "price", 200.00))),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        JsonNode createdOrder = created.getBody();
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.hasNonNull("createdAt")).isTrue();
        String orderId = createdOrder.get("id").asString();

        ResponseEntity<JsonNode> detail = exchange("acme", HttpMethod.GET, ORDERS + "/" + orderId, null, JsonNode.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().get("items")).hasSize(2);
        assertThat(exchange("globex", HttpMethod.GET, ORDERS + "/" + orderId, null, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listReturnsOnlyOwnTenantOrders() {
        exchange("acme", HttpMethod.POST, ORDERS,
                Map.of("customerEmail", "a@acme.cz", "totalAmount", 100), Void.class);
        exchange("globex", HttpMethod.POST, ORDERS,
                Map.of("customerEmail", "b@globex.cz", "totalAmount", 200), Void.class);

        ResponseEntity<JsonNode> list = exchange("acme", HttpMethod.GET, ORDERS, null, JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).isNotNull();
        JsonNode content = list.getBody().get("content");
        assertThat(content).isNotEmpty();
        assertThat(content)
                .allSatisfy(order -> assertThat(order.get("customerEmail").asString()).endsWith("@acme.cz"));
    }

    @Test
    void missingTenantHeaderIsRejected() {
        ResponseEntity<String> response =
                rest.postForEntity(ORDERS, Map.of("customerEmail", "x@y.cz", "totalAmount", 10), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private <T> ResponseEntity<T> exchange(String tenant, HttpMethod method, String url, Object body,
                                           Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TenantFilter.TENANT_HEADER, tenant);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), responseType);
    }
}
