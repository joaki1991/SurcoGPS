# SPRINT-06 — App base: módulo :app, Compose, permisos
- Objetivo: esqueleto Android compilable con navegación y ajustes.
- Alcance: `app/build.gradle.kts` (AGP 8.5.2, Compose BOM 2024.09.00, minSdk 26/target 34,
  package `es.surcogps.app`), `AndroidManifest`, MainActivity + 4 pantallas
  (Campo, Parcela, Sesión, Ajustes), ViewModels, DataStore ajustes, tema.
- Aceptación: `:app:assembleDebug` OK; navegar sin crashes; sin botones muertos
  (todo botón ejecuta acción real o está deshabilitado con motivo).
- Pruebas: compilación + arranque (logcat / emulador).
- Riesgos: requiere Android SDK (si ausente → BLOCKED documentado).
