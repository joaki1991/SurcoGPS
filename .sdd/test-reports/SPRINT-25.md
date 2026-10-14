# Informe de pruebas — SPRINT-25
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **49/49**
  (`CrashReporterTest` 4/4 nuevo: guardar/leer, rotación a 5, traza con causa,
  borrado).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Incidencias: `import kotlinx.coroutines.launch` perdido al adelgazar el VM
  (las extensiones deben importarse); test con `kotlin.test` en `:app` que usa
  JUnit4 (reescrito). Ambas detectadas por el compilador.
- Nota honesta: la causa del cierre v1.3.0 sigue sin identificar (sin logcat);
  este sprint entrega el instrumento para obtenerla + degradación segura.
- Decisión: PASS.
