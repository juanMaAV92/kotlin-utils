# Retry

Retry con exponential backoff para operaciones que pueden fallar de forma transitoria. Una `suspend fun` — no pierde contexto de coroutines ni telemetria.

## Uso basico

```kotlin
// Defaults: 3 intentos, 100ms inicial, factor 2x
val invoice = retry {
    dianClient.sendInvoice(data)
}
```

## Configuracion

```kotlin
val ticket = retry(
    maxAttempts = 5,
    initialDelay = 2.seconds,
    factor = 3.0,
    maxDelay = 30.seconds,
    logger = logger,
) {
    cloudBackend.renewLicense(deviceId)
}
```

## Parametros

| Parametro | Default | Descripcion |
|---|---|---|
| `maxAttempts` | 3 | Numero maximo de intentos |
| `initialDelay` | 100ms | Espera antes del primer reintento |
| `factor` | 2.0 | Multiplicador de backoff (100ms → 200ms → 400ms) |
| `maxDelay` | 10s | Tope maximo de espera entre reintentos |
| `logger` | null | Si se pasa, loggea cada reintento con `StructuredLogger` |
| `retryIf` | `isTransient` | Lambda que decide si el error es retriable |

## Que reintenta por defecto

Solo errores transitorios:

| Error | Reintenta |
|---|---|
| `SocketTimeoutException` | si |
| `ConnectException` | si |
| `IOException` | si |
| `HttpException` 408, 429, 500, 502, 503, 504 | si |
| `HttpException` 400, 401, 403, 404 | no |
| `PlatformException` | no |
| Cualquier otro | no |

## Override del criterio

```kotlin
// Solo reintentar rate limiting
retry(retryIf = { it is HttpException && it.httpStatus == 429 }) {
    externalApi.call()
}

// Reintentar errores de DB
retry(retryIf = { it is SQLException }) {
    database.query(sql)
}
```

## Log output

Si pasas un `logger`, cada reintento genera:

```json
{"level":"warn","step":"retry","message":"Attempt 1/3 failed, retrying in 100ms","attributes":{"attempt":1,"max_attempts":3,"delay_ms":100,"error_type":"SocketTimeoutException","error_message":"Connect timed out"}}
```
