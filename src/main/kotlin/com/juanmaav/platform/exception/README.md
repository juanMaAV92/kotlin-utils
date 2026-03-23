# Exception

Jerarquia de excepciones en dos niveles: base (cualquier contexto) y HTTP (APIs).

## Clases

### PlatformException — base, framework-agnostic

```kotlin
// Compose, CLI, cualquier lado
throw PlatformException(
    code = "ORDER_NOT_FOUND",
    message = "Order 123 not found",
)

// Con detalles
throw PlatformException(
    code = "VALIDATION_FAILED",
    messages = listOf("Name is required", "Email is invalid"),
    details = mapOf("field" to "email"),
)
```

### HttpException — para APIs (Ktor, Quarkus)

```kotlin
throw HttpException(
    code = "ORDER_NOT_FOUND",
    message = "Order 123 not found",
    httpStatus = 404,
)
```

## Catch

```kotlin
// Captura ambos tipos
catch (e: PlatformException) {
    logger.error("operation", e.message ?: "Unknown", error = e,
        mapOf("code" to e.code))
}

// Solo HTTP — para mapear a response
catch (e: HttpException) {
    call.respond(HttpStatusCode.fromValue(e.httpStatus), e.toErrorBody())
}
```

## Campos

| Campo | Tipo | PlatformException | HttpException |
|---|---|---|---|
| `code` | String | si | si |
| `messages` | List\<String\> | si | si |
| `timestamp` | Instant | si | si |
| `details` | Map\<String, Any\> | si | si |
| `httpStatus` | Int | no | si (default 500) |
