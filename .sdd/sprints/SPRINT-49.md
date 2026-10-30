# SPRINT-49 — SurcoGPS mínimo (v2.0.0)

**Fecha:** 2026-10-30
**Estado:** especificado (implementación en curso)
**Objetivo:** sustituir la capa `:app` por una app de una sola pantalla, mínima y
funcional en campo, sin dejar de conservar `:core`, el identificador
`es.surcogps.app` ni los datos ya guardados en el dispositivo.

## 1. Diagnóstico previo (evidencia)

- `LineasOverlay.draw` ya dibuja solo las paralelas que cortan el `boundingBox`
  visible (`indicesLineasVisibles` + `rangoAlong`), pero **no hay tope** de
  líneas ni de radio: con ancho 0,5 m y una vista amplia se generan miles de
  segmentos por fotograma.
- `MotorCampo.restaurarTrasMuerte()` puede cargar una sesión guardada al
  arrancar; `CampoViewModel` mantiene sesiones, parcelas, diagnóstico, mapa
  retenido y cuatro destinos de navegación. Es complejidad que no aporta al uso
  real solicitado y multiplica los caminos de arranque.
- `CoberturaEngine` se recreaba al cambiar ancho o ancla (`engineKey`), lo que
  **borraba la cobertura en memoria** al ajustar el apero.
- `Fix` no transporta rumbo y `UbicacionOverlay` ya sabe dibujar flecha, pero la
  flecha solo podía usar rumbo de desplazamiento (no la orientación en la que
  mira la persona).
- El mapa está dentro de una `Column` con HUD y botones; no ocupa la pantalla
  completa y la navegación inferior resta espacio en horizontal.

## 2. Alcance funcional (lo único que debe hacer)

1. **Mapa a pantalla completa** (edge-to-edge), también en horizontal. Los
   controles flotan encima; en horizontal pasan a un panel lateral.
2. **Punto azul** con cono de precisión y **flecha de orientación**:
   - brújula del teléfono (rotación + declinación magnética) mientras la UI está
     visible;
   - respaldo por rumbo de desplazamiento GPS si no hay brújula y hay marcha.
3. **Pintura del recorrido** con el ancho real del apero, solo con fixes
   aceptados por el filtro (precisión, caducidad, salto).
4. **Ancho del apero configurable** de 0,5 a 30 m en pasos de 0,1 m, con
   botones − / +, slider y valor visible.
5. **Plantilla de líneas guía** según ancho y orientación:
   - ancla automática en el primer fix válido de cada arranque;
   - botón «Ancla aquí» para recolocarla;
   - orientación por slider (1°), presets N/E/S/O y «Usar brújula»;
   - dibujo acotado al **radio de 10 km** alrededor de la posición y a un
     **máximo de 200 líneas**; si la separación en pantalla es menor de 6 px se
     oculta la familia y se dibuja solo la línea activa (evita miles de trazos).
6. **Controles grandes e intuitivos:** pausar/reanudar pintura, centrar en mi
   posición, seguir posición, cambiar satélite/calles, nuevo recorrido (con
   confirmación y solo memoria).
7. **Arranque limpio:** no se cargan sesiones, parcelas ni coberturas antiguas.
   La app abre en una pantalla, sin navegación inferior, sin parsear ficheros
   grandes.

## 3. Fuera de alcance

- Guardar/reanudar sesiones o parcelas desde la nueva UI.
- Diagnóstico en pantalla, exportar informes, chequeos de red.
- Conducción automática, telemetría, cuentas o nube.
- Borrar datos existentes: `files/sesiones`, `files/parcelas`, DataStore y la
  caché de teselas **no se tocan**. La nueva app simplemente deja de mostrarlos.

## 4. Arquitectura propuesta

```
:core (sin cambios salvo Fix.bearingDeg)
  GeoMath, GuidanceEngine, CoverageTracker, GpsFilter, ImplementOffset,
  SessionStore, ParcelaStore (se conservan y siguen testeados)

:app
  MainActivity            → una sola pantalla, edge-to-edge, keep-screen-on
  ui/CampoScreen          → mapa full-screen + panel de control adaptable
  ui/CampoViewModel       → fachada mínima (estado visual + acciones)
  motor/MotorCampo        → GPS, brújula, cobertura, guía, cámara; sin sesiones
  gps/LocationTracker     → Fused + LocationManager (se conserva), Fix con rumbo
  gps/RumboSensor         → brújula (rotation vector + declinación), NUEVO
  gps/GuiadoPipeline      → filtro + posición del apero (se conserva)
  mapa/MapaSimple         → MapView retenido, satélite/calles, overlays
  mapa/MapaRetenido       → una creación por proceso; sin capa de referencia
  mapa/LineasOverlay      → plantilla acotada (10 km / 200 líneas / 6 px)
  mapa/UbicacionOverlay   → punto + cono + flecha (escala por densidad)
  mapa/FranjaOverlay      → recorrido con ancho métrico (se conserva)
```

