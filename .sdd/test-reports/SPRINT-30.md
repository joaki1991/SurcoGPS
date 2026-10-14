# Informe de pruebas — SPRINT-30
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 53/53, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.4.0: 8 614 244 B → `releases/SurcoGPS-v1.4.0-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='7'
  versionName='1.4.0'`, min 26, lanzable.
- Pendiente del propietario: instalar v1.4.0 y enviar captura (píldora
  `Cola: N · HTTP …`, mapa, líneas). Si la cola drena a 0 y el mapa se ve,
  causa cerrada; si la cola no baja, el siguiente paso es red/servidor.
- Decisión: PASS.
