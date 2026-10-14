# Informe de pruebas — SPRINT-14
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Fuente verificada visualmente (`FaviconSurcoGPS.png` == adjunto del propietario),
  1254×1254 RGB, copiada a `assets/icono-fuente.png`.
- `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL. APK 12 232 972 B.
- `aapt dump badging` → `application: label='SurcoGPS' icon='res/mipmap-anydpi-v26/ic_launcher.xml'`
  + `application-icon-{120…640}'` → mismo XML. Icono resuelto en todas las densidades.
- No ejecutado: vista del icono en el lanzador del dispositivo (sin hardware).
- Decisión: PASS.
