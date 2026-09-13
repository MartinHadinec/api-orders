# 001 - Project skeleton

Spring Boot 4.1.1 application on Java 25, built with the Maven wrapper (`./mvnw`).

## Coordinates

- groupId `com.example`, artifactId `api-orders`, base package `com.example.orders`
- main class `ApiOrders`

## Starters

- `spring-boot-starter-webmvc`, `spring-boot-starter-validation`
- `spring-boot-starter-data-jpa` with PostgreSQL driver
- `spring-boot-starter-liquibase`
- `spring-boot-starter-actuator`
- matching `*-test` starters for tests

## Commands

```bash
./mvnw compile        # build
./mvnw test           # tests (needs Docker for Testcontainers)
./mvnw spring-boot:run
```

`JAVA_HOME` must point to a JDK 25.

## Logs

- IDE / `spring-boot:run`: console and one file per day in `logs/`, e.g. `logs/2026-09-13.log`
  (14 days kept, 500 MB cap). The folder is in git, its contents are ignored via `logs/.gitignore`.
  Appenders live in `src/main/resources/logback-spring.xml`.
- tests: `target/surefire-reports/*-output.txt`
- containers: profile `container`, stdout only, `docker compose logs -f app`
