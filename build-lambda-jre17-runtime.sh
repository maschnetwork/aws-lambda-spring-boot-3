#!/bin/sh

# Remove a previously created custom runtime
file="spring-custom-runtime.zip"
if [ -f "$file" ] ; then
    rm "$file"
fi

# Build the custom Java runtime from the Dockerfile
docker build -f Dockerfile --progress=plain -t lambda-custom-runtime-minimal-jre-17-x86 .

# Extract the runtime.zip from the Docker environment and store it locally
docker run --rm --entrypoint cat lambda-custom-runtime-minimal-jre-17-x86 spring-custom-runtime.zip > spring-custom-runtime.zip
