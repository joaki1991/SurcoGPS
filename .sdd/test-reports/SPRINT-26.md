# Informe de pruebas — SPRINT-26
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 49/49, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.3.1: 8 614 244 B → `releases/SurcoGPS-v1.3.1-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='5'
  versionName='1.3.1'`, min 26, lanzable.
- Pendiente del propietario: instalar v1.3.1; si vuelve a cerrarse, abrir la
  app (arrancará degradada si el fallo es el proveedor), pulsar **Compartir**
  en la tarjeta de informe y pegar aquí el texto. Alternativa en PC:
  `adb logcat` con el móvil por USB tras reproducir el cierre.
- Decisión: PASS.
