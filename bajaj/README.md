# Webhook Solver (Spring Boot)

This repository contains a Spring Boot application that implements the hiring task:

- On application startup, it sends a POST request to the `generateWebhook` endpoint to obtain a `webhook` URL and `accessToken`.
- Based on the `regNo` last two digits (odd/even), it picks the appropriate SQL solution file from `src/main/resources/questions/`.
- Writes the final SQL to `final-query.sql`.
- Submits the solution back to the webhook URL using the `accessToken` as a JWT (set in the `Authorization` header).

## How to run

Build with Maven:

```bash
mvn clean package
```

Run:

```bash
java -jar target/webhook-solver-1.0.0.jar
```

Or with system properties to override defaults:

```bash
java -Duser.name="Your Name" -Duser.regNo="REG12348" -Duser.email="you@example.com" -Dwebhook.baseUrl="https://bfhldevapigw.healthrx.co.in/hiring" -jar target/webhook-solver-1.0.0.jar
```

Notes:
- The app uses `RestTemplate` to make HTTP calls.
- The app is designed to run **on startup** (no HTTP controllers required).
- The Google Drive links in the assignment are not downloaded automatically; instead, replace the SQL files under `src/main/resources/questions/` with your solved final queries before building.
- The app treats `accessToken` as a Bearer JWT when setting the `Authorization` header. If the API expects the raw token without `Bearer ` prefix, edit `Application.java` accordingly.

## What is included
- `pom.xml`
- `src/main/java/com/example/webhooksolver/Application.java`
- `src/main/resources/questions/question1.sql`
- `src/main/resources/questions/question2.sql`
- `src/main/resources/application.properties`
- `README.md`

## Packaging for GitHub
This project is ready to be pushed to GitHub. After building, upload the generated jar file (`target/webhook-solver-1.0.0.jar`) to the repo releases or keep it in the repo for raw download (as requested by the assignment).

