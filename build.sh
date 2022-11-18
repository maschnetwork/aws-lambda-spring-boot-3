#!/bin/sh

# Remove a previously created custom runtime
file="spring-custom-runtime.zip"
if [ -f "$file" ] ; then
    rm "$file"
fi

# Build the function locally to get the JAR
mvn clean package -f unicorn-store-spring/pom.xml

# Build a custom Java 17 runtime with jlink from a Dockerfile
docker build -f JRE17LambdaBuilder.dockerfile --progress=plain -t lambda-custom-runtime-minimal-jre-17-x86 .

# Extract the runtime.zip from the Docker environment and store it locally
docker run --rm --entrypoint cat lambda-custom-runtime-minimal-jre-17-x86 spring-custom-runtime.zip > spring-custom-runtime.zip
