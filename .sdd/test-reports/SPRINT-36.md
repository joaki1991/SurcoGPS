# Informe de pruebas — SPRINT-36
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 59/59, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.5.1: 8 614 244 B → `releases/SurcoGPS-v1.5.1-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='9'
  versionName='1.5.1'`, min 26, lanzable.
- Pendiente del propietario: instalar v1.5.1, esperar 30 s quieto y enviar
  captura (píldora `Cola` + mapa). Si la cola drena y el terreno aparece,
  causa cerrada.
- Decisión: PASS.
