# Informe de pruebas — SPRINT-06
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Entorno: Android SDK en `%LOCALAPPDATA%\Android\Sdk` (cmdline-tools 11076708,
  build-tools 34.0.0, plataforma android-34, platform-tools 37.0.1); licencias
  aceptadas (`licenses/android-sdk-license`); Gradle 8.10.2, AGP 8.5.2, Kotlin 2.0.20.
- Comandos y resultados reales:
  1. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL** (37 tareas). APK:
     `app\build\outputs\apk\debug\app-debug.apk` (10 360 027 bytes).
  2. `aapt dump badging` → `package: name='es.surcogps.app' versionCode='1'
     versionName='1.0.0'`, `sdkVersion:'26'`, `targetSdkVersion:'34'`,
     `launchable-activity: es.surcogps.app.MainActivity`, `application-label:'SurcoGPS'`.
  3. `.\gradlew.bat :core:test` → **BUILD SUCCESSFUL** (60/60, sin regresiones).
- Incidencias resueltas:
  - Conflicto de plugins Kotlin (versión en `core` + `apply false` en raíz) →
    versiones centralizadas en `build.gradle.kts` raíz, módulos sin versión.
  - `Icons.Filled.List` deprecado → `Icons.AutoMirrored.Filled.List`.
  - `@android:mipmap/sym_def_app_icon` inexistente (aapt fallaba) → atributo
    `android:icon` eliminado (icono propio pendiente, anotado en revisión).
- No probado en este sprint (fuera de alcance): arranque en emulador/dispositivo
  (Sprints 07–08).
- Decisión: PASS.
