# Informe de pruebas — SPRINT-08
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comandos y resultados reales:
  1. `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**; 10 tests,
     0 fallos (`GuiadoPipelineTest` 6/6 + `FuentesMapaTest` 4/4).
  2. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**.
- Casos del pipeline (ubicaciones simuladas, sustituto ejecutable del mock-location
  en dispositivo): avance recto en línea 0 con XTE≈0 y rumbo 0°; deriva a 7 m → línea 1;
  fix impreciso rechazado conservando el último XTE válido; offset 2 m → XTE≈2;
  reinicio sin salto fantasma; sin ancla no hay XTE pero sí aceptación.
- No ejecutado (sin dispositivo/emulador): recepción Fused real, diálogo de permiso
  en pantalla, NMEA en hardware. Código revisado contra APIs Play-services 21.3.0.
  Prueba con mock locations: Sprint 12 si hay dispositivo.
- Decisión: PASS.
