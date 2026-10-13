# Informe de pruebas — SPRINT-12
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.

## Suites completas (verdes)
- `.\gradlew.bat :core:test` → BUILD SUCCESSFUL — **70/70** (humo 2, GeoMath 8,
  Guidance 13, Coverage 11, Filter 11, Offset 7, SessionStore 8, SessionJson 4, Parcela 6).
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL — **20/20**
  (FuentesMapa 4, GuiadoPipeline 6, CoberturaEngine 7, Politicas 3).

## Compilación y lint
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:assembleRelease` → BUILD SUCCESSFUL (incluye `lintVitalRelease` sin fatales).

## Artefactos verificados
| APK | Tamaño (bytes) | package | min/target | Firma |
|---|---|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 11 648 261 | `es.surcogps.app` v1 (1.0.0) | 26 / 34 | debug (`apksigner verify` OK) |
| `app\build\outputs\apk\release\app-release.apk` | 8 266 192 | `es.surcogps.app` v1 (1.0.0) | 26 / 34 | `CN=SurcoGPS-Dev` SHA-256 `39:46:59:9E:…:C4:01` (`apksigner verify --print-certs` OK, coincide con el keystore local) |
- `aapt dump badging` en ambos: actividad lanzable `es.surcogps.app.MainActivity`, etiqueta `SurcoGPS`.

## Incidencias del sprint
- `java.util.Properties` ambiguo en `app/build.gradle.kts` (`java` = extensión del plugin): import explícito. Verde.
- Búsqueda de secretos/TODOs en producción: limpia (ver `SEGURIDAD-12.md`).

## No ejecutado (bloqueo externo documentado)
- Instalación y prueba en dispositivo/emulador: `adb devices` vacío y VT-x
  desactivado en firmware (verificado en Sprint 07). Pendiente con hardware:
  instalar APK, conceder permiso, fijar ancla, caminar 100 m, comprobar franja +
  hectáreas, matar app y reanudar, pantalla apagada 5 min. El usuario puede
  ejecutarlo con la guía de instalación del README.

## Decisión: PASS (entrega con deuda explícita de prueba en hardware).
