# Logger

Structured JSON logging directo a stdout. Sin JSON anidado — cada log es una linea JSON plana que Grafana/Loki/CloudWatch parsean directo.

## Setup

```kotlin
// Sin tracing (Compose, CLI)
val logger = JsonStructuredLogger(serviceName = "pos-desktop")

// Con OpenTelemetry (Ktor, Quarkus)
val logger = JsonStructuredLogger(
    serviceName = "pos-server",
    traceProvider = {
        val span = Span.current()
        if (span.spanContext.isValid) TraceInfo(span.spanContext.traceId, span.spanContext.spanId)
        else null
    }
)
```

## Uso

```kotlin
logger.info("process_payment", "Payment processed", mapOf("amount" to 50000, "method" to "cash"))

logger.error("validate_stock", "Insufficient stock", error = ex, mapOf("productId" to "SKU-123"))
```

## Output

Una linea, un JSON. Sin wrapping de frameworks.

```json
{"time":"2026-03-22T10:15:30Z","level":"info","service":"pos-server","step":"process_payment","message":"Payment processed","trace_id":"abc123","span_id":"def456","attributes":{"amount":50000,"method":"cash"}}
```

## Campos

| Campo | Siempre | Descripcion |
|---|---|---|
| `time` | si | ISO-8601 UTC |
| `level` | si | fatal, error, warn, info, debug |
| `service` | si | Nombre del servicio/app |
| `step` | si | Operacion que se ejecuta |
| `message` | si | Texto legible |
| `trace_id` | si hay TraceProvider | ID de traza OpenTelemetry |
| `span_id` | si hay TraceProvider | ID de span OpenTelemetry |
| `attributes` | si hay | Datos extras del contexto |

## Consideraciones

- **No usar con quarkus-logging-json**: desactiva el JSON formatter de Quarkus (`quarkus.log.console.json=false`), si no obtienes JSON dentro de JSON.
- **SLF4J**: se usa solo para control de niveles y thread-safety. El JSON lo construye la lib, no el framework.
- **Sin Jackson**: serializa con StringBuilder manual. Cero dependencias extra.
- **TraceProvider es opcional**: en Compose/CLI no pasas nada. En backends, conectas OpenTelemetry con un lambda.
