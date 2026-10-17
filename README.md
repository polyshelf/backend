# backend

# Инструкция: временный запуск с токенами без auth service

1. Создаем юзера в бд, запоминаем его id

2. Идем на [jwt.io](https://www.jwt.io/), выбираем JWT Encoder, задаем следующие параметры:

    Header:
    ```json
    {
      "alg": "RS256",
      "typ": "JWT"
    }
    ```

    Payload (sub заменить на id вашего юзера):
    ```json
    {
      "sub": "f46d2bd8-4703-41bd-afc9-898c731d1af1",
      "iss": "polyshelf-auth",
      "aud": ["polyshelf-api"],
      "iat": 1792252800,
      "exp": 1812339200,
      "role": "USER"
    }
    ```

    Sign JWT: вставляем целиком содержимое файла private.pem (лежит в телеге)

3. Полученный токен используем в запросах, добавив перед ним `Bearer `

    Пример:
    ```
    POST http://localhost:8081/products/from-barcode
    Authorization: Bearer eyJhbGciO...
    Content-Type: application/json

    {
      "code": "0123401234012",
      "expiration_date": "2026-12-17"
    }
    ```
