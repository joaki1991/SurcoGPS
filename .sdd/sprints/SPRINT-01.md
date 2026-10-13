# SPRINT-01 — Fundación: Gradle + :core + tests
- Objetivo: proyecto reproducible que compile y testee el núcleo sin Android SDK.
- Alcance: `settings.gradle.kts`, `gradle.properties`, `.gitignore`, `gradlew` + wrapper
  8.10.2, `:core/build.gradle.kts` (Kotlin 2.0.20, JUnit5), clase humo + test humo.
- Módulos/archivos: raíz Gradle, `core/`.
- Tareas: 1) wrapper y settings 2) build de core 3) test humo 4) `.gitignore` 5) ejecutar tests.
- Aceptación: `.\gradlew.bat :core:test` verde; sin dependencias Android en `:core`.
- Pruebas: JUnit5 humo (`CoreSmokeTest`).
- Riesgos: descarga de Gradle/distribución (red); Java 21 vs toolchain (fijar jvmToolchain 17).
