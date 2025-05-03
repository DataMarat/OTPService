# OTP Service

Проект представляет собой backend-сервис на Java (Spring Boot) для генерации, отправки и валидации одноразовых кодов подтверждения (OTP).  
Он построен на слоистой архитектуре и может использоваться как самостоятельное приложение, так и быть частью микросервисной архитектуры, взаимодействуя с другими системами, предоставляя API для использования OTP-кодов.

## Технологии

- Java 17
- Spring Boot 3.4
- Spring Web
- Spring Data JDBC
- Spring Security
- PostgreSQL 17
- Maven
- BCrypt (хеширование паролей)
- JavaMail (для будущей рассылки по Email)

# Структура проекта

```
src/
├── main/
│   ├── java/com/example/otpservice/
│   │   ├── config/                          # Конфигурация Spring Security
│   │   │   └── SecurityConfig.java
│   │   ├── controller/                      # REST-контроллеры
│   │   │   ├── AuthController.java
│   │   │   └── AdminController.java         # Админское API (OTP, пользователи)
│   │   ├── dao/                             # Репозитории (DAO слой)
│   │   │   ├── UserRepository.java
│   │   │   ├── UserRepositoryImpl.java
│   │   │   ├── OtpCodeRepository.java
│   │   │   ├── OtpCodeRepositoryImpl.java
│   │   │   ├── OtpConfigRepository.java
│   │   │   └── OtpConfigRepositoryImpl.java
│   │   ├── dto/                             # DTO с аннотациями валидации
│   │   │   ├── JwtResponse.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegistrationRequest.java
│   │   │   ├── OtpGenerateRequest.java
│   │   │   ├── OtpValidateRequest.java
│   │   │   └── OtpConfigRequest.java        # DTO для изменения конфигурации OTP
│   │   ├── exception/                       # Глобальные обработчики ошибок
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/                           # Модели данных
│   │   │   ├── User.java
│   │   │   ├── OtpCode.java
│   │   │   ├── OtpStatus.java
│   │   │   └── OtpConfig.java               # Конфигурация OTP (модель)
│   │   ├── security/                        # Работа с безопасностью и токенами
│   │   │   ├── JwtFilter.java
│   │   │   └── JwtUtil.java
│   │   └── service/                         # Бизнес-логика
│   │   │   ├── OtpExpirationService.java
│   │   │   ├── OtpService.java
│   │   │   ├── UserService.java
│   │   │   └── OtpConfigService.java        # Работа с конфигурацией OTP
│   │   └── OtpServiceApplication.java       # Точка входа
│   └── resources/
│       ├── application.properties           # Настройки (включая jwt.secret, smtp, smpp и т.д.)
│       ├── application.example.properties   # Шаблон конфигурации
│       └── schema.sql                       # Скрипт создания таблиц (users, otp_codes, otp_config)
├── test/
│   ├── java/com/example/otpservice/
│   │   └── OtpServiceApplicationTests.java  # Тест запуска приложения
```

## Запуск приложения

Точка входа: `OtpServiceApplication.java`

Запуск выполняется как стандартное Spring Boot-приложение:

```bash
mvn spring-boot:run
```

Или после сборки:
```bash
java -jar target/otp-service.jar
```

## Зависимости и сторонние библиотеки

Проект использует систему сборки Maven. Все необходимые зависимости описаны в `pom.xml` и загружаются автоматически при сборке проекта.

### Установка зависимостей

Для загрузки и установки всех внешних библиотек используйте команду:

```bash
mvn clean install
```
Если используется IntelliJ IDEA, зависимости будут автоматически подгружены при первом открытии проекта.
Все зависимости, указанные в pom.xml, управляются через Maven и не требуют ручной установки.

## Работа с базой данных

База данных реализована с помощью PostgreSQL 17, взаимодействие с БД реализовано через JDBC.
В БД 3 таблицы:
- Пользователи (хранит логин пользователя, его пароль в зашифрованном виде, а также его роль).
```agsl
CREATE TABLE users (
	id serial4 NOT NULL,
	username varchar(100) NOT NULL,
	email varchar(100) NOT NULL,
	password_hash text NOT NULL,
	"role" varchar(10) NOT NULL,
	CONSTRAINT users_email_key UNIQUE (email),
	CONSTRAINT users_pkey PRIMARY KEY (id),
	CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'USER'::character varying])::text[]))),
	CONSTRAINT users_username_key UNIQUE (username)
);
```
- Конфигурация OTP-кода (количество записей в ней никогда не превышает 1).
```agsl
CREATE TABLE otp_config (
	id serial4 NOT NULL,
	code_length int4 DEFAULT 6 NOT NULL,
	ttl_seconds int4 DEFAULT 300 NOT NULL,
	CONSTRAINT otp_config_pkey PRIMARY KEY (id)
);
```
- Таблица OTP-кодов (содержит идентификатор операции в привязке к OTP-коду).
OTP-код имеет три статуса:
1. ACTIVE (код активен);
2. EXPIRED (код просрочен);
3. USED (код прошел валидацию и был использован).

