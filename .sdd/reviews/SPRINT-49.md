# SPRINT-49 — Revisión independiente (diff completo)

Revisión hecha como revisor separado sobre el diff de la sesión principal, sin
confiar en los informes previos: se leyeron los ficheros completos afectados, se
ejecutaron las suites y los builds, y se probó el APK en emulador Android 14.

## 1. Cambios revisados

| Fichero | Cambio | Veredicto |
|---|---|---|
| `core/.../GpsFilter.kt` | `Fix.bearingDeg` opcional, documentado | OK |
| `app/build.gradle.kts` | v2.0.0 / versionCode 16 | OK |
| `AndroidManifest.xml` | sin `FileProvider` ni receptor de arranque; servicio `location` | OK |
| `MainActivity.kt` | una Activity, edge-to-edge, pantalla encendida | OK |
| `motor/MotorCampo.kt` | estado de campo sin sesiones/parcelas; cobertura por ancho vigente; brújula; reinicio solo en memoria | OK tras corregir NPE de arranque |
| `ui/CampoViewModel.kt` | fachada mínima; mapa retenido; seguir/centrar; sin borrado de datos | OK |
| `ui/CampoScreen.kt` | pantalla única, panel vertical/horizontal, tarjeta de permiso, diálogo de nuevo recorrido | OK |
| `mapa/MapaSimple.kt` (nuevo) | mapa a pantalla completa, offset de cámara, seguimiento, capas acotadas | OK |
| `mapa/MapaRetenido.kt` | una sola fuente de teselas, vista retenida, `setDestroyMode(false)`, `garantizarCapas()` | OK tras corregir pérdida de capas al rotar |
| `mapa/LineasGuia.kt` | recorte por bbox, radio 10 km, tope 200 líneas | OK |
| `mapa/LineasOverlay.kt` | dibujo acotado, grosores en dp, línea activa y ancla | OK |
| `mapa/UbicacionOverlay.kt` | punto azul, flecha, cono de precisión | OK |
| `mapa/RumboFlecha.kt` (nuevo) | brújula con respaldo GPS solo en movimiento | OK |
| `gps/RumboSensor.kt` (nuevo) | rotation vector + declinación + rotación de pantalla | OK (pendiente sensor real) |
| `servicio/TrackingService.kt` | notificación simple, sin reanudación automática | OK |
| `ui/AjustesScreen.kt`, `ParcelaScreen.kt`, `SesionScreen.kt`, `PantallaDiagnostico.kt`, `AvisoCierre.kt`, `MapaCampo.kt`, `ArranqueReceiver.kt`, `res/xml/ficheros.xml` | eliminados del flujo de producción | OK |
| Tests nuevos/modificados | 15 casos (rumbo, líneas acotadas, cardinal, ancho sin borrar, rumbo en el fix) | OK |

## 2. Comprobaciones de fondo

- **Datos del usuario**: ninguna ruta borra `files/sesiones`, `files/parcelas`,
  DataStore `ajustes` ni la caché de teselas. Los únicos borrados son el fichero
  temporal de la sonda de almacenamiento, los cierres antiguos (tope) y el propio
  registro de diagnóstico. «Nuevo recorrido» solo limpia la cobertura en memoria.
- **Métrica**: líneas y cobertura siguen en marco ENU local; no se restan grados.
- **Precisión**: la píldora muestra siempre ±m; la puerta de 10 m sigue aplicándose.
- **Sin restos**: búsqueda de `TODO/FIXME/placeholder/simulad` sin resultados;
  sin `onClick = {}` vacíos; sin claves ni secretos nuevos.
- **Atribución**: visible en ambos modos (Esri World Imagery / OpenStreetMap).

## 3. Incidencias detectadas por la revisión (y estado)

1. **Crash al abrir** (`MotorCampo.kt`): `init` usaba `guiado` antes de su
   inicialización. Corregido y verificado con arranque en frío.
2. **Capas borradas al girar**: `MapView.onDetachedFromWindow()` llamaba a
   `onDetach()` y OSMDroid vaciaba los overlays (`capas=0` en el registro).
   Corregido con `setDestroyMode(false)` + restauración defensiva. Verificado:
   `capas=4` y dibujo correcto tras rotar en ambos sentidos.
3. **Seguir no recentraba al rotar**: se añadió comparación del centro real del
   mapa con la posición. Verificado en emulador.
4. **Diagnóstico temporal**: se añadió registro de tamaño/zoom/capas para
   localizar el fallo 2 y se retiró al confirmar la corrección (no queda ruido).

## 4. Pruebas ejecutadas por el revisor

| Comando | Resultado |
|---|---|
| `.\gradlew.bat :core:test --rerun-tasks` | BUILD SUCCESSFUL, 79/79 |
| `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 103/103 |
| `.\gradlew.bat :app:assembleDebug :app:assembleRelease` | BUILD SUCCESSFUL |
| `aapt dump badging` | `es.surcogps.app` 2.0.0 (16), min 26, target 34 |
| `apksigner verify --print-certs` | OK, `CN=SurcoGPS-Dev` |
| Emulador AVD `surco` (Android 14) | arranque, GPS, pintura, ±0,1 m, pausa, diálogo, ocultar panel, satélite/calles, rotación y vuelta |

## 5. Decisión

**PASS** con una salvedad declarada: no hay dispositivo físico en esta sesión,
por lo que la brújula real, el GPS de campo, el consumo y la pintura con apero
real quedan pendientes de verificación física (registrado en el informe de
pruebas y en README). El emulador confirma el comportamiento lógico y visual.
