# Revisión — SPRINT-15 (revisor independiente)
- Alcance: `Camara.kt`, `MapaCampo.kt` (update + listener + params), motor/VM
  (destino), `CampoScreen` (cableado), `PoliticaCamaraTest`.
- Causas SPRINT-13 → corrección: (1) setTileSource por fix → guardia por modo —
  OK. (2) overlay recreado → instancia recordada — OK. (3) setCenter continuo →
  umbral 2 m + gesto manual desactiva — OK. (4) invalidate incondicional →
  condicional — OK. Fuentes Esri recordadas (sin instancia nueva por update) — OK.
- Híbrido conservado (`configPara` intacto); franja y líneas intactas; sin reducir
  frecuencia GPS (misma tasa, menos trabajo por update) — CUMPLE el requisito.
- Seguridad: sin cambios. Calidad: `EstadoMapaAplicado` fuera del snapshot de
  Compose (sin recomposiciones fantasma); callbacks con `rememberUpdatedState`.
- Regresiones: 24/24 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
