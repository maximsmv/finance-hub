DOCKER_COMPOSE = docker compose

.PHONY: help nexus-up up stop down-full restart restart-full first-up stop-all wait-kafka-contracts

## Поднять Nexus и сменить пароль
nexus-up:
	$(DOCKER_COMPOSE) up -d nexus nexus-change-password

## Поднять kafka-contracts и дождаться его завершения
wait-kafka-contracts:
	$(DOCKER_COMPOSE) up kafka-contracts --build

## Поднять все сервисы (кроме nexus), с --build
up: wait-kafka-contracts
	$(DOCKER_COMPOSE) up --build -d --remove-orphans \
		individuals-api \
		person-service \
		transaction-service \
		transaction-service-migration \
		kafka-ui \
		schema-registry \
		kafka \
		zookeeper \
		keycloak \
		keycloak-postgres \
		person-service-postgres \
		transaction-service-postgres-0 \
		transaction-service-postgres-1 \
		alloy \
		loki \
		prometheus \
		tempo \
		minio \
		create-tempo-bucket \
		grafana

## Остановить все сервисы (кроме nexus), без удаления
stop:
	$(DOCKER_COMPOSE) stop \
		individuals-api \
		person-service \
		transaction-service \
		transaction-service-migration \
		kafka-contracts \
		kafka-ui \
		schema-registry \
		kafka \
		zookeeper \
		keycloak \
		keycloak-postgres \
		person-service-postgres \
		transaction-service-postgres-0 \
		transaction-service-postgres-1 \
		alloy \
		loki \
		prometheus \
		tempo \
		minio \
		create-tempo-bucket \
		grafana

stop-all:
	$(DOCKER_COMPOSE) stop

## Остановить и удалить все контейнеры
down-full:
	$(DOCKER_COMPOSE) down --remove-orphans --volumes

## Мягкий перезапуск (без удаления томов и orphan-контейнеров)
restart:
	$(DOCKER_COMPOSE) stop
	$(DOCKER_COMPOSE) up --build -d

## Полный перезапуск (c удалением всего окружения)
restart-full: down-full first-up

## Полный первый запуск: nexus + все сервисы
first-up: nexus-up up

help:
	@echo Available commands:
	@echo   make first-up        - initial startup: nexus + all other services
	@echo   make up              - build and start all services (including kafka-contracts automatically)
	@echo   make stop            - stop all services except nexus (keep data and volumes)
	@echo   make stop-all        - stop all services (keep data and volumes)
	@echo   make down-full       - stop and remove all containers, volumes, and orphan containers
	@echo   make restart         - soft restart with --build (without removing data or volumes)
	@echo   make restart-full    - full restart including volume and orphan removal
	@echo   make nexus-up        - start only nexus and reset admin password
