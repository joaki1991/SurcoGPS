# SPRINT-22 — Teselas con diagnóstico + clip
- Objetivo: saber en pantalla si las teselas fallan por red o por render + evitar
  el sangrado sobre el menú.
- Alcance: `mapa/VigilanteTeselas.kt` (subclase que cuenta ok/fallo/caducada y
  avisa en hilo principal), `MapView(context, vigilante)` + proveedor vigilado
  para la referencia, píldora «▦ N ok · M fallo» (`textoTeselas`), UA con
  contacto, `clipToBounds` en el `AndroidView`, `TextoTeselasTest`.
- Aceptación: contadores vivos por proveedor (reinicio por modo); píldora visible;
  mapa recortado a su cuadro; suite verde.
- Pruebas: `TextoTeselasTest` + suite 41/41 + `assembleDebug`.
