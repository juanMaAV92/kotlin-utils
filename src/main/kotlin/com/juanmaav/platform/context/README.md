# Platform Context

Base para contextos de ejecución con trazabilidad integrada.

### Definición
Extender de `FlowContext` para incluir datos de negocio. Los campos `traceId`, `userId` y `tenantId` ya están disponibles.

```kotlin
class UserActionContext(
    val action: String,
    userId: String? = null
) : FlowContext(userId = userId)
```

### Metadatos
Usa `addMetadata(key, value)` para persistir datos temporales entre componentes sin cambiar el esquema del objeto. Útil para tokens temporales, flags de control o resultados intermedios que no forman parte del modelo de datos principal.

El mapa es thread-safe (`ConcurrentHashMap`): los steps de un bloque `parallel` pueden mutarlo concurrentemente. El `Map` que pases al constructor se copia.

**Ejemplo de uso:**
```kotlin
// Guardar metadatos en un Step
ctx.addMetadata("EXTERNAL_API_TOKEN", token)

// Recuperar metadatos en un Step posterior
val token = ctx.metadata["EXTERNAL_API_TOKEN"] as? String
```
