# Flow Orchestration & DSL

Motor de orquestación reactivo con soporte para Sagas (compensaciones) y ejecución asíncrona/paralela.

### 1. Definir el Contexto
Todo flujo requiere un objeto que extienda de `FlowContext`.

```kotlin
data class OrderContext(
    val orderId: String,
    userId: String? = null
) : FlowContext(userId = userId)
```

### 2. Definición de Pasos (Steps)
Implementa `Step<T>` para tu contexto específico.

```kotlin
class SaveOrderStep : Step<OrderContext> {
    override fun execute(ctx: OrderContext): Uni<OrderContext> = // Lógica...
    override fun onFailure(ctx: OrderContext): Uni<Void> = // Compensación (Saga)
    
    // Cambiar timeout (por defecto 30s)
    override val timeout: Duration = Duration.ofSeconds(10)
}
```

### 3. Timeouts e Implicaciones
Si un paso excede su tiempo de ejecución (`timeout`):
1. El motor lanza una excepción de timeout.
2. Se detiene el flujo principal inmediatamente.
3. **Saga Trigger**: Se ejecutan los `onFailure` de todos los pasos completados previamente en orden inverso para asegurar la consistencia.

### 4. Uso del DSL
```kotlin
flow(context, logger) {
    step(ValidateStep())       // Secuencial
    asyncStep(NotifyStep())    // Fire & Forget (No bloqueante, no dispara Saga si falla)
    
    parallel {                 // Ejecución simultánea (Fork-Join)
        step(AuditStep())
        step(IndexStep())
    }
}.subscribe().with { ctx -> ... }
```
