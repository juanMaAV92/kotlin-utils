# Exception

Jerarquia de excepciones, responses estandarizados y excepciones tipadas comunes.

## Jerarquia

```
PlatformException (base — Compose, CLI, cualquier lado)
└── HttpException (APIs — agrega httpStatus)
    ├── ForbiddenException (403)
    └── UnauthorizedException (401)
```

## Excepciones

```kotlin
// Base — sin HTTP
throw PlatformException(code = "ORDER_NOT_FOUND", message = "Order 123 not found")

// HTTP — con status
throw HttpException(code = "ORDER_NOT_FOUND", message = "Order 123 not found", httpStatus = 404)

// Tipadas — atajos comunes
throw ForbiddenException()  // 403, code = "FORBIDDEN"
throw UnauthorizedException()  // 401, code = "UNAUTHORIZED"
throw ForbiddenException("Cannot delete admin users")  // mensaje custom
```

## Error Responses

Formato estandarizado para respuestas de error. La lib da el formato, el handler lo escribes una vez por framework.

```kotlin
// ErrorResponse — base (Compose, CLI)
val error = exception.toErrorResponse()
// { "code": "ORDER_NOT_FOUND", "messages": ["..."], "timestamp": "...", "details": {} }

// HttpErrorResponse — APIs (incluye httpStatus)
val error = httpException.toHttpErrorResponse()
// { "code": "ORDER_NOT_FOUND", "messages": ["..."], "timestamp": "...", "httpStatus": 404, "details": {} }
```

## Handler por framework (una vez por proyecto)

### Ktor
```kotlin
install(StatusPages) {
    exception<HttpException> { call, e ->
        call.respond(HttpStatusCode.fromValue(e.httpStatus), e.toHttpErrorResponse())
    }
    exception<PlatformException> { call, e ->
        call.respond(HttpStatusCode.InternalServerError, e.toErrorResponse())
    }
}
```

### Quarkus
```kotlin
@Provider
class ExceptionHandler : ExceptionMapper<HttpException> {
    override fun toResponse(e: HttpException): Response =
        Response.status(e.httpStatus).entity(e.toHttpErrorResponse()).build()
}
```

### Compose
```kotlin
catch (e: PlatformException) {
    val error = e.toErrorResponse()
    showErrorDialog(title = error.code, messages = error.messages)
}
```
