# Spring Boot on AWS Lambda example

The following repository contains an example of a Spring Boot 3 application that is running on AWS Lambda.
Since Spring Boot 3 is based on Java 17, we have to use an [AWS Lambda custom runtime](https://aws.amazon.com/blogs/compute/build-a-custom-java-runtime-for-aws-lambda/). For translation between
API Gateway Events and plain HTTP requests the example project uses the [AWS Lambda Web adapter](https://github.com/awslabs/aws-lambda-web-adapter).

## Prerequisites

- Docker
- Java 17
- AWS CDK CLI

## How to build the application

Run the following script to execute the maven build, create a minimal Java 17 JRE via jlink and package it with starting instructions.
```
./build.sh
```

To deploy the application via CDK use:

```
./deploy.sh
```

After successful deployment you can use the following script to test the application:


```
./test-app.sh
```