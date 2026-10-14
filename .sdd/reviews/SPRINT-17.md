# Revisión — SPRINT-17 (revisor independiente)
- Alcance: `Encuadre.kt`, motor (encuadre + destinos), VM (`irAUbicacion`),
  `MapaCampo` (botón), `CampoScreen` (carga), versión 1.1.0, tests, APK.
- Criterios del encargo (1–6): (1) primer fix válido centra + zoom 19 — CUMPLE
  (one-shot con puerta). (2) siguientes fixes no tocan cámara — CUMPLE (flag).
  (3) gestos manuales libres — CUMPLE (S15 + sin comandos posteriores).
  (4) botón reencuadra con detalle — CUMPLE. (5) sin parpadeos ni pérdida de
  líneas (usa infra S15) — CUMPLE. (6) tests distinguen primer fix de
  posteriores — CUMPLE (`EncuadreTest`).
- Sin regresiones: el botón «Mi zona» se sustituye por «Mi ubicación» (mejora
  pedida, no función rota); 35/35 + `:core` intacto; APKs verificados.
- Seguridad: sin cambios. Cambios ajenos: ninguno.
- Decisión: **PASS**.
