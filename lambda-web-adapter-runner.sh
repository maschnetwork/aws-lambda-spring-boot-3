#!/bin/sh

$LAMBDA_TASK_ROOT/jre17-slim/bin/java \
    --add-opens java.base/java.util=ALL-UNNAMED \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseSerialGC \
    -jar spring-uber.jar "com.unicorn.store.StoreApplication"
