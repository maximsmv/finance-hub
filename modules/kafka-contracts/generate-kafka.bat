@echo off
echo [AsyncAPI] Generating Kafka contracts (Java Spring Template v1.6.0)...

REM Ensure node_modules/.bin is used
npx asyncapi generate models java ^
  asyncapi.yaml ^
  --packageName com.advanced.kafka.contracts.model ^
  --output src/main/java

if %errorlevel% neq 0 (
    echo [ERROR] Generation failed!
    exit /b %errorlevel%
)

echo [SUCCESS] Generation completed successfully.
