# Finance Hub

**Finance Hub** — это микросервисное приложение на Spring Boot. Оно включает в себя:

- Аутентификацию через **Keycloak**
- Оркестрацию через **SAGA**
- Асинхронные процессы через **Kafka + Schema Registry (Avro)**
- Наблюдаемость через **Grafana, Alloy, Prometheus, Tempo, Loki, Minio**

## 🧭 Структура проекта

```
finance-hub/
├── demo/                         # Содержит Postman Collection для импорта
├── modules/
│   ├── individuals-api           # API-шлюз, интеграция с Keycloak, оркестрация (SAGA)
│   ├── person-service            # Сервис для управления персональными данными
│   ├── transaction-service       # Работа с транзакциями и кошельками, Kafka, шардирование
│   └── kafka-contracts           # Общие Avro-сообщения и генерация DTO
├── config/                       # Конфигурации шардирования
├── docker-compose.yml            # Основной Docker Compose
├── grafana-datasources.yml       # Источники для Grafana
├── prometheus.yml                # Настройка Prometheus
├── tempo-config.yml              # Настройка Tempo
├── loki-config.yml               # Настройка Loki
├── config.alloy                  # Настройка Alloy
```

## 🚀 Быстрый старт

```bash
docker-compose up --build -d
```

> ⚠️ Убедитесь, что у вас установлен Docker
> ⚠️ Artifactory должен быть доступен, если вы не используете `mavenLocal()` и запускаете сборку через `docker-compose.yml`

## 🌐 Доступ к сервисам

| Сервис                 | URL                     | Доступ           |
|------------------------|-------------------------|------------------|
| individuals-api        | http://localhost:8080   | API-шлюз         |
| person-service         | http://localhost:8082   | Внутренний       |
| transaction-service    | http://localhost:8083   | Внутренний       |
| Kafka UI               | http://localhost:8085   | Интерфейс Kafka  |
| Keycloak               | http://localhost:8081   | admin / admin    |
| Grafana                | http://localhost:3000   | anon (admin)     |
| Prometheus             | http://localhost:9090   | —                |
| Tempo UI               | http://localhost:3200   | —                |

## 🔐 Аутентификация

- Realm `my-realm` импортируется из:  
  `modules/individuals-api/keycloak/import/my-realm-realm.json`

- Новому пользователю автоматически назначается роль `USER`,  
  `user_id` сохраняется как кастомный атрибут и включается в токен

## 🧩 Модули

### `individuals-api`
- Аутентификация через Keycloak (OIDC)
- SAGA-оркестратор для взаимодействия с внутренними сервисами
- Проксирует запросы в `person-service` и `transaction-service` оборачивая их логикой согласно бизнес процессам

### `person-service`
- CRUD для персональных данных
- PostgreSQL `person-service-postgres`

### `transaction-service`
- CRUD для кошельков. Операции init и confirm транзакций.
- Шардирование через ShardingSphere JDBC (2 PostgreSQL: `transaction_0` и `transaction_1`)
- Поддержка Kafka + Avro (схемы через Confluent Schema Registry)

### `kafka-contracts`
- Avro схемы и генерация DTO
- Публикация артефакта в Artifactory

## 📡 Kafka и Schema Registry

- Kafka Bootstrap: `localhost:9092`  
- Schema Registry: `http://localhost:8086`  
- Kafka UI: `http://localhost:8085`

Используется **Confluent Schema Registry** для регистрации и валидации Avro-схем.

## 🔭 Наблюдаемость

| Компонент      | Назначение                                                                |
|----------------|---------------------------------------------------------------------------|
| Grafana Alloy  | Сбор OTLP-трейсов, логов                                                  |
| Prometheus     | Метрики (SCRAP идет напрямую из сервисов, поправлю в будущих обновлениях) |
| Tempo          | Трассировки (S3 совместимое хранилище Minio)                              |
| Loki           | Логи                                                                      |
| Grafana        | Интерфейс визуализации                                                    |

## ⚙️ Переменные окружения

```yaml
ARTIFACTORY_URL:        http://host.docker.internal:8092/artifactory/libs-release-local/
ARTIFACTORY_USER:       person-service
ARTIFACTORY_PASSWORD:   Person_service1
```

## 🧪 Запуск из IDE

- Профиль `dev` для запуска `individuals-api`, `person-service`, `transaction-service` отдельно
- Используйте VM опцию:  
  ```
  -Dspring.profiles.active=dev
  ```
