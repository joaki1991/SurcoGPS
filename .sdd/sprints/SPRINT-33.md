# SPRINT-33 — Rotación del mapa
- Objetivo: girar la orientación como cualquier app de mapas, con flecha correcta.
- Alcance: `RotationGestureOverlay` persistente, `anguloFlecha` puro,
  `UbicacionOverlay` compensado por `mapOrientation`, firma con orientación,
  `AnguloFlechaTest`.
- Aceptación: gesto de dos dedos rota; flecha = marcha − orientación, siempre en
  rango; resto de capas usan la proyección (rotación automática); suite verde.
- Pruebas: `AnguloFlechaTest` 3/3 + suite 59/59 + `assembleDebug`. Gesto real:
  pendiente de dispositivo.
