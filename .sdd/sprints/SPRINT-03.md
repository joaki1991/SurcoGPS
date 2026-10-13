# SPRINT-03 — Cobertura: CoverageTracker raster
- Objetivo: colorear solo recorrido real y medir superficie sin duplicar.
- Alcance: `core/.../CoverageTracker.kt`; franja = cápsula segmento ± ancho/2;
  rasterización de celdas 1 m en marco ENU; área = celdas×1 m²; reset; stats.
- Tareas: 1) rasterizar segmento (ancho) 2) set de celdas con clave Long 3) área única
  4) tests (pasada simple, solape total/parcial, ida+vuelta, diagonal, reset).
- Aceptación: 100 m × 6 m = 600 m² ±2%; segunda pasada idéntica añade <2%; adyacentes suman.
- Pruebas: `CoverageTrackerTest` (≥10 casos).
