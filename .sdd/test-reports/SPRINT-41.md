# Informe de pruebas — SPRINT-41
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 72/72, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.6.1: 8 630 628 B → `releases/SurcoGPS-v1.6.1-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='11'
  versionName='1.6.1'`, min 26, lanzable.
- Requiere móvil: Ajustes → Diagnóstico → Ejecutar diagnóstico → Exportar
  informe (pegar el TXT, sobre todo la sección de caché/almacenamiento).
- Decisión: PASS.
