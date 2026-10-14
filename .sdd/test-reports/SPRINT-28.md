# Informe de pruebas — SPRINT-28
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 49/49, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.3.2: 8 614 244 B → `releases/SurcoGPS-v1.3.2-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='6'
  versionName='1.3.2'`, min 26, lanzable.
- Pendiente del propietario: instalar v1.3.2, usarla con normalidad y enviar
  captura con la píldora completa (`▦ ok · fallo · HTTP …`).
- Decisión: PASS.