```agsl
CREATE TABLE otp_codes (
	id serial4 NOT NULL,
	user_id int8 NOT NULL,
	code varchar(20) NOT NULL,
	operation_id varchar(255) NOT NULL,
	status varchar(10) NOT NULL,
	created_at timestamp NOT NULL,
	expires_at timestamp NOT NULL,
	CONSTRAINT otp_codes_pkey PRIMARY KEY (id),
	CONSTRAINT otp_codes_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'USED'::character varying, 'EXPIRED'::character varying])::text[])))
);
CREATE INDEX idx_otp_user_operation ON public.otp_codes USING btree (user_id, operation_id);
-- otp_codes foreign keys
ALTER TABLE public.otp_codes ADD CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
```

## REST API
Для регистрации и аутентификации пользователей реализовато API, которое позволяет выполнять следующие операции:
- Регистрация нового пользователя (POST /register). У пользователей может быть две роли: либо администратор, либо простой
пользователь. Если администратор уже существует, то регистрация второго администратора является невозможной.
- Логин зарегистрированного пользователя (POST /login). Данная операция возвращает токен с ограниченным сроком действия для осуществления аутентификации и авторизации пользователя.
API пользователя реализует следующие функции:
- Генерация OTP-кода, привязанного к операции либо к ее идентификатору, и рассылка его тремя способами, а также сохранение сгенерированного кода в файл в корне проекта.
- Валидация OTP-кода, который был выслан пользователю по одному из каналов. Пользователи, не являющиеся администраторами, не имеют доступа к API администратора.


## Аутентификация и авторизация
- Все эндпоинты требуют авторизации через JWT. В каждом токене содержится:
  - subject
  - email пользователя
