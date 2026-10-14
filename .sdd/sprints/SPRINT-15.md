# SPRINT-15 — Estabilidad del mapa (anti-parpadeo)
- Objetivo: eliminar el parpadeo causado por re-aplicar fuentes/capas/cámara en cada fix GPS.
- Alcance: `mapa/Camara.kt` (`DestinoCamara`, `PoliticaCamara`), `MapaCampo.kt`
  (fuentes recordadas, `EstadoMapaAplicado`, `MapListener` de gesto, seguimiento
  con umbral 2 m, `invalidate` solo con cambios), motor (`destinoCamara`,
  solicitar/consumir), VM (reexponer + `alGestoManual`), `CampoScreen` (cableado).
- Tareas: recordar fuente Esri + overlay; guardia por modo; comandos one-shot;
  seguimiento con umbral; gesto manual desactiva seguir; tests de política.
- Aceptación: ningún `setTileSource`/`removeAll`/recreación de proveedor por fix;
  cámara quieta sin comando ni desplazamiento real; gesto manual respetado.
- Pruebas: `PoliticaCamaraTest` 4/4 + `assembleDebug`. Parpadeo visual: pendiente
  de dispositivo (se declara; la causa se eliminó por construcción).
