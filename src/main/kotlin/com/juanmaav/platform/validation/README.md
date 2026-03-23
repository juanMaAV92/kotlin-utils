# Validation DSL

DSL minimalista para validacion de datos sin anotaciones ni reflexion. Se integra nativamente con `PlatformException`.

## Caracteristicas

- **Acumulativo**: Recolecta todos los errores de una vez en lugar de fallar en el primero.
- **Tipado**: Acceso directo al objeto validado via `value`.
- **Integrado**: Lanza `PlatformException` con el codigo `VALIDATION_FAILED`.

## Uso basico

```kotlin
data class User(val name: String, val age: Int)

validate(user) {
    check(value.name.isNotBlank()) { "El nombre es obligatorio" }
    check(value.age >= 18) { "Debe ser mayor de edad" }
}
```

Si alguna condicion falla, se lanza una `PlatformException`:
- `code`: "VALIDATION_FAILED"
- `messages`: Lista de todos los mensajes de error recolectados.
- `details`: `{"target": "User"}`

## Integracion en Steps

Ideal para usar al inicio de un `Step` en el motor de flujos.

```kotlin
class CreateUserStep : Step<UserContext> {
    override suspend fun execute(ctx: UserContext): UserContext {
        validate(ctx.user) {
            check(value.email.contains("@")) { "Email invalido" }
        }
        // ... persistencia
        return ctx
    }
}
```
