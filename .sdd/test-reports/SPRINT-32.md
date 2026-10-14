# Informe de pruebas — SPRINT-32
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **56/56**
  (`PuntoRumboTest` 3/3 nuevo: cardinales, cercanía nula, firma).
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Incidencia: smart-cast imposible sobre propiedad delegada (`a.finLat`) en
  `CampoScreen` (locales intermedios). Detectado por el compilador.
- La ralentización de sliders no es reproducible sin dispositivo, pero la causa
  (emisión+disco+pipeline+invalidate por tick) se elimina por construcción:
  cero emisiones durante el arrastre + cero invalidates redundantes.
- Decisión: PASS.
