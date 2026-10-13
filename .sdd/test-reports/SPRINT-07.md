# Informe de pruebas — SPRINT-07
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comandos y resultados reales:
  1. `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**,
     `FuentesMapaTest`: 4 tests, 0 fallos, 0 errores (ver
     `app/build/test-results/testDebugUnitTest/TEST-es.surcogps.app.mapa.FuentesMapaTest.xml`).
     Casos: URL Esri imagery `tile/{z}/{y}/{x}` exacta, URL de referencia exacta,
     `configPara` de los 3 modos, atribución con OSM + Esri.
  2. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**. APK 12 979 878 bytes.
- Incidencias resueltas:
  - Firma real de `XYTileSource` en 6.1.20 verificada con `javap` sobre el AAR
    descargado (sin `resourceId`, con `String[]`): código adaptado.
  - `androidx.preference.PreferenceManager` evitado (usa SharedPreferences directas).
- Verificación visual en pantalla (teselas renderizadas) NO ejecutada: sin
  dispositivo físico y sin emulador (VT-x desactivado en firmware, ver revisión).
  El cableado modo→fuente→overlay queda cubierto por `configPara` + tests; el
  render lo hace OSMDroid aguas abajo. Prueba en dispositivo: Sprint 12 (QA).
- Decisión: PASS (con deuda explícita de verificación visual).
