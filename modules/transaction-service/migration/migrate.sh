#!/bin/bash

set -e

echo "Запуск Flyway миграций"

if [[ -z "$SHARDS_CONFIG" && -z "$SHARDS_CONFIG_PATH" ]]; then
  echo "SHARDS_CONFIG или SHARDS_CONFIG_PATH не заданы"
  exit 1
fi

# Если SHARDS_CONFIG пуст, читаем из файла
if [[ -z "$SHARDS_CONFIG" ]]; then
  echo "Чтение SHARDS_CONFIG из файла: $SHARDS_CONFIG_PATH"

  if [[ -f "$SHARDS_CONFIG_PATH" ]]; then
    SHARDS_CONFIG=$(cat "$SHARDS_CONFIG_PATH")
  elif [[ -f "/app/$SHARDS_CONFIG_PATH" ]]; then
    SHARDS_CONFIG=$(cat "/app/$SHARDS_CONFIG_PATH")
  else
    echo "Файл $SHARDS_CONFIG_PATH не найден"
    exit 1
  fi
fi

shard_keys=$(echo "$SHARDS_CONFIG" | jq -r '.datasources | keys[]')

for shard in $shard_keys; do
  url=$(echo "$SHARDS_CONFIG" | jq -r ".datasources[\"$shard\"].jdbcUrl")
  user=$(echo "$SHARDS_CONFIG" | jq -r ".datasources[\"$shard\"].username")
  password=$(echo "$SHARDS_CONFIG" | jq -r ".datasources[\"$shard\"].password")

  echo "Миграция для [$shard] → $url"

  flyway -url="$url" \
         -user="$user" \
         -password="$password" \
         -locations=filesystem:/app/db/migration \
         migrate || {
           echo "Миграция провалилась для [$shard]"
           exit 1
         }

  echo "[$shard] миграция завершена"
done

echo "Все миграции успешно завершены"