- role: USER или ADMIN (используется для разграничения доступа)
- Роль пользователя (`USER` или `ADMIN`) встраивается в токен и используется для разграничения доступа.
- Эндпоинты /admin/** доступны только пользователям с ролью ADMIN.

## Генерация и валидация OTP-кодов
- Реализован сервис генерации OTP-кодов:
  - Генерация случайного числового кода заданной длины.
  - Срок действия кода настраивается через `otp.code.ttl-seconds`.
  - Коды сохраняются в таблице `otp_codes` с привязкой к пользователю и операции.

- Эндпоинты:
  - **POST /otp/generate**
    - Требует авторизацию через JWT.
    - Принимает `operationId` в теле запроса.
    - Генерирует новый OTP-код и сохраняет его с привязанным временем истечения.
    - Ответ: `201 Created` без тела.

  - **POST /otp/validate**
    - Требует авторизацию через JWT.
    - Принимает `operationId` и `code` в теле запроса.
    - Валидирует OTP-код:
      - Проверяет наличие активного кода для пользователя и операции.
      - Проверяет срок действия кода.
      - В случае успеха помечает код как `USED`.
      - В случае неудачи возвращает ошибку и помечает код как `EXPIRED` (если срок истёк).
    - Ответы:
      - `200 OK` — код подтверждён.
      - `400 Bad Request` — код недействителен или просрочен.

## Отправка OTP-кодов пользователю
Пользователи могут получать защитные коды через различные каналы, что обеспечивает гибкость и удобство в использовании сервиса. 
За данную функциональность отвечает интерфейс OtpDeliveryService, варианты отправки указаны в модели DeliveryChannel. Выбор варианта отправки осуществляется при запуске приложения в настройке otp.delivery.channel, находящейся в application.properties.
Предусмотрены следующие варианты отправки:
- Отправка кода по SMS (класс SmsService). Протестирован на эмуляторе SMPPSim. 
- Отправка кода по Email (класс EmailService). Коды можно отправлять как на эмулятор, так и на реальные почтовые адреса. Это даёт пользователям возможность получать коды на удобный для них почтовый ящик. Проверено на реальных адресах.
- Отправка кода через Telegram (класс TelegramService). С помощью Telegram API создан бот, который отправляет коды пользователям. Это позволяет мгновенно доставлять коды через популярное приложение для обмена сообщениями.
- Сохранение кода в файл (класс FileService). Реализована возможность сохранения сгенерированных кодов в файл в корне проекта (файл otp_codes.txt).
Настройки всех каналов отправки осуществляется в файле application.properties.

## Повторная генерация OTP-кода
Если пользователь повторно запрашивает OTP-код для той же операции до истечения срока действия, то возникнет ошибка. Нельзя сгенерировать новый код по той же самой операции пока не истек старый.

## Настройка канала доставки OTP
Выбор канала осуществляется через параметр в `application.properties`:
```bash
otp.delivery.channel=EMAIL
```
Доступные значения:
- EMAIL
- TELEGRAM
- SMS
- FILE

## Планировщик для устаревших OTP-кодов
В OtpExpirationService реализован механизм, который отмечает просроченные OTP-коды раз в определенный интервал времени (по умолчанию раз в 5 минут) и присваивает им статус
EXPIRED.

## Логирование
- Все действия пользователя и администратора логируются через SLF4J (`LoggerFactory`).
- Логи фиксируют операции генерации, валидации, удаления, конфигурации и доступ к API.
- Пример логов:
```bash
[ADMIN] Requested current OTP configuration 
[ADMIN] Deleting user with id=5 and related OTP codes 
[USER] Generating OTP code for operation ID abc123
```

## Обработка ошибок:
- Используется глобальный `GlobalExceptionHandler`.
- Возвращаются стандартизированные JSON-ответы при ошибках валидации, некорректных данных или внутренних ошибках сервиса.

## Сборка приложения
Приложение использует систему сборки Maven.

## Конфигурирование приложения
Конфигурационный файл `application.properties` исключён из репозитория. Используйте `application.example.properties` как шаблон, скопировав его в application.properties.
1. Создайте свой файл конфигурации:
```bash
cp src/main/resources/application.example.properties src/main/resources/application.properties
```
## Конфигурационные файлы

Основной файл конфигурации — `application.properties`. В нём задаются параметры запуска сервиса, подключения к базе данных, логирования, настройки JWT, параметры OTP и выбор канала доставки.

Также используются отдельные вспомогательные конфигурационные файлы, импортируемые из основного:

- `email.properties` — настройки SMTP-сервера (например, host, port, логин, пароль).
- `sms.properties` — настройки SMPP (например, адрес, порт, системный идентификатор).
- `telegram.properties` — настройки Telegram-бота (токен и chatId).

В проекте предоставлены шаблоны этих файлов с расширением `.example.properties`. При настройке среды необходимо создать файлы без суффикса `.example`, указав в них актуальные значения параметров.

Для подключения вспомогательных файлов используется параметр:

```properties
spring.config.import=classpath:email.properties,classpath:sms.properties,classpath:telegram.properties
````
Файл application.properties должен находиться в каталоге src/main/resources и содержать только актуальные значения, без комментариев и тестовых данных.


## REST API для администратора
### GET /admin/otp-config
Возвращает текущую конфигурацию генерации OTP-кодов: длину кода и TTL (время жизни кода в секундах).
**Требуется авторизация администратора.**
**Пример запроса:**
GET /admin/otp-config Authorization: Bearer <admin_token>
**Пример ответа:**
```json
{
  "codeLength": 6,
  "ttlSeconds": 300
}
```

### PUT /admin/otp-config
Позволяет администратору изменить параметры генерации OTP-кодов:
- `codeLength` — длина генерируемого кода (например, 6–10)
- `ttlSeconds` — время жизни кода в секундах (например, 60–600)
**Требуется авторизация администратора.**
**Пример запроса:**
PUT /admin/otp-config Authorization: Bearer <admin_token> Content-Type: application/json
**Тело запроса:**
```json
{
  "codeLength": 8,
  "ttlSeconds": 180
}
```
Ответ: 200 OK при успешном обновлении.

### GET /admin/users
Возвращает список всех зарегистрированных пользователей с ролью `USER`.
**Требуется авторизация администратора.**
**Пример запроса:**
GET /admin/users Authorization: Bearer <admin_token>
**Пример ответа:**
```json
[
  {
    "id": 2,
    "username": "user1",
    "email": "user1@example.com",
    "phoneNumber": "+79991234567",
    "passwordHash": "$2a$...",
    "role": "USER"
  }
]
```
Примечание: возвращаются пользователи без фильтрации по активности, дате регистрации и т.д.

### DELETE /admin/users/{id}
Удаляет пользователя по указанному идентификатору и все связанные с ним OTP-коды.
**Требуется авторизация администратора.**
**Пример запроса:**
DELETE /admin/users/5 Authorization: Bearer <admin_token>
**Ответ:** `204 No Content` — успешное удаление.

## Бэклог задач
- Повторное удаление несуществующего пользователя не вызывает ошибки или предупреждения.
- Дать возможность пользователю выбора способа доставки OTP-токена.