# SPRINT-04 — GPS: GpsFilter + ImplementOffset
- Objetivo: solo ubicaciones válidas generan trabajo; soporte de brazo antena→apero.
- Alcance: `core/.../GpsFilter.kt` (puerta precisión def. 10 m, puerta salto def. 50 m/s,
  caducidad def. 10 s, racha mínima 1), `ImplementOffset.kt` (dx lateral, dy longitudinal
  rotados por rumbo del vehículo), `Fix`/`FilteredFix` data classes.
- Tareas: 1) modelo Fix 2) filtro con motivos de rechazo 3) offset rotado 4) tests.
- Aceptación: fix 25 m precisión rechazado (puerta 10); salto 500 m en 1 s rechazado;
  offset lateral 2 m a 90° desplaza +E; a 0° desplaza +E igualmente (lateral relativo al rumbo).
- Pruebas: `GpsFilterTest`, `ImplementOffsetTest` (≥12 casos).
