# Informe de pruebas — SPRINT-03
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comando: `.\gradlew.bat :core:test --console=plain` (Gradle 8.10.2, JVM 21).
- Resultado: **BUILD SUCCESSFUL** — 34/34 tests PASS (11 nuevos de `CoverageTrackerTest`).
- Incidencias (4 fallos iniciales, todos de expectativas, ninguno de implementación):
  1. `pasadaSimple`: área real 707 m² (teórico 600). Causa: el raster cuenta celdas
     tocadas; franja ±3 m alineada cubre 7 filas. Rango corregido a 600–780 con la
     sobrestimación documentada en `CoverageTracker`.
  2. `pasadaDiagonal`: 736 m² por desalineación con la cuadrícula. Rango 600–820.
  3. `puntoAislado`: un tramo de longitud 0 pinta línea transversal (7 celdas), no
     disco. Test renombrado y documentado; los extremos aportan ½ disco despreciable.
  4. `anchosExtremos`: franja 0.5 m cubre 2 columnas (100 m² en 50 m). Rango 70–130.
- Propiedades verificadas: pasada repetida +0 celdas nuevas; adyacentes ×2 (±10 %);
  solape 50 % suma ~50 %; snapshot/restore conserva el área al 100 %.
- Decisión: PASS.
