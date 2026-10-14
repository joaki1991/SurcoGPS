# Informe de pruebas — SPRINT-29
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **53/53**
  (nuevos: `DiagnosticoTeselasTest` 4/4 con servidor HTTP local real —
  URL Esri exacta, 200/404/puerto cerrado — y `TextoColaTest` 1/1).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Incidencias: `com.sun.net.httpserver` ausente en tests (reescrito con
  `ServerSocket`); expectativa z/y/x copiada de otro test (corregida a 10/395/500).
- Evidencia externa: Esri 200 + JPEG 256 válido; OSM 200 en z0 (throttling
  intermitente en otras).
- Decisión: PASS.
