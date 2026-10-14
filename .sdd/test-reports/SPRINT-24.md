# Informe de pruebas — SPRINT-24
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 45/45, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.3.0: 8 597 308 B → `releases/SurcoGPS-v1.3.0-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='4'
  versionName='1.3.0'`, `application-label:'SurcoGPS'`, MainActivity lanzable.
- No ejecutado: captura de confirmación en dispositivo (píldora con cifras,
  líneas guía, long-press, clip). Se pide al propietario.
- Decisión: PASS.
