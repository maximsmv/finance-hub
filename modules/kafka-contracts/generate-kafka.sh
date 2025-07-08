#!/bin/bash

echo "[AsyncAPI] Generating Kafka contracts (Java Spring Template v1.6.0)..."

npx asyncapi generate models java \
  asyncapi.yaml \
  --packageName com.advanced.kafka.contracts.model \
  --output src/main/java

if [ $? -ne 0 ]; then
  echo "[ERROR] Generation failed!"
  exit 1
fi

echo "[SUCCESS] Generation completed successfully."