# Memoria — estado actual

## 2026-10-13 — Arranque (sesión principal, sin subagentes)
- Inspección: repo vacío (solo `.git`), rama `main` sin commits, remoto
  `origin=https://github.com/joaki1991/SurcoGPS.git`.
- Herramientas: Java 21.0 (Oracle LTS), Node 20.15.1, winget 1.29.380.
  Sin Gradle, sin Android SDK, sin adb/kotlin. Red OK (Gradle services alcanzable),
  disco C: 351 GB libres.
- Creados: `AGENTS.md`, `.sdd/constitution.md`, `.sdd/architecture.md`, `README.md`
  (plan maestro de 12 sprints).
- Decisión: `:core` puro Kotlin/JVM primero (testeable sin SDK); `:app` Android
  después de instalar SDK (cmdline-tools + platform 34 + build-tools 34).
- Siguiente paso: SPRINT-01 (Gradle wrapper + `:core` + test humo).

## 2026-10-13 — SPRINT-01 PASS
- Gradle 8.10.2 (descargado ~130 MB) + wrapper generado (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.jar`).
- `.\gradlew.bat :core:test` → BUILD SUCCESSFUL, 2/2 PASS (ver
  `.sdd/test-reports/SPRINT-01.md`, `.sdd/reviews/SPRINT-01.md`).
- Siguiente: commit + push SPRINT-01, luego SPRINT-02 (GeoMath + GuidanceEngine).
