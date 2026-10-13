# Informe de pruebas — SPRINT-05
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comando: `.\gradlew.bat :core:test --console=plain` (Gradle 8.10.2, JVM 21).
- Resultado: **BUILD SUCCESSFUL** — 60/60 tests PASS (8 nuevos `SessionStoreTest`).
- Sin incidencias: verde a la primera ejecución.
- Casos: round-trip completo (track 11 puntos + ~700 celdas + offset + nombre con
  comillas/barras), lista ordenada por actualización, borrado, carga inexistente → null,
  fichero corrupto → null sin excepción, sesión vacía, reanudación de interrumpida
  (restaura celdas, suma 2.ª pasada ×1.8, marca finished), compactación por filas.
- Decisión: PASS.
