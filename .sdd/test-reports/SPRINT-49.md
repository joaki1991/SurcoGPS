# SPRINT-49 — Informe de pruebas

Fecha: 2026-10-30. Sesión principal de OpenCode, sin subagentes.

## 1. Suites unitarias (ejecutadas de verdad, `--rerun-tasks`)

| Comando | Resultado | Casos |
|---|---|---|
| `.\gradlew.bat :core:test --rerun-tasks` | BUILD SUCCESSFUL | **79/79** (0 fallos, 0 errores) |
| `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL | **103/103** (0 fallos, 0 errores) |

Casos nuevos (15):

- `RumboFlechaTest` (6): prioridad brújula; respaldo GPS solo con velocidad
  ≥ 0,8 m/s; sin datos → null; normalización 0–360.
- `LineasSegurasTest` (5): recorte de la familia al recuadro visible, radio de
  10 km, tope de 200 líneas, línea activa siempre incluida.
- `CardinalTest` (2): 8 sectores cardinales y bordes 22,5°/337,5°.
- `CoberturaEngineTest.cambiarAnchoNoBorraYUsaElVigente`: cambiar el ancho no
  borra la superficie ya pintada y los tramos nuevos usan el ancho vigente.
- `GuiadoPipelineTest.conservaElRumboDelReceptorEnElFix`: el rumbo del receptor
  llega intacto al `Fix` (respaldo de la flecha).

## 2. Compilación y APK

| Artefacto | Ruta | Tamaño |
|---|---|---|
| Debug | `releases/SurcoGPS-v2.0.0-debug.apk` | 11 994 865 B |
| Release | `releases/SurcoGPS-v2.0.0-release.apk` | 8 496 741 B |

- `aapt dump badging`: `package: name='es.surcogps.app' versionCode='16'
  versionName='2.0.0'`, `sdkVersion:'26'`, `targetSdkVersion:'34'`,
  `launchable-activity: es.surcogps.app.MainActivity`.
- `apksigner verify --print-certs`: OK. `CN=SurcoGPS-Dev`,
  SHA-256 `3946599ef0f42bde96ddbc382f83a38bd27b523f35e950708b2e7a63f73cc401`.
- SHA-256 del APK release:
  `073B725EBBFE406365F9D266A45C9CB5195160036A379CF4C6E1642E750051F5`.

## 3. Prueba de humo en emulador (real)

AVD `surco`, Android 14 (API 34), pantalla forzada 540×1170, densidad 240
(780×360 dp en horizontal). Google Play Services desactivado en una fase para
forzar el GPS del sistema y reactivado al terminar.

Comandos usados: `adb install -r`, `pm clear`, `pm grant` (ubicación fina,
gruesa y notificaciones), `am start -W`, `emu geo fix` (recorrido recto de
~50 m), `settings put system user_rotation`, `screencap`, `uiautomator dump`,
`run-as … cat files/diag/diag-eventos.jsonl`.

Comprobado en pantalla:

1. Arranque en frío sin crash; permiso concedido; GPS del dispositivo con
   precisión ±5 m visible.
2. Punto azul + flecha + círculo de precisión + líneas guía + franja verde
   pintada; superficie y tramos actualizados.
3. Botones «+»/«−»: 6,0 → 6,2 m en dos pulsaciones de 0,1 m; la superficie
   pintada (0,02 ha, 4 tramos) NO se borra al cambiar el ancho.
4. «Pausar pintura» → píldora «Pintura en pausa» y botón «Reanudar pintura».
5. «Nuevo recorrido» abre confirmación que explica que solo borra lo pintado y
   que las sesiones/parcelas guardadas no se tocan.
6. «Ocultar»/«Controles»: el mapa ocupa toda la pantalla y el punto se centra.
7. Satélite ↔ Calles: cambia la fuente de teselas y la atribución visible
   (Esri World Imagery / © OpenStreetMap contributors).
8. Rotación vertical → horizontal → vertical: punto, flecha, líneas y franja
   siguen dibujados; «Seguir: sí» se mantiene; el punto queda a la izquierda del
   panel en horizontal y por encima del panel en vertical.

### Fallos reales encontrados y corregidos durante la prueba

1. **NPE al arrancar** (`MotorCampo.kt:105`): el `init` coleccionaba `guiado`
   antes de que la propiedad se inicializara (orden de inicialización de Kotlin).
   Corregido moviendo la colección a un `init` posterior; verificado con
   arranque en frío.
2. **Capas perdidas al girar la pantalla**: `MapView.onDetachedFromWindow()`
   llamaba a `onDetach()` (modo por defecto) y OSMDroid borraba las capas; el
   mapa quedaba sin punto/líneas/franja (`capas=0` en el registro). Corregido con
   `setDestroyMode(false)` en la vista retenida + `garantizarCapas()` defensivo.
   Verificado: `capas=4` tras rotar y dibujo correcto en ambas orientaciones.
3. **Seguimiento no recentraba tras rotar** si el fix no se movía: ahora se
   compara también el centro real del mapa con la posición actual.

## 4. Pendiente en dispositivo físico

No hay móvil ni GPS real disponibles en esta sesión. Queda pendiente de
verificación física: brújula real (declinación y remapeo por rotación de
pantalla), precisión/deriva real del GPS, pintura en campo con ancho real,
comportamiento con pantalla apagada y consumo de batería. El emulador no tiene
sensores reales: la flecha en horizontal es un artefacto de la rotación forzada.
