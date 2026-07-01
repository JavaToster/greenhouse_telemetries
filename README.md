# greenhouse-telemetries

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Liquibase](https://img.shields.io/badge/Liquibase-migrations-cc6600)
![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED)

Сервис приёма и выдачи телеметрии для системы **[Greenhouse](../../)**.

Принимает показания датчиков (температура, влажность воздуха, влажность почвы, освещённость) от IoT-устройств и предоставляет владельцам кластеров и администраторам историю телеметрии в разрезе каждого устройства. Авторизацию по владению кластером делегирует `greenhouse-inventory`.

> Это один из сервисов super-репозитория **Greenhouse**, подключённый как git submodule. Общая архитектура, docker-compose и инструкция по запуску всей системы — в [README супер-репозитория](../../README.md).

---

## Содержание

- [Роль в системе](#роль-в-системе)
- [Технологии](#технологии)
- [Доменная модель](#доменная-модель)
- [API](#api)
- [Авторизация по владению кластером](#авторизация-по-владению-кластером)
- [Запрос top-N телеметрии](#запрос-top-n-телеметрии)
- [Безопасность](#безопасность)
- [Обработка ошибок](#обработка-ошибок)
- [Миграции БД](#миграции-бд)
- [Переменные окружения](#переменные-окружения)
- [Запуск](#запуск)
- [Структура проекта](#структура-проекта)

---

## Роль в системе

```
   IoT-устройство (JWT role=DEVICE)        OWNER / ADMIN (JWT role=USER)
           │                                         │
           │ POST /api/telemetries                   │ GET /api/telemetries/clusters/{id}?limit=N
           ▼                                         ▼
  ┌────────────────────────────────────────────────────────┐
  │                  greenhouse-telemetries                 │
  └────────────────────────┬───────────────────────────────┘
                            │
                            │ Feign (форвард JWT пользователя)
                            │ GET /api/devices/my-clusters/{clusterId}
                            ▼
                  greenhouse-inventory
              (отдаёт устройства кластера
               + проверяет, что вызывающий — владелец)
                            │
                            ▼
                       PostgreSQL
                   (таблица telemetries,
                    оконная функция ROW_NUMBER)
```

Сервис не хранит сведения о кластерах и владельцах. Список устройств и проверка владения — исключительно через `greenhouse-inventory`. Благодаря этому telemetries не дублирует бизнес-логику авторизации и всегда консистентен с актуальным состоянием кластера.

> Сервис **не использует Redis** — временные структуры и challenge-response полностью в ответственности `greenhouse-inventory`.

## Технологии

- **Java 21**, **Spring Boot 4.0.6**
- Spring Web MVC, Spring Security (`@PreAuthorize`, кастомный `JwtFilter`)
- Spring Data JPA, **PostgreSQL**
- **Liquibase** — миграции схемы
- **java-jwt** (Auth0) — верификация JWT (HMAC256)
- **Spring Cloud OpenFeign** — вызовы `greenhouse-inventory`
- **ModelMapper**, Lombok, Jakarta Validation
- Docker / multi-stage build: `maven:3.9.5-eclipse-temurin-21` → `eclipse-temurin:21-jre-jammy`

## Доменная модель

### `Telemetry`

| Поле | Тип | Описание |
|---|---|---|
| `id` | `BIGSERIAL` | Автоинкремент |
| `deviceId` | `UUID` | Устройство-источник — берётся из JWT (`DevicePrincipal`), не из тела запроса |
| `temperature` | `double` | °C, диапазон −50..80 |
| `airHumidity` | `double` | %, диапазон 0..100 |
| `soilHumidity` | `double` | %, диапазон 0..100 |
| `illumination` | `double` | Лк, ≥ 0 |
| `createdAt` | `timestamptz` | Устанавливается сервером в момент записи |

Составной индекс `idx_telemetry_device_created (device_id, created_at DESC)` — оптимизирован под основной паттерн чтения: последние N записей по конкретным устройствам.

## API

### `POST /api/telemetries`

| Доступ | Описание |
|---|---|
| `DEVICE` | Устройство отправляет одно показание датчиков |

`deviceId` извлекается из JWT устройства (`DevicePrincipal`) — устройство физически не может записать показания от имени другого устройства.

```bash
curl -X POST http://localhost:8082/api/telemetries \
  -H "Authorization: Bearer <device-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "temperature": 24.5,
    "airHumidity": 60.0,
    "soilHumidity": 45.0,
    "illumination": 12000.0
  }'
```

Все четыре поля обязательны. При нарушении диапазонов — `400 Bad Request` с перечислением ошибок по каждому полю.

### `GET /api/telemetries/clusters/{clusterId}`

| Доступ | Описание |
|---|---|
| `OWNER`, `ADMIN` | История телеметрии по всем устройствам кластера |

**Параметры:**

| Параметр | Тип | По умолчанию | Диапазон | Описание |
|---|---|---|---|---|
| `limit` | `int` | `50` | `1–200` | Количество последних записей **на каждое устройство** |

```bash
curl -H "Authorization: Bearer <owner-jwt>" \
  "http://localhost:8082/api/telemetries/clusters/a1b2c3d4-...?limit=20"
```

**Ответ:**

```json
{
  "clusterId": "a1b2c3d4-...",
  "devices": [
    {
      "deviceId": "device-uuid-1",
      "telemetries": [
        {
          "temperature": 24.5,
          "airHumidity": 60.0,
          "soilHumidity": 45.0,
          "illumination": 12000.0,
          "createdAt": "2026-06-30T10:00:00Z"
        }
      ]
    },
    {
      "deviceId": "device-uuid-2",
      "telemetries": []
    }
  ]
}
```

Устройства без телеметрии попадают в ответ с пустым массивом `telemetries`. Если у кластера нет устройств — `devices: []` без ошибки.

`limit` ограничен `@Min(1)` / `@Max(200)` на уровне контроллера (`@Validated`). Нарушение — `400 Bad Request` через `ConstraintViolationException`.

## Авторизация по владению кластером

`TelemetryService.findByCluster` первым делом вызывает `deviceClient.getDevicesByCluster(clusterId)` — Feign-запрос к `greenhouse-inventory`. `FeignJwtInterceptor` форвардит JWT вызывающего пользователя в этот запрос.

`greenhouse-inventory` проверяет, является ли пользователь из JWT владельцем кластера. Если нет — возвращает `403`. `InventoryClientErrorDecoder` транслирует `FeignException(403)` в `AccessDeniedException`, которое `GlobalExceptionHandler` отдаёт клиенту как `403 Forbidden`.

Таким образом:
- `OWNER` видит телеметрию только своих кластеров;
- `ADMIN` — любых (`greenhouse-inventory` пропускает `ADMIN` без проверки владения);
- `DEVICE`-токен заблокирован ещё на уровне `@PreAuthorize` (`principal instanceof UserPrincipal`).

## Запрос top-N телеметрии

`TelemetryRepository` использует нативный SQL с оконной функцией:

```sql
SELECT * FROM (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY created_at DESC) AS rn
  FROM telemetries
  WHERE device_id IN (:deviceIds)
) ranked
WHERE rn <= :limit
```

Один запрос к БД возвращает последние `limit` записей **для каждого устройства кластера одновременно** — вне зависимости от числа устройств. Индекс `(device_id, created_at DESC)` обеспечивает эффективное выполнение оконной функции.

Результат группируется в Java по `deviceId` и маппится на структуру ответа, где каждое устройство имеет собственный список телеметрии.

## Безопасность

`SecurityConfiguration`:
- публичных эндпоинтов нет — `anyRequest().authenticated()`;
- stateless-сессии, CSRF отключён;
- `@PreAuthorize` на `getClusterTelemetries` проверяет `principal instanceof UserPrincipal` — `DevicePrincipal` не может обратиться к этому эндпоинту вне зависимости от статуса аутентификации.

`JwtAuthenticationProvider` различает `USER`- и `DEVICE`-токены по claim'у `token_type`. Оба типа верифицируются по общему `SECURITY_JWT`-секрету — сетевой запрос к auth-сервису не нужен.

Feign-клиент к inventory (`device-service`) логирует только метод и статус ответа (`Logger.Level.BASIC`).

## Обработка ошибок

`GlobalExceptionHandler` — единый формат `{ "statusCode": ..., "message": ... }`:

| Исключение | HTTP статус |
|---|---|
| `MethodArgumentNotValidException` (`@Valid` на body) | 400 Bad Request |
| `ConstraintViolationException` (`@Validated` на `@RequestParam limit`) | 400 Bad Request |
| `HttpMessageNotReadableException` | 400 Bad Request |
| `BadRequestException` | 400 Bad Request |
| `DataIntegrityViolationException` | 409 Conflict |
| `BadCredentialsException` | 401 Unauthorized |
| `AccessDeniedException` (включая транслированный 403 от inventory) | 403 Forbidden |
| `EntityNotFoundException` | 404 Not Found |
| `FeignException` (прочие ошибки межсервисного вызова) | 500 Internal Server Error |
| `Exception` (fallback) | 500 Internal Server Error |

## Миграции БД

```
db/changelog/
└── migrations/
    └── 001-telemetries-init.yaml
        ├── таблица telemetries (все поля + createdAt DEFAULT now())
        └── индекс idx_telemetry_device_created (device_id, created_at DESC)
```

## Переменные окружения

| Переменная | Назначение |
|---|---|
| `TELEMETRIES_SERVER_PORT` | Порт сервиса (пример: `8082`) |
| `TELEMETRIES_SPRING_DATASOURCE_URL` | JDBC URL базы `greenhouse_telemetries` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Креды Postgres |
| `SECURITY_JWT` | Общий JWT-секрет — **одинаковый во всех сервисах системы** |
| `CLIENTS_INVENTORY` | Базовый URL `greenhouse-inventory` для Feign (`DeviceClient`) |

> Сервис не требует Redis и не имеет дополнительных секретов для шифрования — конфигурация намеренно минимальна.

## Запуск

### В составе всей системы (рекомендуется)

```bash
cd Greenhouse
docker compose up -d --build
```

### Локально, для разработки

Нужны локальный Postgres (база `greenhouse_telemetries`) и заполненные переменные окружения. `greenhouse-inventory` должен быть доступен по `CLIENTS_INVENTORY` — иначе `GET /api/telemetries/clusters/{id}` будет падать с ошибкой Feign.

```bash
./mvnw clean package -DskipTests
java -jar target/greenhouse_telemetries-0.0.1-SNAPSHOT.jar
```

### Docker-образ отдельно

```bash
docker build -t greenhouse-telemetries .
docker run --rm -p 8082:8082 --env-file .env greenhouse-telemetries
```

## Структура проекта

```
src/main/java/com/example/greenhouse_telemetries/
├── controllers/
│   └── TelemetryController.java           # POST /api/telemetries, GET /clusters/{id}
├── services/
│   └── TelemetryService.java               # бизнес-логика: запись, top-N группировка
├── store/
│   └── TelemetryStore.java                  # инкапсуляция доступа к репозиторию
├── repositories/postgres/
│   └── TelemetryRepository.java             # нативный SQL с ROW_NUMBER()
├── models/
│   └── Telemetry.java                        # JPA-entity
├── DTO/
│   ├── telemetry/
│   │   ├── AddTelemetryDTO.java              # входящие данные от устройства (с валидацией)
│   │   ├── TelemetryDTO.java                  # одна запись в ответе
│   │   └── DeviceTelemetryDTO.java             # телеметрия одного устройства
│   ├── cluster/
│   │   └── ClusterTelemetryDTO.java            # итоговый ответ по кластеру
│   ├── device/
│   │   └── DeviceDTO.java                       # DTO ответа inventory (Feign)
│   └── error/
│       └── ErrorResponseDTO.java
├── security/
│   ├── jwt/                                     # JwtUtil, JwtFilter, JwtAuthenticationProvider
│   ├── principals/                                # UserPrincipal, DevicePrincipal
│   └── FeignJwtInterceptor.java                   # форвард JWT пользователя в вызов к inventory
├── clients/
│   └── DeviceClient.java                          # Feign → greenhouse-inventory
├── configurations/
│   ├── security/SecurityConfiguration.java
│   ├── feign/
│   │   ├── FeignConfiguration.java
│   │   └── InventoryClientErrorDecoder.java        # FeignException(403) → AccessDeniedException
│   └── general/BeanConfiguration.java              # ModelMapper, Feign Logger.Level.BASIC
├── exceptions/
│   └── GlobalExceptionHandler.java
└── util/
    ├── Convertor.java                              # Entity → TelemetryDTO
    └── enums/                                       # Role, TokenType, DeviceStatus
```
