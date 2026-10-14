# SPRINT-13 — Diagnóstico fase 2 + planificación (sin código)

## 1. Inspección realizada
- Leídos `AGENTS.md`, `README.md` (plan v1.0.0, 12/12 PASS), `.sdd/architecture.md`
  (ADRs 01–06), `.sdd/memory/state.md`.
- Git: rama `main` limpia, 13 commits (12 sprints + entrega v1.0.0 + tag `v1.0.0`).
  Nota: la petición habla de rama `master`, pero la rama principal real del repo
  es `main`; se sigue publicando en `main` (regla de no reescribir historia).
- Código revisado: `mapa/MapaCampo.kt`, `mapa/FranjaOverlay.kt`, `mapa/FuentesMapa.kt`,
  `gps/LocationTracker.kt`, `gps/GuiadoPipeline.kt`, `motor/MotorCampo.kt`,
  `ui/CampoScreen.kt`, `ui/CampoViewModel.kt`, `AndroidManifest.xml`.
- Icono: manifest SIN `android:icon` (se eliminó en Sprint 06 por referencia rota);
  no hay recursos `mipmap-*`. Recurso fuente LOCALIZADO en disco:
  `C:\Users\Joaquin\Downloads\FaviconSurcoGPS.png` (1254×1254, RGB, coincide con
  el adjunto del propietario).

## 2. Causas del parpadeo del mapa (verificadas en código)
`MapaCampo` usa `AndroidView(factory, update)`; `update` se ejecuta en CADA
recomposición, y la pantalla recompone a ~1 Hz (flows `guiado`/`tramos`/`areaHa`):
1. **`mv.setTileSource(...)` en cada update** (`MapaCampo.kt:119`): reasigna el
   proveedor de teselas, vacía la cola y recarga todas las teselas visibles →
   parpadeo con cada fix GPS. Además se crea un `XYTileSource` NUEVO por llamada.
2. **Capa de referencia destruida y recreada en cada update** (`removeAll` +
   `MapTileProviderBasic` + `TilesOverlay` nuevos, líneas 117–126) → recarga del
   overlay híbrido a 1 Hz.
3. **`setCenter(posicionSeguir)` en cada update** (línea 128) mientras `seguir`:
   la cámara se fuerza 1×/s y lucha contra el gesto manual del usuario (zoom,
   pan) → saltos.
4. `mv.invalidate()` incondicional redibuja franja + teselas a 1 Hz (agravante).
- Lo que NO causa el problema: el `MapView` sí se conserva (`remember`), y
  `FranjaOverlay` es capa única (correcto).

## 3. Posición/orientación
- No existe ningún overlay de ubicación (solo HUD textual). Hay que crear
  `UbicacionOverlay` (punto + cono de precisión + flecha de rumbo de
  desplazamiento suavizado). Brújula: no se usará (el rumbo de marcha del
  tractor es la señal fiable; la brújula exige calibración y confunde
  orientación del dispositivo con dirección de marcha — se documenta).
- Botón «Mi zona» actual: centra en vista por defecto (península), NO en el
  usuario. Debe convertirse en «Mi ubicación» (centrar + zoom detalle).

## 4. Plan de sprints fase 2 (añadidos al README)
| Sprint | Contenido | Deps |
|---|---|---|
| 13 | Diagnóstico + planificación (este documento) | — |
| 14 | Icono personalizado (fuente 1254px → mipmaps + adaptativo, manifest, aapt) | 13 |
| 15 | Estabilidad del mapa: aplicar fuente/capas solo al cambiar de modo; cámara por comandos + gesto manual respetado; sin `setCenter` continuo | 13 |
| 16 | Punto azul + cono de precisión + flecha de rumbo suavizado; sin brújula (documentado); estados sin permiso/GPS | 13 |
| 17 | Zoom detalle (19) solo en primer fix válido + botón «Mi ubicación» + estado de carga; sin interferir en gestos | 13, 15 |

Versión de entrega: **v1.1.0** (`versionCode 2`) en `releases/`.
Prueba visual en dispositivo: pendiente de hardware (igual que v1.0.0); se
declara lo no ejecutado en cada informe.

## Decisión: PASS (sprint de documentación; criterios: causas identificadas + plan publicado).
