# Informe de pruebas — SPRINT-34
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 59/59, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.5.0: 8 614 244 B → `releases/SurcoGPS-v1.5.0-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='8'
  versionName='1.5.0'`, min 26, lanzable.
- Pendiente del propietario: instalar v1.5.0 y comprobar fluidez tras tocar
  Ajustes, rumbo con dos toques, rotación con dos dedos e «Iniciar».
- Decisión: PASS.
