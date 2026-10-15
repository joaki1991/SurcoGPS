# Informe de pruebas — SPRINT-40
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **72/72**
  (`SondaAlmacenTest` 4/4 nuevo: inspección, inexistente, escritura OK,
  fallo con motivo).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- `SqlTileWriter()` + `saveFile/exists/remove` verificados por `javap` en el
  AAR real antes de usarlos; la sonda SQL solo corre en el dispositivo.
- Decisión: PASS.
