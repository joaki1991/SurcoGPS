# Informe de pruebas — SPRINT-11
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comandos y resultados reales:
  1. `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**; 20/20
     (`PoliticasTest` 3/3 nuevo + 17 previos).
  2. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**.
  3. `.\gradlew.bat :core:test` → **BUILD SUCCESSFUL** (70/70, sin regresión).
- Incidencias:
  - `CampoScreen.kt` con salto de línea perdido tras una edición (detectado por
    el compilador, corregido).
  - `PoliticasTest`: coma decimal del locale es-ES → formato con `Locale.US`
    explícito en la notificación (texto estable); expectativa `0.05→0.0`
    corregida a `0.04` (0.05 binario redondea a 0.1). Reejecución verde.
- Casos: reanudar solo con flag + permiso (4 combos); texto de notificación con
  y sin guía.
- No ejecutado (sin dispositivo/emulador): pantalla apagada 5 min, reboot real,
  notificación visible. Servicio y receptor revisados contra APIs 34. Prueba en
  dispositivo: Sprint 12 si hay hardware.
- Decisión: PASS.
