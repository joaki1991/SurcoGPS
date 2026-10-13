# Revisión — SPRINT-06 (revisor independiente)
- Alcance: `settings.gradle.kts` (+`:app`), `build.gradle.kts` raíz, `app/` completo
  (build, manifest, MainActivity, 4 pantallas, ViewModel, DataStore).
- Aceptación: `:app:assembleDebug` OK + badging `es.surcogps.app` + actividad
  lanzable — CUMPLE. Navegación entre 4 pantallas (código verificado; sin
  dispositivo en este sprint) — CUMPLE con prueba en dispositivo pendiente (07/08).
- Botones muertos: ninguno. Todos los controles ejecutan acción real (navegar,
  guardar rumbo/ancho/shift/puerta vía DataStore). El botón "Centrar shift" se
  deshabilita con motivo cuando ya es 0. Las pantallas Parcela/Sesión son
  informativas (texto, no botones falsos) y declaran su sprint — OK.
- Seguridad/privacidad: permisos declarados según arquitectura; sin solicitar nada
  en ejecución todavía; sin secretos (`local.properties` ignorado por git, verificado
  ausente del stage); sin telemetría — OK.
- Deuda anotada (no bloqueante): falta icono de lanzador propio (se usa el del
  sistema); arranque en emulador pendiente de verificar en Sprint 07.
- Regresiones: `:core:test` 60/60 verde. Cambios ajenos: ninguno.
- Decisión: **PASS**.
