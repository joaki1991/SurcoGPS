# Informe de pruebas — SPRINT-04
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comando: `.\gradlew.bat :core:test --console=plain` (Gradle 8.10.2, JVM 21).
- Resultado: **BUILD SUCCESSFUL** — 52/52 tests PASS (11 `GpsFilterTest` + 7 `ImplementOffsetTest` nuevos).
- Incidencia: `offsetDesplazaXteEnGuia` falló una vez por empate exacto de media calle
  (3 m con ancho 6 → `round(0.5)=1`, XTE −3 contra línea 1). Punto de prueba movido a
  2.5 m totales (inequívoco). Reejecución verde.
- Casos: puerta 10 m (25 m rechazado, 10 m aceptado), NaN/negativa rechazados, salto
  500 m/s rechazado, 10 m/s aceptado, caducidad 30 s > 10 s rechazada, puerta
  configurable, el rechazo no avanza el último fix, offset en rumbos 0/90/180/270,
  apero arrastrado (−2 m), integración offset→XTE.
- Decisión: PASS.
