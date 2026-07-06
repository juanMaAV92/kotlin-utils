# kotlin-utils

[![CI](https://github.com/juanMaAV92/kotlin-utils/actions/workflows/ci.yml/badge.svg)](https://github.com/juanMaAV92/kotlin-utils/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/juanMaAV92/kotlin-utils?color=6ee7a8&label=release)](https://github.com/juanMaAV92/kotlin-utils/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![JVM](https://img.shields.io/badge/JVM-21-orange)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-juanmaav92.github.io-6ee7a8)](https://juanmaav92.github.io/kotlin-utils)

Libreria de utilidades para proyectos Kotlin. **Framework-agnostic** — funciona con Ktor, Quarkus, Spring, Compose, o cualquier proyecto Kotlin/JVM.

> Documentacion interactiva: **https://juanmaav92.github.io/kotlin-utils**
>
> Ejemplo real: [kotlin-quarkus-blueprint](https://github.com/juanMaAV92/kotlin-quarkus-blueprint) — template de microservicio Quarkus con arquitectura hexagonal que integra Flow Engine (saga), logger con OpenTelemetry, retry, validation y exception mappers.

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
    asyncStep(AuditLogStep()) // en background — nunca falla el flujo
}
```

**Caracteristicas:**
- Timeouts configurables por step (default: 30s)
- Compensacion automatica en orden inverso (Saga) — corre bajo `NonCancellable`, asi que se completa aunque cancelen la coroutine
- `ParallelStep`: ejecucion concurrente con `async`/`awaitAll`
- `AsyncStep`: side effects no criticos en background — los fallos se loguean y nunca fallan el flujo; pasa `asyncScope` a `flow()` para fire-and-forget real
- DSL declarativo para composicion de flujos
- Logging integrado con `StructuredLogger` (traceId, error_code, error_details si es `PlatformException`)

### Logger — Structured JSON Logging

JSON plano, una linea por evento — los atributos quedan en la raiz, sin anidar. Se emite via `slf4j`, asi que funciona con cualquier backend (Logback, etc.). Compatible con Grafana/Loki/CloudWatch.

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
{"time":"2026-03-22T10:15:30.123456Z","level":"INFO","service":"pos-server","step":"process_payment","message":"Payment processed","trace_id":"abc","span_id":"def","amount":50000}
```

`error()` con throwable agrega `error_type`, `error_message` y el `stack_trace` completo.

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

// Envolver otra excepcion conserva la cadena original
throw PlatformException(code = "DB_ERROR", message = "Insert failed", cause = e)

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

### Retry — Exponential Backoff

Retry con backoff exponencial para errores transitorios. Por defecto reintenta errores de conexion, `IOException` y HTTP 408/429/500/502/503/504 — nunca 400, 401 o 403. El delay se limita a 10s (`maxDelay`). La cancelacion de coroutines nunca se reintenta.

```kotlin
// Defaults: 3 intentos, 100ms inicial, factor 2x, max delay 10s
val invoice = retry { dianClient.sendInvoice(data) }

// Customizado
val ticket = retry(maxAttempts = 5, initialDelay = 2.seconds, logger = logger) {
    cloudBackend.renewLicense(deviceId)
}

// Override del criterio
retry(retryIf = { it is HttpException && it.httpStatus == 429 }) {
    externalApi.call()
}
```

### Validation — DSL con acumulacion de errores

Checks declarativos que se acumulan en vez de fallar al primero: cada `check` fallido se recolecta y se lanza todo junto en una sola `PlatformException` (`VALIDATION_FAILED`), lista para el handler del modulo Exception.

```kotlin
validate(request) {
    check(value.orderId.isNotBlank()) { "orderId must not be blank" }
    check(value.amount > 0) { "amount must be positive" }
}

// Si algo fallo:
// PlatformException(code = "VALIDATION_FAILED", messages = [todos los mensajes])
```

### Context

`FlowContext` — contexto base con trazabilidad (`traceId`, `userId`, `tenantId`, `metadata` thread-safe). Viaja por todos los steps del flujo — los steps paralelos pueden mutarlo sin riesgo — y el engine incluye el `traceId` en sus propias lineas de log.

```kotlin
class OrderContext(
    val orderId: String,
    userId: String,
    tenantId: String? = null,
) : FlowContext(
    userId = userId,
    tenantId = tenantId,
    metadata = mapOf("order_id" to orderId), // se copia a un mapa thread-safe
)
```

## Compatibilidad

| Framework | Uso |
|---|---|
| **Ktor** | Directo — Ktor es coroutines-native |
| **Quarkus** | `uni { flowEngine.run(ctx, steps) }` via `mutiny-kotlin` |
| **Spring WebFlux** | `mono { flowEngine.run(ctx, steps) }` via `kotlinx-coroutines-reactor` |
| **Compose** | `coroutineScope { flowEngine.run(ctx, steps) }` |
| **CLI / Scripts** | `runBlocking { flowEngine.run(ctx, steps) }` |

## Setup

La libreria se publica en **GitHub Packages**:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/juanMaAV92/kotlin-utils")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN") // token con read:packages
        }
    }
}

dependencies {
    implementation("com.juanmaav:kotlin-utils:0.1.0")

    // la lib trae slf4j-api en runtime — elige un backend
    implementation("ch.qos.logback:logback-classic:1.5.6")
}
```

> GitHub Packages requiere un token con scope `read:packages` incluso para paquetes publicos. Guia paso a paso en la [documentacion interactiva](https://juanmaav92.github.io/kotlin-utils#install).
