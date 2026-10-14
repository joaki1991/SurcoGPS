# SPRINT-16 — Punto azul + precisión + rumbo
- Objetivo: indicador de ubicación estilo navegación, sin brújula.
- Alcance: `mapa/Suavizado.kt` (`SuavizadorRumbo`, media exponencial vectorial),
  `mapa/UbicacionOverlay.kt` (cono de precisión + punto azul + flecha, capa única),
  `EstadoUbicacion`, cableado en `MapaCampo`/`CampoScreen`/VM (flujo `ubicacion`,
  umbral de marcha 0,8 m/s).
- Decisión documentada: rumbo = RUMBO DE MARCHA (desplazamiento GPS suavizado),
  NO brújula del dispositivo (exige calibración y confunde orientación con
  dirección; el tractor necesita la marcha). Parado o sin velocidad fiable =
  sin flecha (no se inventa orientación).
- Aceptación: punto siempre con fix válido; cono = precisión estimada; flecha
  solo en marcha y sin saltos; capa única (no se duplica); norte arriba.
- Pruebas: `SuavizadorRumboTest` 7/7 + `assembleDebug`. Render visual: pendiente
  de dispositivo.
