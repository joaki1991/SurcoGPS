# Informe de pruebas — SPRINT-20
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :core:test :app:testDebugUnitTest --rerun-tasks` → BUILD
  SUCCESSFUL; **core 70/70 + app 40/40, 0 fallos** (XML con timestamps frescos;
  se detectó y corrigió un primer intento con tareas UP-TO-DATE).
- `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL (lint vital incluido).
- Release v1.2.0: 8 597 308 B → `releases/SurcoGPS-v1.2.0-release.apk`
  (`apksigner verify` OK).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='3'
  versionName='1.2.0'`, `application-label:'SurcoGPS'`, MainActivity lanzable,
  firma `CN=SurcoGPS-Dev` (huella coincide).
- No ejecutado: captura de confirmación en dispositivo (se pide al propietario:
  abrir Campo, comprobar chips visibles, teselas cargando y píldora de estado).
- Decisión: PASS.
