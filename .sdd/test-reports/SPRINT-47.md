# Informe de pruebas — SPRINT-47

Fecha: 2026-10-29 · Ejecutado por: sesión principal OpenCode.

- Suites forzadas (`--rerun-tasks`): **core 79/79 · app 75/75**, 0 fallos.
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL.
- Release v1.8.0: 8 647 012 B → `releases/SurcoGPS-v1.8.0-release.apk`.
- `apksigner verify` OK (firma `CN=SurcoGPS-Dev`, misma que versiones previas,
  por lo que se instala encima sin desinstalar).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='14'
  versionName='1.8.0'`, minSdk 26, targetSdk 34.
- Evidencia de rendimiento: ver `.sdd/test-reports/SPRINT-46.md` (57 950 ms →
  89 ms en emulador con los mismos datos; 489 ms → 2 ms en JVM).

Pendiente en móvil real (declarado, no ejecutado aquí): instalar v1.8.0 encima
sin borrar datos, abrir/cerrar varias veces, cambiar de pestaña y comprobar en
el registro «sesión en memoria: no se recarga al reabrir» y un listado de
sesiones en pocos milisegundos.

Decisión: PASS.
