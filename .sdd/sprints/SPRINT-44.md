# SPRINT-44 — Mapa retenido + sonda SQL corregida
- Objetivo: no reconstruir el mapa al cambiar de pestaña; veredicto SQL honesto.
- Alcance: `mapa/MapaRetenido.kt` (MapView + proveedores + overlays + listeners
  una vez, con contexto de app; `onDetach` solo en `onCleared`), `MapaCampo`
  (usa el retenido; `DisposableEffect` solo pausa/reanuda), VM (retiene,
  suelta al morir, `veredictoSonda` en la sonda SQL con borrado previo),
  `SondaAlmacen.veredictoSonda` + test.
- Base en fuente osmdroid: `onPause/onResume` no tocan hilos; `onDetach` sí
  (verificado en `MapView.java`). Antes se destruía todo al salir de Campo.
- Aceptación: una sola creación por proceso (visible en el registro);
  veredicto SQL sin falsos positivos; suite verde.
- Pruebas: `veredictoSonda` en `SondaAlmacenTest` + suite 75/75 + `assembleDebug`.
