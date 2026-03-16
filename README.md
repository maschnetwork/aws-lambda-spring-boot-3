# Unicorn Store - Spring Boot 3 on AWS Lambda

A sample application demonstrating how to run a **Spring Boot 3** REST API on **AWS Lambda** with Java 17. It uses the [AWS Serverless Java Container](https://github.com/awslabs/aws-serverless-java-container) library to bridge API Gateway HTTP events to Spring Boot, and [AWS CDK (Java)](https://docs.aws.amazon.com/cdk/v2/guide/home.html) to provision all infrastructure.

> For a guided walkthrough, see the [Java on AWS Lambda Workshop](https://catalog.workshops.aws/java-on-aws-lambda/en-US/01-migration/01-setup-and-deploy/java-container).

---

## Table of Contents

- [Architecture](#architecture)
- [AWS Services Used](#aws-services-used)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Building the Application](#building-the-application)
- [Deploying to AWS](#deploying-to-aws)
- [Testing the API](#testing-the-api)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Load Testing](#load-testing)
- [Cleanup](#cleanup)
- [Key Implementation Details](#key-implementation-details)
- [References](#references)

---

## Architecture

```
Client
  |
  v
API Gateway (REST, proxy mode)
  |
  v
AWS Lambda (Java 17, SnapStart enabled)
  |  - StreamLambdaHandler translates API Gateway events to Spring Boot requests
  |  - Spring Boot 3 handles routing, serialization, and business logic
  v
Amazon EventBridge (custom event bus: "unicorns-spring")
```

1. A client sends an HTTP request to the API Gateway endpoint.
2. API Gateway forwards the request as a Lambda proxy event.
3. The `StreamLambdaHandler` uses the AWS Serverless Java Container to initialize the Spring Boot application context and proxy the request to Spring's `DispatcherServlet`.
4. The `UnicornController` receives the request, delegates to `UnicornService`, which publishes a `UNICORN_CREATED` event to Amazon EventBridge.
5. The response is returned through the same path back to the client.

**SnapStart** is enabled on the Lambda function to dramatically reduce cold-start latency for the Spring Boot application.

## AWS Services Used

| Service | Purpose |
|---|---|
| **AWS Lambda** | Runs the Spring Boot application (Java 17 runtime, 2048 MB memory, 29 s timeout) |
| **Amazon API Gateway** | Provides an HTTPS REST endpoint that proxies all requests to the Lambda function |
| **Amazon EventBridge** | Custom event bus (`unicorns-spring`) that receives unicorn lifecycle events |
| **AWS Lambda SnapStart** | Reduces cold-start time by snapshotting the initialized execution environment |
| **AWS Lambda Web Adapter** | Layer that enables running web frameworks on Lambda |
| **AWS CDK v2** | Infrastructure as Code for provisioning all resources |

## Project Structure

```
.
├── README.md                              # This file
├── deploy.sh                              # One-step deployment script
├── test-app.sh                            # Quick smoke test via curl
├── img/
│   ├── result.png                         # Screenshot of API response
│   └── logs.png                           # Screenshot of CloudWatch logs
├── infrastructure/
│   ├── loadtest.yaml                      # Artillery load-test configuration
│   └── cdk/
│       ├── pom.xml                        # CDK project dependencies (CDK v2.97.0)
│       ├── cdk.json                       # CDK app configuration
│       ├── app/                           # Build artifact directory (JAR copied here)
│       └── src/main/java/com/unicorn/
│           ├── UnicornStoreApp.java       # CDK app entry point
│           └── UnicornStoreSpringStack.java  # Stack: Lambda, API GW, EventBridge
└── unicorn-store-spring/
    ├── pom.xml                            # Spring Boot app dependencies
    └── src/main/java/com/unicorn/store/
        ├── StoreApplication.java          # Spring Boot main class
        ├── StreamLambdaHandler.java        # Lambda request handler (AWS Serverless Java Container)
        ├── controller/
        │   └── UnicornController.java     # REST controller (POST /unicorns)
        ├── service/
        │   └── UnicornService.java        # Business logic
        ├── data/
        │   └── UnicornPublisher.java      # Publishes events to EventBridge
        ├── model/
        │   ├── Unicorn.java               # Unicorn data model
        │   └── UnicornEventType.java      # Event types enum
        └── exceptions/
            ├── PublisherException.java     # EventBridge publishing errors
            └── ResourceNotFoundException.java  # Resource not found errors
```

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **Java JDK** | 17 | Required by both the application and CDK project |
| **Apache Maven** | 3.8+ | Builds the Spring Boot app and CDK project |
| **AWS CDK CLI** | 2.x | [Installation guide](https://docs.aws.amazon.com/cdk/v2/guide/cli.html) |
| **AWS CLI** | 2.x | Configured with credentials and a default region |
| **jq** | any | Used by `test-app.sh` to parse JSON output |
| **curl** | any | Used by `test-app.sh` for HTTP requests |

Make sure your AWS credentials are configured (via `aws configure`, environment variables, or an IAM role) and that your default region is set.

## Building the Application

Build the Spring Boot application into an uber-JAR and copy it to the CDK project's `app/` directory:

```bash
mvn clean package -f unicorn-store-spring/pom.xml
cp unicorn-store-spring/target/spring-boot-lambda.jar infrastructure/cdk/app/
```

The Maven Shade plugin produces `spring-boot-lambda.jar` with all dependencies included, and excludes the embedded Tomcat since API Gateway handles HTTP.

## Deploying to AWS

### First-time setup

If you have never used AWS CDK in your target AWS account and region, bootstrap it first:

```bash
cdk bootstrap
```

### Deploy

Run the deployment script from the project root:

```bash
./deploy.sh
```

This runs `cdk deploy --all` from the `infrastructure/cdk/` directory and writes stack outputs (including the API endpoint URL) to `infrastructure/cdk/target/output.json`.

The deployment creates:
- A Lambda function named `unicorn-store-spring-boot-3` with SnapStart enabled
- An API Gateway REST API named `UnicornStoreSpringApi`
- An EventBridge custom event bus named `unicorns-spring`
- All necessary IAM roles and permissions

## Testing the API

After a successful deployment, run the included test script:

```bash
./test-app.sh
```

This sends a `POST` request to the `/unicorns` endpoint with a sample payload and prints the JSON response.

You can also test manually with curl:

```bash
# Get the API endpoint from the CDK output
API_URL=$(cat infrastructure/cdk/target/output.json | jq -r '.UnicornStoreSpringStack.ApiEndpointSpring')

# Create a unicorn
curl --location --request POST "${API_URL}unicorns" \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "name": "Something",
    "age": "Older",
    "type": "Animal",
    "size": "Very big"
  }' | jq
```

### Result

![API response](img/result.png)

![CloudWatch logs](img/logs.png)

## API Reference

### Create Unicorn

```
POST /unicorns
```

**Request body** (JSON):

```json
{
  "name": "Something",
  "age": "Older",
  "type": "Animal",
  "size": "Very big"
}
```

**Response** (`200 OK`):

```json
{
  "name": "Something",
  "age": "Older",
  "type": "Animal",
  "size": "Very big"
}
```

On success the unicorn is also published as a `UNICORN_CREATED` event to the `unicorns-spring` EventBridge bus.

## Data Model

### Unicorn

| Field | Type | Description |
|---|---|---|
| `name` | String | Name of the unicorn |
| `age` | String | Age description |
| `type` | String | Type or breed |
| `size` | String | Size description |

### Event Types

The application defines the following event types (currently `UNICORN_CREATED` is used):

- `UNICORN_CREATED`
- `UNICORN_UPDATED`
- `UNICORN_DELETED`

Events are published to EventBridge with the source `com.unicorn.store` and the detail type set to the event type name.

## Load Testing

An [Artillery](https://www.artillery.io/) load-test configuration is included at `infrastructure/loadtest.yaml`. It runs 25 requests per second for 60 seconds.

To run it:

```bash
artillery run --target "${API_URL}" -v '{ "url": "unicorns" }' infrastructure/loadtest.yaml
```

Replace `${API_URL}` with your deployed API endpoint (available in `infrastructure/cdk/target/output.json`).

## Cleanup

To remove all deployed resources and avoid ongoing AWS charges:

```bash
cd infrastructure/cdk
cdk destroy --all
```

Confirm the deletion when prompted. This tears down the Lambda function, API Gateway, EventBridge bus, and all associated IAM roles.

## Key Implementation Details

- **AWS Serverless Java Container (`2.0.0-M2`)** - Translates API Gateway proxy requests into Spring Boot `HttpServletRequest`/`HttpServletResponse` objects. The `StreamLambdaHandler` initializes the container with `asyncInit()` for faster startup.
- **SnapStart** - Enabled on the Lambda function (`snapStart: ON_PUBLISHED_VERSIONS`) with a `live` alias to take advantage of pre-initialized snapshots, significantly reducing cold-start latency.
- **AWS CRT HTTP Client** - The EventBridge client uses the AWS Common Runtime (CRT) async HTTP client instead of the default Netty or Apache clients for better Lambda performance.
- **No embedded Tomcat** - The Maven Shade plugin excludes `org.apache.tomcat.embed:*` since API Gateway and the serverless container handle HTTP.
- **Spring Boot 3.1.4** - Uses Jakarta EE (the `javax` to `jakarta` namespace migration), which is why version 2.x of the serverless Java container library is required.

## References

- [AWS Serverless Java Container](https://github.com/awslabs/aws-serverless-java-container) - The library that makes Spring Boot work on Lambda
- [AWS CDK v2 Developer Guide](https://docs.aws.amazon.com/cdk/v2/guide/home.html) - Infrastructure as Code framework used for deployment
- [Java on AWS Lambda Workshop](https://catalog.workshops.aws/java-on-aws-lambda/en-US/01-migration/01-setup-and-deploy/java-container) - Hands-on workshop this project is based on
- [AWS Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html) - Documentation on SnapStart for Java functions
- [Spring Boot 3 Reference](https://docs.spring.io/spring-boot/docs/3.1.4/reference/html/) - Spring Boot framework documentation
