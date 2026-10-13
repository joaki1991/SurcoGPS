# Revisión — SPRINT-04 (revisor independiente)
- Alcance: `GpsFilter.kt` (+`Fix`, `FilterConfig`, `RejectReason`), `ImplementOffset.kt`,
  `GpsFilterTest.kt` (incluye `ImplementOffsetTest`).
- Aceptación: fix 25 m rechazado con puerta 10 — CUMPLE. Salto 500 m/1 s rechazado —
  CUMPLE. Offset lateral 2 m a 90° → −2 N (sur, derecha del este) — CUMPLE.
  Offset 0/180 verificado — CUMPLE.
- Reglas técnicas: precisión estimada obligatoria en el modelo (`accuracyM` no
  anulable); puertas configurables con defecto 10 m; sin promesas centimétricas — CUMPLE.
- Seguridad: sin E/S ni red; rechazos con motivo explícito (diagnóstico, no silencio) — OK.
- Calidad: orden caducidad→precisión→salto; el salto rechazado no contamina `last`;
  `reset()` para reanudación; offset acotado a 30 m; convención de signos documentada.
- Regresiones: 52/52 verde. Cambios ajenos: ninguno.
- Decisión: **PASS**.
