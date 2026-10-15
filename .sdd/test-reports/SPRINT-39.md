# Informe de pruebas — SPRINT-39
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 68/68, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.6.0: 8 630 628 B → `releases/SurcoGPS-v1.6.0-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='10'
  versionName='1.6.0'`, min 26, lanzable.
- Requiere móvil (declarado, no ejecutado):performance de la pantalla,
  chequeo de 3 teselas, exportar/compartir TXT, persistencia tras muerte,
  protocolo de 10 ciclos del informe SPRINT-37.
- Decisión: PASS.
