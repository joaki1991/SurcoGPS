# Revisión — SPRINT-19 (revisor independiente)
- Alcance: `MapaCampo.kt` (reescritura), `FuentesMapa.kt` (diagnóstico+política),
  `DiagnosticoTeselas.kt`, tests. `CampoScreen` intacta (misma API).
- Aceptación: controles dentro del cuadro (estructural, no desplazables) — CUMPLE.
  Overlays persistentes + referencia bajo franja — CUMPLE. Diagnóstico con aviso
  honesto + sin auto-conmutación — CUMPLE. Atribución dentro del mapa
  (`CopyrightOverlay`) — CUMPLE. Zoom propio abajo-derecha; pinch intacto — CUMPLE.
- Seguridad: watchdog con timeout y UA propio; sin secretos. OK.
- Calidad: import de iconos extendidos evitado (APK contenido); `LaunchedEffect(modo)`
  + `remember(modo)` coherentes; `CampoScreen` sin cambios (sin regresión de UI).
- Regresiones: 40/40 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
