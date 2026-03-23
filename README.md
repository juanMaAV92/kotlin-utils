# kotlin-utils

Libreria de utilidades para proyectos Kotlin. **Framework-agnostic** — funciona con Ktor, Quarkus, Spring, Compose, o cualquier proyecto Kotlin/JVM.

## Principios

- **Cero acoplamiento a frameworks**: solo depende de `kotlinx-coroutines` y `slf4j`
- **Coroutines nativas**: toda la API es `suspend fun`, sin wrappers reactivos (Mutiny, Reactor, RxJava)
- **Minimalista**: solo lo necesario, sin over-engineering

## Dependencias

| Dependencia | Proposito |
|---|---|
| `kotlinx-coroutines-core` | Concurrencia estructurada |
| `slf4j-api` | Logging (el consumidor elige la implementacion) |

## Modulos

### Flow Engine — Orquestacion con Saga

Motor de orquestacion de pasos secuenciales con compensacion automatica (patron Saga). Soporta ejecucion secuencial, paralela y asincrona con timeouts por paso.

```kotlin
// 1. Define tu contexto
class OrderContext(
    val orderId: String,
    var paymentId: String? = null,
    userId: String,
) : FlowContext(userId = userId)

// 2. Define tus steps
class ValidateStockStep : Step<OrderContext> {
    override suspend fun execute(context: OrderContext): OrderContext {
        // validar stock...
        return context
    }

    override suspend fun onFailure(context: OrderContext) {
        // compensar: liberar reserva
    }
}

// 3. Ejecuta con el DSL
val result = flow(OrderContext("order-1", userId = "user-1"), logger) {
    step(ValidateStockStep())
    step(ProcessPaymentStep())
    parallel {
        step(SendEmailStep())
        step(UpdateAnalyticsStep())
    }
    asyncStep(AuditLogStep()) // fire-and-forget
}
```

**Caracteristicas:**
- Timeouts configurables por step (default: 30s)
- Compensacion automatica en orden inverso (Saga)
- `ParallelStep`: ejecucion concurrente con `async`/`awaitAll`
- `AsyncStep`: fire-and-forget para side effects no criticos
- DSL declarativo para composicion de flujos
- Logging integrado con `StructuredLogger` (traceId, error_code, error_details si es `PlatformException`)

### Logger — Structured JSON Logging

JSON plano directo a stdout. Sin JSON anidado. Compatible con Grafana/Loki/CloudWatch.

```kotlin
// Sin tracing (Compose, CLI)
val logger = JsonStructuredLogger(serviceName = "pos-desktop")

// Con OpenTelemetry (Ktor, Quarkus)
val logger = JsonStructuredLogger(serviceName = "pos-server", traceProvider = { ... })

// Uso
logger.info("process_payment", "Payment processed", mapOf("amount" to 50000))
```

Output:
```json
{"time":"2026-03-22T10:15:30Z","level":"info","service":"pos-server","step":"process_payment","message":"Payment processed","trace_id":"abc","span_id":"def","attributes":{"amount":50000}}
```

### Exception — Jerarquia de errores y responses estandarizados

La lib centraliza el formato de errores. El handler lo escribes una vez por framework.

```
PlatformException (base)  →  ErrorResponse
└── HttpException (APIs)  →  HttpErrorResponse
    ├── ForbiddenException (403)
    └── UnauthorizedException (401)
```

```kotlin
// Lanzar errores
throw PlatformException(code = "ORDER_NOT_FOUND", message = "Order 123 not found")
throw HttpException(code = "ORDER_NOT_FOUND", message = "Order 123 not found", httpStatus = 404)
throw ForbiddenException()
throw UnauthorizedException("Token expired")

// Convertir a response estandarizado
val error = exception.toErrorResponse()          // ErrorResponse
val error = httpException.toHttpErrorResponse()   // HttpErrorResponse (incluye httpStatus)
```

Handler en Ktor (una vez):
```kotlin
install(StatusPages) {
    exception<HttpException> { call, e ->
        call.respond(HttpStatusCode.fromValue(e.httpStatus), e.toHttpErrorResponse())
    }
}
```

### Context

`FlowContext` — contexto base con trazabilidad (`traceId`, `userId`, `tenantId`, `metadata`).

## Compatibilidad

| Framework | Uso |
|---|---|
| **Ktor** | Directo — Ktor es coroutines-native |
| **Quarkus** | `uni { flowEngine.run(ctx, steps) }` via `mutiny-kotlin` |
| **Spring WebFlux** | `mono { flowEngine.run(ctx, steps) }` via `kotlinx-coroutines-reactor` |
| **Compose** | `coroutineScope { flowEngine.run(ctx, steps) }` |
| **CLI / Scripts** | `runBlocking { flowEngine.run(ctx, steps) }` |

## Setup

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.juanmaav:kotlin-utils:0.1.0")
}
```
