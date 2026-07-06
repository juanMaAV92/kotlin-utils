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
    override val timeout: Duration = 10.seconds // kotlin.time.Duration
}
```

### 3. Timeouts e implicaciones

Si un paso excede su `timeout`:
1. El motor lanza una excepcion de timeout.
2. Se detiene el flujo inmediatamente.
3. **Saga**: Se ejecutan los `onFailure` de todos los pasos ejecutados en orden inverso.

La compensacion corre bajo `NonCancellable`: se completa entera aunque el timeout o una cancelacion externa hayan interrumpido el flujo.

### 4. Uso del DSL

```kotlin
val result = flow(context, logger) {
    step(ValidateStep())       // Secuencial
    asyncStep(NotifyStep())    // Background (si falla no dispara Saga)

    parallel {                 // Ejecucion simultanea (Fork-Join)
        step(AuditStep())
        step(IndexStep())
    }
}
```

Por defecto los `asyncStep` corren en el scope del propio flow: el flow espera a que terminen y los cancela si falla. Para fire-and-forget real, pasa un scope externo:

```kotlin
flow(context, logger, asyncScope = applicationScope) {
    asyncStep(AuditLogStep()) // sobrevive al flow y no lo bloquea
}
```

### 5. Logging integrado

El `FlowEngine` loggea automaticamente con `StructuredLogger`:
- Inicio de cada step
- Errores con `traceId`, `error_code` y `error_details` (si es `PlatformException`)
- Compensaciones y sus fallos
