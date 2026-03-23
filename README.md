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

### Exception

`PlatformException` — excepcion base con codigo, mensajes, HTTP status, timestamp y detalles.

```kotlin
throw PlatformException(
    code = "ORDER_NOT_FOUND",
    message = "Order 123 not found",
    httpStatus = 404,
)
```

### Context

`FlowContext` — contexto base con trazabilidad (`correlationId`, `userId`, `tenantId`, `metadata`).

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
