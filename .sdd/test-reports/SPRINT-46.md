# Informe de pruebas — SPRINT-46

Fecha: 2026-10-29 · Ejecutado por: sesión principal OpenCode.

## 1. Pruebas automáticas (forzadas)

```
.\gradlew.bat :core:test :app:testDebugUnitTest --rerun-tasks
→ BUILD SUCCESSFUL
→ core 79/79 (0 fallos) · app 75/75 (0 fallos)
```

Nuevas pruebas: `SessionResumen` (resumen == carga completa, celdas negativas,
vacías, fichero corrupto), índice `.meta` (creación, migración de sesión antigua,
índice ilegible, borrado conjunto) y guardado atómico (sin `.tmp` residuales).
`RendimientoSesionesTest` actúa como prueba de regresión del coste de listado.

## 2. Medición en JVM (misma máquina, mejor de 3, tras calentar)

Datos: 4 sesiones × 100 000 celdas + 20 000 puntos de track (1595 KB).

| Operación | Antes | Después |
|---|---|---|
| `FileSessionStore.list()` | 489 ms (una pasada, implementación eliminada) | **2 ms** |
| `FileSessionStore.load(id)` | 124 ms | 117–187 ms (sin cambios funcionales) |

## 3. Medición en emulador Android 14 (x86_64, sin aceleración gráfica)

Mismos datos: 6 sesiones × 100 000 celdas + 20 000 puntos (2,9 MB).
APK debug con el código de producción, compilada AOT (`cmd package compile -m speed`).

| Versión | Evento del registro | Tiempo pared | CPU del hilo |
|---|---|---|---|
| v1.7.0 (antes) | `ANTES listado de 6 sesiones` | **57 950 ms** | **22 128 ms** |
| v1.8.0 (después, migrando índices) | `listado de 6 sesiones` | 3 239 ms | 900 ms |
| v1.8.0 (después, con índice) | `listado de 6 sesiones` | **89 ms** | **17 ms** |

Con v1.7.0 el sistema registró **ANR** de la app (7,7 s) y picos de CPU del 179 %
con pausas de GC de hasta 1,03 s: es el síntoma reportado («se ralentiza el móvil
y da problemas hasta borrar datos»). Con v1.8.0 el listado deja de leer el cuerpo
de las sesiones y no hay ANR ni GC apreciable en la misma prueba.

## 4. Builds y artefacto

```
.\gradlew.bat :app:assembleDebug :app:assembleRelease → BUILD SUCCESSFUL
aapt dump badging  → package='es.surcogps.app' versionCode='14' versionName='1.8.0'
apksigner verify releases/SurcoGPS-v1.8.0-release.apk → OK (CN=SurcoGPS-Dev)
tamaño → 8 647 012 B
```

## 5. No ejecutado (pendiente, declarado)

- Recorrido completo de UI en emulador (crear sesión, cambiar de pestaña,
  reabrir): el host dispone de 7,9 GB de RAM y el SystemUI del emulador entra en
  ANR por falta de memoria; no es un fallo de la app (no hay ANR de
  `es.surcogps.app` tras la corrección). Debe verificarse en el móvil real.
- GPS real, gestos de pellizco/rotación y trabajo de campo: requieren hardware.
- Comprobación en el móvil del dueño de que el registro muestra
  «sesión en memoria: no se recarga al reabrir» al cambiar de pestaña.

## Decisión: PASS (con verificaciones físicas pendientes)
