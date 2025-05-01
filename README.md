# OTP Service

Проект представляет собой backend-сервис на Java (Spring Boot) для генерации, отправки и валидации одноразовых кодов подтверждения (OTP).  
Он может использоваться как самостоятельное приложение, так и быть частью микросервисной архитектуры, взаимодействуя с другими системами, например, с банковским сервисом на Go.

## Технологии

- Java 17
- Spring Boot 3.4
- Spring Web
- Spring Data JDBC
- Spring Security
- PostgreSQL
- Maven
- BCrypt (хеширование паролей)
- JavaMail (для будущей рассылки по Email)

## Структура проекта
src/
├── main/
│   ├── java/com/example/otpservice/
│   │   ├── config/                          # Конфигурация Spring Security
│   │   │   └── SecurityConfig.java
│   │   ├── controller/                      # REST-контроллеры (AuthController)
│   │   │   └── AuthController.java
│   │   ├── dao/                             # Репозитории (DAO слой)
│   │   │   ├── UserRepository.java
│   │   │   └── UserRepositoryImpl.java
│   │   ├── dto/                             # DTO с аннотациями валидации
│   │   │   ├── JwtResponse.java             # DTO для ответа с токеном
│   │   │   ├── LoginRequest.java             # DTO для логина
│   │   │   └── RegistrationRequest.java
│   │   ├── exception/                       # Глобальные обработчики ошибок
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/                           # Модели данных
│   │   │   └── User.java
│   │   ├── security/                        # Работа с безопасностью и токенами
│   │   │   ├── JwtFilter.java               # Фильтрация запросов по JWT
│   │   │   └── JwtUtil.java                 # Генерация токенов
│   │   └── service/                         # Бизнес-логика
│   │   │   └── OtpExpirationService.java
│   │   │   └── OtpService.java
│   │   │   └── UserService.java
│   │   └── OtpServiceApplication.java       # Точка входа
│   └── resources/
│       ├── application.properties           # Настройки (включая jwt.secret, jwt.expiration)
│       └── schema.sql                        # Скрипт создания таблиц
├── test/
│   ├── java/com/example/otpservice/
│   │   └── OtpServiceApplicationTests.java   # Тест запуска приложения

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
- Регистрация нового пользователя. У пользователей может быть две роли: либо администратор, либо простой
пользователь. Если администратор уже существует, то регистрация второго администратора является невозможной.
Эндпоинт /register
- Логин зарегистрированного пользователя. Данная операция возвращает токен с ограниченным сроком действия для осуществления аутентификации и авторизации пользователя.
Эндпоинт /login
API пользователя реализует следующие функции:
- Генерация OTP-кода, привязанного к операции либо к ее идентификатору, и рассылка его тремя способами, а также сохранение сгенерированного кода в файл в корне проекта.
- Валидация OTP-кода, который был выслан пользователю по одному из каналов. Пользователи, не являющиеся администраторами, не имеют доступа к API администратора.

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
- Отправка кода по Email (класс EmailService). Коды можно отправлять как на эмулятор, так и на реальные почтовые адреса. Это обеспечит пользователям возможность
получать коды на удобный для них почтовый ящик. Проверено на реальных адресах.
- Отправка кода через Telegram (класс TelegramService). С помощью Telegram API создан бот, который отправляет коды пользователям. Это позволяет мгновенно доставлять коды через популярное приложение для обмена сообщениями.
- Сохранение кода в файл (класс FileService). Реализована возможность сохранения сгенерированных кодов в файл в корне проекта (файл otp_codes.txt).
Настройки всех каналов отправки осуществляется в файле application.properties.


## Планировщик для устаревших OTP-кодов
В OtpExpirationService реализован механизм, который отмечает просроченные OTP-коды раз в определенный интервал времени (по умолчанию раз в 5 минут) и присваивает им статус
EXPIRED.

## Логирование
- Все операции генерации, валидации и истечения OTP-кодов логируются через SLF4J (`LoggerFactory`).
- Логи содержат информацию о пользователях, операциях и статусах кодов.

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













## Что уже реализовано

- Инициализирован Maven-проект
- Настроена структура пакетов
- Создана сущность `User` с ролями `USER` и `ADMIN`
- Реализована таблица `users` в базе данных через `schema.sql`
- Подключена PostgreSQL и настроен `application.properties`
- Создан DAO-слой `UserRepository` с поддержкой:
    - поиска по username, email, роли
    - добавления пользователя
    - проверки существования администратора
- Реализован `UserService` с возможностью:
    - регистрации пользователя
    - проверок уникальности имени и почты
    - ограничения на единственного администратора
    - хеширования пароля с помощью BCrypt
- Реализован REST-эндпоинт POST /register:
  - Создание пользователя с ролями USER/ADMIN
  - Проверка уникальности username/email
  - Ограничение: может быть только один администратор
  - Валидация входящих данных через @Valid и DTO
  - Возврат структурированных JSON-ответов с кодами 201, 400, 409



## Настройка окружения



## План работ

1. Создание REST-контроллера `/register` и DTO с валидацией
2. Реализация логина и генерации JWT
3. Настройка фильтрации запросов по ролям и авторизации
4. Создание конфигурации OTP-кода и таблицы `otp_config`
5. Генерация OTP-кодов (бизнес-логика и модель)
6. Реализация рассылки кода: Email, SMS (эмулятор SMPP), Telegram, файл
7. Проверка и валидация OTP-кодов
8. Планировщик истечения срока действия OTP-кодов
9. Логирование событий в приложении
10. Тесты и документация по API

## Примечания


