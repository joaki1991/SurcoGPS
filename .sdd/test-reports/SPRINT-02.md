# Informe de pruebas — SPRINT-02
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comando: `.\gradlew.bat :core:test --console=plain` (Gradle 8.10.2, JVM 21, Kotlin 2.0.20).
- Resultado: **BUILD SUCCESSFUL** — 23/23 tests PASS (2 humo + 8 GeoMath + 13 Guidance).
- Incidencia durante el sprint: `signoXteDerechaPositivoRumboEste` falló en primera
  ejecución porque el punto de prueba estaba exactamente a media calle (2 m con ancho
  4 m → empate de línea, XTE −2 contra la línea 1). Causa: punto de prueba ambiguo,
  no error de implementación. Corrección: offsets de 1 m (inequívocos). Reejecución: verde.
- Cobertura de casos: rumbos 0/90/180/270/359/360/−90, anchos 0.5/30/fuera de rango,
  separación métrica exacta (10.5 m ±1 cm), shift lateral, re-rumbo, segmento invertido.
- Decisión: PASS.
