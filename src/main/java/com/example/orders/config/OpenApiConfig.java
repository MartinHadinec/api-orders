package com.example.orders.config;

import com.example.orders.tenant.TenantFilter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiOrdersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Orders API")
                .version("v1")
                .description("Multi-tenant Orders API. Every request includes a header X-Tenant-ID."));
    }

    @Bean
    public OperationCustomizer tenantHeaderCustomizer() {
        return (operation, _) -> operation.addParametersItem(
                new HeaderParameter()
                .name(TenantFilter.TENANT_HEADER)
                .description("Tenant ID, [a-z0-9-]{1,64}")
                .required(true)
                .schema(new StringSchema().pattern("[a-z0-9-]{1,64}").example("acme")));
    }
}
