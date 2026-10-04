# Kardex — tienda y depósito

API del kardex. Una sola aplicación, tres locales: Uniformes, Artículos religiosos y Limpieza.

El contrato de análisis está en `docs/00-analisis/Documentacion_Analisis_Diseno.md`. Las decisiones viven en `../inventario/DECISIONES.md`.

## Stack

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.1 |
| Spring Security + JWT | jjwt 0.12.3 |
| MySQL | 8 |
| Maven | wrapper |

## Cómo correrlo

1. MySQL local encendido.
2. Ajustar usuario y clave en `src/main/resources/application.properties` si no son los de tu máquina. La base `inventario` se crea al arrancar.
3. Con JDK 17:

```bash
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8081`.

## Usuarios de demostración

| Usuario | Clave | Local de trabajo |
|---|---|---|
| rosita | rosita123 | Uniformes y artículos religiosos |
| mary | mary123 | Limpieza |

Los productos de muestra son provisionales. No son el catálogo real.

## Regla que no se negocia

El stock no se edita. Una salida mayor que el saldo responde 409 y no guarda. Un cambio guarda dos líneas o ninguna.
