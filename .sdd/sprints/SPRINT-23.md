# SPRINT-23 — Líneas guía en mapa + ancla por toque
- Objetivo: ver las paralelas del ancho, seguirlas y colorear lo recorrido.
- Alcance: `mapa/LineasGuia.kt` (`BBox`, `EstadoGuia`, `indicesLineasVisibles`,
  `rangoAlong`), `mapa/LineasOverlay.kt` (paralelas + activa amarilla + ancla
  roja + long-press con Toast), motor (`guiaActual`, `fijarAnclaEn`), VM y
  `CampoScreen` (cableado), `RangoLineasTest`.
- Aceptación: líneas del ancho separadas sobre la vista + activa resaltada +
  ancla marcada; long-press fija ancla con aviso; capa única persistente.
- Pruebas: `RangoLineasTest` 4/4 + suite 45/45 + `assembleDebug`.
