# SPRINT-02 — Geometría: GeoMath + GuidanceEngine
- Objetivo: líneas rectas paralelas métricas según rumbo y ancho de apero.
- Alcance: `core/.../GeoMath.kt`, `GuidanceEngine.kt`; rumbo [0,360), ancho [0.5,30] m;
  marco ENU equirectangular con cos(latRef); XTE con signo; línea más cercana; shift lateral; re-rumbo.
- Tareas: 1) GeoMath (toEnu/fromEnu, haversine, destino, normalizarRumbo) 2) GuidanceEngine
  3) tests borde (0/90/180/270/359, anchos extremos, separación exacta, shift, ancla estable).
- Aceptación: separación entre paralelas == ancho ±1 mm en tests; XTE(±) correcto en 4 rumbos cardinales.
- Pruebas: `GeoMathTest`, `GuidanceEngineTest` (≥15 casos).
- Riesgos: regresión numérica cerca de latRef; documentar error <1% en <5 km.
