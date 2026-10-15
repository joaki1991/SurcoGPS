# Informe de pruebas — SPRINT-43
- Fecha: 2026-10-15 · Ejecutado por: sesión principal OpenCode.
- Suites forzadas (`--rerun-tasks`): **core 70/70 + app 74/74, 0 fallos**.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.6.2: 8 630 628 B → `releases/SurcoGPS-v1.6.2-release.apk`
  (`apksigner verify` OK, firma `CN=SurcoGPS-Dev` coincidente).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='12'
  versionName='1.6.2'`, min 26, lanzable.
- Requiere móvil: instalar v1.6.2 SIN borrar nada antes; el mapa debería
  pintar en segundos. Si aparece en el registro «cache por defecto no
  escribible → interna», la causa queda doblemente confirmada.
- Decisión: PASS.
