# Informe de pruebas — SPRINT-01
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comando: `.\gradlew.bat :core:test --console=plain` (Gradle 8.10.2, JVM 21, Kotlin 2.0.20).
- Resultado: **BUILD SUCCESSFUL** — 2/2 tests PASS.
  - `CoreSmokeTest > versionExpuesta()` PASSED
  - `CoreSmokeTest > normalizaRumbos()` PASSED
- Notas: el wrapper descargó la distribución `gradle-8.10.2-bin.zip` (~130 MB) en la
  primera ejecución. Sin dependencias Android en `:core` (verificado en `core/build.gradle.kts`).
- Decisión: PASS.