### 4.1 Reglas de cálculo

- Toda distancia/anchura en marco ENU local con `GeoMath.toEnu/fromEnu`.
- El ancla de la guía y el ancla de la cobertura son independientes: recolocar
  la plantilla **no** borra lo pintado.
- `CoberturaEngine.registrar(p, anchoActualM)` usa el ancho vigente en cada
  tramo; cambiar el ancho no reinicia la cuadrícula.
- `indicesLineasSeguras` = intersección de:
  `[visibles] ∩ [centro ± 10 km / ancho] ∩ [centro ± 100 líneas]`.
- `rangoAlongSeguro` = `[caja visible] ∩ [proyección del centro ± 10 km]`.
- La flecha usa brújula si existe; si no, rumbo GPS solo con velocidad
  ≥ 0,8 m/s. Sin dato no se inventa orientación.

## 5. Cambios de datos y compatibilidad

- Se mantiene `applicationId = es.surcogps.app`.
- Se mantiene el esquema de DataStore (`ajustes`): se siguen leyendo
  `ancho_apero_m` y `rumbo_deg`; el resto de claves no se borra.
- No se migran ni eliminan ficheros de sesiones/parcelas.
- `Fix` gana `bearingDeg: Float? = null` (compatible con llamadas existentes).
- Se retiran de la app las pantallas Ajustes/Parcela/Sesión/Diagnóstico y el
  receptor de arranque; sus ficheros de soporte puro se conservan si están
  testeados.

## 6. Criterios de aceptación

- [ ] La app abre directamente en el mapa, sin bottom navigation, y no lee
      sesiones ni parcelas al arrancar.
- [ ] Con permiso de ubicación, el GPS arranca solo y el mapa se centra en el
      primer fix.
- [ ] El punto azul se ve con cono de precisión; la flecha responde a la
      brújula (o al rumbo GPS si no hay brújula/marcha).
- [ ] El recorrido se pinta con el ancho configurado; cambiar el ancho no borra
      lo ya pintado.
- [ ] El ancho se ajusta en pasos de 0,1 m y se persiste.
- [ ] La plantilla de líneas respeta orientación y ancho; se acota a 10 km y
      200 líneas; no se dibuja si la separación es < 6 px.
- [ ] Los botones responden y su estado es inequívoco (pausado/pintando,
      satélite/calles, seguir/no seguir).
- [ ] En horizontal el mapa ocupa toda la pantalla y el panel no lo tapa por
      completo (panel lateral colapsable).
- [ ] No se borra ningún dato existente ni la caché.
- [ ] `:core:test` y `:app:testDebugUnitTest` verdes; `assembleDebug` y
      `assembleRelease` OK; APK verificado con `aapt`/`apksigner`.
- [ ] Pruebas en dispositivo físico (GPS real, brújula, campo, rotación)
      declaradas como pendientes si no hay hardware disponible.

## 7. Pruebas previstas

- `RumboFlechaTest`: prioridad brújula, respaldo GPS con marcha, sin dato.
- `LineasSegurasTest`: tope de 200 líneas, radio 10 km, rango longitudinal,
  línea activa cuando la familia se oculta.
- `CoberturaEngineTest`: cambio de ancho conserva celdas y usa el ancho vigente.
- `GpsFilterTest`/`GuiadoPipelineTest`: `bearingDeg` no altera el filtrado.
- `PasosAjusteTest`: ya cubre 0,1 m (se conserva).
- Compilación debug/release y verificación de APK.

## 8. Riesgos y mitigación

| Riesgo | Mitigación |
|---|---|
| La brújula del móvil tiene sesgo/declinación | Corrección con `GeomagneticField`; el usuario puede fijar rumbo a mano |
| Miles de líneas a zoom lejano | Tope 200 + radio 10 km + ocultar si < 6 px |
| Cambiar ancho reinicia cobertura | `registrar(p, anchoActual)` sin recrear el motor |
| Rotación destruye el MapView | `MapaRetenido` en ViewModel; una creación por proceso |
| Datos antiguos interpretados como actuales | No se cargan sesiones; solo se leen ancho/rumbo de DataStore |
| Sin dispositivo físico | Se declara pendiente; emulador solo para compilación/arranque |

## 9. Entrega

- Versión `2.0.0` (`versionCode 16`).
- `releases/SurcoGPS-v2.0.0-release.apk` firmada.
- README, memoria, informe de pruebas y revisión del sprint.
- Commit descriptivo + push a `main` + tag `v2.0.0` solo tras PASS.
