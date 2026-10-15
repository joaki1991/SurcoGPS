# SPRINT-38 — Diagnóstico autónomo en app
- Objetivo: diagnosticar sin ADB ni Android Studio, desde el móvil.
- Alcance: `soporte/RegistroDiag.kt` (jsonl acotado 300, sesiones, hilos),
  `soporte/InformeDiag.kt` (TXT sin coords exactas/secretos), `mapa` +
  `comprobarDetalle`, VM (estado, chequeo de 3 teselas, exportar, limpiar),
  `ui/PantallaDiagnostico.kt` (ruta desde Ajustes), ciclo de vida en
  `MainActivity`, sesión + espejo de fatales en `SurcoApp`, contadores osmdroid
  leídos sin subclases.
- Aceptación: pantalla usable; registro sobrevive al cierre; exportar comparte;
  red vs render separados (HTTP / descarga-caché / estado); nada borra
  parcelas/sesiones/ajustes; suite verde.
- Pruebas: `RegistroDiagTest` 6/6 + `InformeDiagTest` 2/2 + detalle HTTP 1/1 +
  suite 68/68 + `assembleDebug`.
