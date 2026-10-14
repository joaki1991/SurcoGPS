# Informe de pruebas — SPRINT-15
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; 24/24
  (`PoliticaCamaraTest` 4/4 nuevo: sin-seguir, primer centro, umbral 2 m, jitter).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Verificación estructural (revisión de código): `setTileSource`, `removeAll` y
  creación de proveedor/overlay movidos tras la guardia `modo != ultimoModo`;
  `setCenter` solo vía destino one-shot o umbral >2 m; `MapListener` distingue
  gesto programático de manual; `invalidate` solo con cambios o franja no vacía.
- No ejecutado: comprobación visual del parpadeo en dispositivo (sin hardware).
  Se eliminaron por construcción las 4 causas identificadas en SPRINT-13.
- Decisión: PASS.
