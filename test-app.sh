#bin/sh

curl --location --request POST $(cat infrastructure/cdk/target/output.json | jq -r '.UnicornStoreSpringStack.ApiEndpointSpring')'/unicorns' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "name": "Something",
    "age": "Older",
    "type": "Animal",
    "size": "Very big"
}' | jq




