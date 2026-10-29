# SPRINT-48 — Revisión independiente (diff completo)

Revisión hecha como revisor separado sobre el diff de la sesión principal, sin
confiar en los informes previos: se leyeron los ficheros completos afectados y se
ejecutaron las suites y los builds.

## 1. Cambios revisados

| Fichero | Cambio | Veredicto |
|---|---|---|
| `ui/PasosAjuste.kt` (nuevo) | paso 0,1 m, 294 pasos, redondeo y límites | OK |
| `ui/AjustesScreen.kt` | slider usa el helper; texto en vivo; guarda redondeado | OK |
| `mapa/EstiloFranja.kt` (nuevo) | alfa relleno 210, borde 245, grosor 2,5 | OK |
| `mapa/FranjaOverlay.kt` | usa `EstiloFranja`; sin cambios de geometría/colores | OK |
| `gps/FuenteGps.kt` (nuevo) | elección Fused/plataforma y umbral de respaldo | OK |
| `gps/LocationTracker.kt` | Fused opcional + `LocationManager` inmediato o respaldo a 20 s | OK |
| `mapa/MapaCampo.kt` | sin red no lanza sonda HTTP; registra la omisión | OK |
| `motor/MotorCampo.kt` | expone `fuenteGps` | OK |
| `ui/CampoViewModel.kt` | `fuenteGps` en la foto de diagnóstico | OK |
| `ui/PantallaDiagnostico.kt` | línea «Fuente GPS» | OK |
| `app/build.gradle.kts` | versionCode 15 / 1.9.0 | OK |
| Tests nuevos | 13 casos | OK |

## 2. Comprobaciones de correctitud

- **0,1 m**: `(30 − 0,5) / 0,1 = 295` intervalos → `steps = 294`. El guardado
  redondea a 1 decimal y recorta a `0.5..30.0`, que es exactamente el `require`
  de `AjustesRepository`; no hay ruta que pueda lanzar por coma flotante.
- **Opacidad**: `alpha` de `Paint` va 0–255; 210 sobre relleno y 245 sobre borde
  son válidos. No se tocan colores ni el cálculo de píxeles por metro; el test
  impide bajar de 200.
- **GPS**:
  - Si Play Services no responde, **no se espera**: se pide al `LocationManager`
    en el acto (probado con GMS desactivado).
  - Con Fused, el vigilante solo actúa si no hay fix fresco en 20 s; el primer
    fix de cada arranque se registra sin coordenadas exactas.
  - `publicar` descarta fixes con más de 500 ms de antigüedad y, a igual marca,
    prefiere el de mejor precisión: las dos fuentes no se pisan.
  - `detener` retira Fused, GPS/red, NMEA y el vigilante; no deja callbacks.
  - El permiso se sigue comprobando en `MotorCampo` antes de arrancar.
- **Sin red**: la sonda HTTP se omite y el aviso de caché existente se mantiene.
  No se cambia de modo de mapa de forma mágica (decisión ya documentada).
- **Datos**: no se toca el formato de sesiones, parcelas ni DataStore; el ancho
  sigue en `0.5..30.0`, así que las sesiones antiguas cargan igual.
- **Diagnóstico**: la fuente se muestra en la pantalla y solo se añaden líneas al
  registro acotado; no se registran lat/lon.

## 3. Riesgos y límites

1. `GoogleApiAvailability` se consulta al construir el tracker (hilo principal).
   Medido en emulador: ~150 ms en frío; aceptable para el arranque, pero es el
   único coste nuevo en `onCreate`.
2. En un móvil con Play Services pero GPS apagado, el respaldo tampoco dará
   fix: es limitación del dispositivo, y el diagnóstico lo muestra.
3. La comprobación visual de la franja sobre satélite **no** queda demostrada en
   este informe: el emulador entra en ANR de System UI y tapa la pantalla. Se
   declara pendiente en el móvil físico, sin afirmar lo contrario.

## 4. Incidencias encontradas en la revisión

Ninguna funcional. Dos avisos de compilación ya existentes
(`addNmeaListener` y `setBuiltInZoomControls` obsoletos) y uno de
`versionCode` obsoleto; no rompen ni afectan al sprint.

## 5. Decisión

**PASS** para el código y la entrega v1.9.0, con la verificación en dispositivo
físico (fix real, campo, gestos y contraste visual) declarada **pendiente** y
sin presentarla como hecha.
