# SPRINT-09 — Registro y coloreado de cobertura
- Objetivo: la franja del apero se pinta en el mapa y suma hectáreas.
- Alcance: pipeline Fix→Filter→Offset→Coverage→overlay (polilíneas de franja),
  distinción trabajado/pendiente, contador ha en vivo, pausa/reanudar pasada.
- Aceptación: acercarse a guía sin moverse NO colorea; moverse con fix válido SÍ;
  ha UI == ha core.
- Pruebas: instrumentada/mock + test integrado.
