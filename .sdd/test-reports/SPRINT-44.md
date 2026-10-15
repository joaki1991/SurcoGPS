# Informe de pruebas — SPRINT-44
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **75/75**
  (74 previos + `veredictoSonda`).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Incidencias: bloque duplicado al migrar a retenido + import ausente
  (compilador, corregidos).
- No ejecutado: ida y vuelta Campo→Ajustes→Campo en dispositivo (sin hardware);
  el registro «MapView creado (retenido)» debe salir UNA vez por proceso.
- Decisión: PASS.
