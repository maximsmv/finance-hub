FROM gradle:8.14-jdk21-jammy

WORKDIR /app
COPY . .

ENTRYPOINT ["gradle", "publish", "--no-daemon"]
