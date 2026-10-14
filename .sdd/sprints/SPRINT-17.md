# SPRINT-17 — Zoom automático + botón ubicación + v1.1.0
- Objetivo: encuadre inicial automático al máximo zoom útil + botón «Mi ubicación».
- Alcance: `mapa/Encuadre.kt` (`PoliticaEncuadre.ZOOM_DETALLE=19`,
  `EncuadreInicial` one-shot), motor (encuadre en `onGuiado` con puerta de
  precisión, `reiniciar` en arranque/reanudación, destino en `reanudarSesion`),
  VM (`irAUbicacion` + seguir), `MapaCampo` (botón «Mi ubicación», sustituye a
  «Mi zona»), `CampoScreen` (estado «Buscando GPS…»), `versionCode 2 / 1.1.0`.
- Aceptación: primer fix válido centra + zoom 19 (una vez; impreciso no dispara);
  siguientes fixes no tocan cámara; gestos manuales respetados (S15); botón
  reencuadra + sigue; pérdida de señal conserva última posición (sin comandos);
  zoom recortado al máximo del proveedor; release v1.1.0 verificado.
- Pruebas: `EncuadreTest` 4/4 + suites + `assembleDebug/Release` + aapt/apksigner.
