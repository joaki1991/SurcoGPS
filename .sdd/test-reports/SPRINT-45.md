# Informe de pruebas — SPRINT-45
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 75/75, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.7.0: 8 630 628 B → `releases/SurcoGPS-v1.7.0-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='13'
  versionName='1.7.0'`, min 26, lanzable.
- Requiere móvil: instalar v1.7.0 SIN borrar nada; ir a otra pestaña y volver
  (el registro debe mostrar UNA creación de MapView por proceso); Ejecutar
  diagnóstico (sonda SQL honesta) + Exportar informe.
- Decisión: PASS.
