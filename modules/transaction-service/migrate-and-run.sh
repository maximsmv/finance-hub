#!/bin/bash
echo "Запуск миграций Flyway для DS0..."
flyway -url=$FLYWAY_MIGRATE_DS0_URL \
       -user=$FLYWAY_MIGRATE_DS0_USER \
       -password=$FLYWAY_MIGRATE_DS0_PASSWORD \
       -locations=filesystem:/app/db/migration \
       migrate
if [ $? -eq 0 ]; then
    echo "Миграции DS0 успешно выполнены."
else
    echo "Ошибка при выполнении миграций DS0."
    exit 1
fi

echo "Запуск миграций Flyway для DS1..."
flyway -url=$FLYWAY_MIGRATE_DS1_URL \
       -user=$FLYWAY_MIGRATE_DS1_USER \
       -password=$FLYWAY_MIGRATE_DS1_PASSWORD \
       -locations=filesystem:/app/db/migration \
       migrate
if [ $? -eq 0 ]; then
    echo "Миграции DS1 успешно выполнены, запуск приложения..."
    java -jar /app/app.jar
else
    echo "Ошибка при выполнении миграций DS1."
    exit 1
fi