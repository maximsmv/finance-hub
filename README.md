# Finance Hub

**Finance Hub** — это микросервисное приложение, построенное на Spring Boot, с Keycloak для аутентификации, телеметрией через OpenTelemetry и мониторингом через Grafana/Loki/Prometheus/Tempo.

## Структура проекта

```
finance-hub/
├── modules/
│   ├── individuals-api           # API-шлюз, интеграция с Keycloak, оркестрация (SAGA)
│   └── person-service            # Сервис для управления персональными данными
├── docker-compose.yml            # Основной Docker Compose файл
├── prometheus.yml                # Конфигурация Prometheus
├── tempo-config.yml              # Конфигурация Tempo
├── grafana-datasources.yml       # Источники данных Grafana
├── otel-collector-config.yml     # Конфигурация OpenTelemetry Collector
```

## Быстрый старт

### 1. Запуск всей системы

Убедитесь, что установлен **Docker** и **Docker Compose**.

Также необходимо наличие **Artifactory** или другого артефакт-хранилища. Использование `mavenLocal()` не подойдет при сборке контейнера, поскольку контекст сборки находится вне локального Maven-кэша.

```bash
    docker-compose up --build
```

### 2. Доступ к сервисам

| Сервис           | URL                    | Доступ               |
|------------------|------------------------|----------------------|
| individuals-api  | http://localhost:8080  | —                    |
| person-service   | http://localhost:8082  | —                    |
| Keycloak         | http://localhost:8081  | admin / admin        |
| Grafana          | http://localhost:3000  | Анонимный вход       |
| Prometheus       | http://localhost:9090  | —                    |
| Tempo UI         | http://localhost:3200  | —                    |


## Аутентификация
- Реалм `my-realm` автоматически импортируется из:  
  `modules/individuals-api/keycloak/import/my-realm-realm.json`

- При регистрации пользователю автоматически назначается роль `USER`.

- После регистрации сервис `person-service` сохраняет пользователя и возвращает `user_id`.  
  Этот идентификатор сохраняется как кастомный атрибут `user_id` в Keycloak (доступен в токене).

## Модули

### `individuals-api`

- Интеграция с Keycloak
- Обработка регистрации, логина, авторизации
- SAGA-оркестратор для создания пользователя
- Проксирование запросов к `person-service`

### `person-service`

- Работа с PostgreSQL
- CRUD-операции над персональными данными

## Мониторинг и трассировка

В систему встроены средства наблюдаемости через OpenTelemetry:

| Компонент      | Назначение                                        |
|----------------|---------------------------------------------------|
| OTEL Collector | Централизованный приём метрик, логов и трейсингов |
| Prometheus     | Сбор и агрегация метрик                           |
| Tempo          | Хранение трассировок                              |
| Loki           | Хранение логов                                    |
| Grafana        | Отображение логов, метрик и трассировок           |

## Переменные окружения

В `docker-compose.yml` определены переменные окружения для доступа к Artifactory:

```yaml
ARTIFACTORY_URL:        http://host.docker.internal:8092/artifactory/libs-release-local/
ARTIFACTORY_USER:       person-service
ARTIFACTORY_PASSWORD:   Person_service1
```

При необходимости их можно изменить под своё окружение.

## Запуск сервисов из IDE

Для локальной разработки каждый сервис можно запускать отдельно через IDE.

- В каждом модуле (`individuals-api`, `person-service`) добавлен профиль `dev`.
- В режиме `dev` можно использовать `mavenLocal()` и не запускать Artifactory.
- Профиль `dev` активируется через VM options

```
-Dspring.profiles.active=dev
```

## Примечания

- Все ключевые настройки (Keycloak realm, мапперы, атрибуты) уже включены в `my-realm-realm.json`
- После экспорта Realm можно заменить файл `my-realm-realm.json` для сохранения конфигурации