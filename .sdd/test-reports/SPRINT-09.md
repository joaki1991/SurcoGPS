# Informe de pruebas — SPRINT-09
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comandos y resultados reales:
  1. `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**; 16 tests,
     0 fallos (`CoberturaEngineTest` 6/6 nuevo + 10 previos).
  2. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**.
- Casos nuevos: avance 100 m/6 m → área en rango y 10 tramos; quieto (5× mismo
  punto) → 0 tramos y 0 m²; pausa congela y reanuda sin tramo fantasma; ida y
  vuelta por el mismo sitio no duplica; reinicio limpia; ha == m²/10000.
- Incidencia: `quietoNoColorea` falló al inicio (tramos de longitud 0 pintaban la
  línea transversal y se apilaban). Causa de diseño, corregida en `CoberturaEngine`
  (umbral `MIN_TRAMO_M = 0.1 m`, sin actualizar el último punto). Reejecución verde.
- No ejecutado (sin dispositivo): render de la franja en pantalla. `FranjaOverlay`
  usa escala px/m por tramo (haversine) — verificable visualmente en Sprint 12.
- Decisión: PASS.
