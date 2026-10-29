# SPRINT-46 — Reapertura con datos acumulados (rendimiento real)

## Problema reportado
"Al cerrar la app y volver a abrirla, si no borro caché y datos, se ralentiza el
móvil y da problemas". Con datos, abrir/reabrir es lento; sin datos, va bien.

## Evidencia (no suposición)
Medido en `core` con `RendimientoSesionesTest` (PC rápida, tras calentar el JIT):

| Operación | Datos | Tiempo |
|---|---|---|
| `FileSessionStore.list()` | 4 sesiones × 100 000 celdas + 20 000 puntos (1594 KB) | **489 ms** |
| `FileSessionStore.load(id)` | 1 sesión del mismo tamaño | **124 ms** |

Y el código las ejecuta en bucle:
1. `MotorCampo.init` → `recargarListas()` → `list()` (arranque).
2. `CampoScreen` (cada entrada a Campo) → `iniciarGps()` → servicio →
   `restaurarTrasMuerte()` → `load()` **aunque la sesión ya esté en memoria**.
3. `autoguardarSiToca()` cada 15 s → `load()` + `save()` + `recargarListas()`
   → `list()` de **todas** las sesiones, aunque solo cambie una.

`list()` materializa el JSON completo y además **expande** cada sesión a un
`HashSet<Long>` (una `Long` boxeada por m²) y reconstruye todos los `LatLon` del
track. Coste por llamada: cientos de miles de objetos → GC continuo y CPU
sostenida; en móvil, ×5-10 sobre los tiempos de la tabla. Al borrar datos el
problema desaparece, que es exactamente el síntoma.

## Objetivo
Que abrir/reabrir con datos acumulados cueste lo mismo que con la app vacía.

## Alcance
- `core/SessionStore.kt`: lector de resumen en streaming (`SessionResumen`) que
  obtiene id/nombre/instantes/finished/cellCount **sin expandir celdas ni
  materializar el track**. `list()` lo usa.
- `core/SessionStore.kt`: `save()` atómico (`.tmp` + renombrado), para que matar
  la app a mitad de guardado no corrompa la sesión.
- `app/motor/MotorCampo.kt`:
  - `restaurarTrasMuerte()` no recarga si la sesión ya está en memoria.
  - `guardarSesionActiva()` no hace `load()` ni `recargarListas()`: usa el
    nombre y el instante de inicio en memoria y actualiza solo su resumen.
  - `startedMs` real de la sesión (antes se perdía en cada foto).
  - No copiar la lista de tramos si su tamaño no cambió.
- Tests: resumen == carga completa (incluido fichero legado sin `.meta`),
  guardado atómico, y umbral de rendimiento.

## Fuera de alcance
- Cambiar el formato del fichero de sesión (compatibilidad total).
- Sustituir `Set<Long>` por estructura con compresión en memoria.
- Caché de teselas de osmdroid (ya validada en SPRINT-42).

## Aceptación
- `list()` con 4×100 000 celdas < 200 ms en PC (antes 489 ms).
- `list()` no escala con las celdas: 2× datos < 2× tiempo.
- Cero recargas de sesión al cambiar de pestaña o reabrir en el mismo proceso.
- Autoguardado cada 15 s sin releer ni reescribir el resto de sesiones.
- Suites y builds verdes; APK v1.8.0 verificado.
