# Flow Orchestration & DSL

Motor de orquestacion con soporte para Sagas (compensaciones) y ejecucion asincrona/paralela. Usa `StructuredLogger` y `PlatformException` de la propia lib.

### 1. Definir el Contexto

```kotlin
data class OrderContext(
    val orderId: String,
    userId: String? = null,
) : FlowContext(userId = userId)
```

### 2. Definir Steps

```kotlin
class SaveOrderStep : Step<OrderContext> {
    override suspend fun execute(ctx: OrderContext): OrderContext {
        // logica...
        return ctx
    }

    override suspend fun onFailure(ctx: OrderContext) {
        // compensacion (Saga)
    }

    // Cambiar timeout (por defecto 30s)
    override val timeout: Duration = Duration.ofSeconds(10)
}
```

### 3. Timeouts e implicaciones

Si un paso excede su `timeout`:
1. El motor lanza una excepcion de timeout.
2. Se detiene el flujo inmediatamente.
3. **Saga**: Se ejecutan los `onFailure` de todos los pasos ejecutados en orden inverso.

### 4. Uso del DSL

```kotlin
val result = flow(context, logger) {
    step(ValidateStep())       // Secuencial
    asyncStep(NotifyStep())    // Fire & Forget (no dispara Saga si falla)

    parallel {                 // Ejecucion simultanea (Fork-Join)
        step(AuditStep())
        step(IndexStep())
    }
}
```

### 5. Logging integrado

El `FlowEngine` loggea automaticamente con `StructuredLogger`:
- Inicio y fin de cada step
- Errores con `traceId`, `error_code` y `error_details` (si es `PlatformException`)
- Compensaciones y sus fallos
