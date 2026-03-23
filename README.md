# Kotlin Utils - Mejorado

## Novedades

1. **Contexto Enriquecido**: Todos los flujos heredan de `FlowContext`, garantizando trazabilidad de negocio (`correlationId`, `userId`, `tenantId`).
2. **Tipado Fuerte de Errores**: Uso de `PlatformException` con soporte para timestamps, metadatos y códigos de error estandarizados.
3. **DSL de Orquestación**: Configuración de flujos mucho más legible.

### Ejemplo de Uso del DSL

```kotlin
val logger = Logger.getLogger("MyService")
val myContext = MyBusinessContext(userId = "user-123")

flow(myContext, logger) {
    step(ValidateUserStep())
    step(UpdateBalanceStep())
    step(AuditStep())
}.subscribe().with { result -> 
    println("Flujo completado con ID: ${result.correlationId}")
}
```

4. **Timeouts por Paso**: Cada `Step` tiene ahora un timeout configurable (por defecto 30s) para evitar bloqueos infinitos.
