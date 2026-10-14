# Informe de pruebas — SPRINT-17
- Fecha: 2026-10-14 · Ejecutado por: sesión principal OpenCode.
- `.\gradlew.bat :app:testDebugUnitTest` → BUILD SUCCESSFUL; **35/35**
  (`EncuadreTest` 4/4 nuevo: one-shot, impreciso no dispara, reinicio, recorte).
- `.\gradlew.bat :app:assembleDebug` y `:app:assembleRelease` → BUILD SUCCESSFUL.
- Release v1.1.0: `app-release.apk` 8 580 920 B → `releases/SurcoGPS-v1.1.0-release.apk`
  (`apksigner verify` OK).
- `aapt dump badging`: `package='es.surcogps.app' versionCode='2' versionName='1.1.0'`,
  `application-label:'SurcoGPS'`, actividad lanzable OK, firma `CN=SurcoGPS-Dev`
  (huella coincide con keystore local).
- Falsa alarma investigada: `application-icon-*:res/BW.xml` en el release. Causa:
  AAPT2 colapsa los nombres de recursos en release (el debug conserva rutas).
  Verificado con `aapt2 dump xmltree --file res/BW.xml`: es nuestro
  `<adaptive-icon>` (background + foreground). Icono intacto.
- No ejecutado: encuadre real en dispositivo (sin hardware). Política one-shot,
  recorte de zoom y respeto de gestos, verificados por tests + revisión.
- Decisión: PASS.
