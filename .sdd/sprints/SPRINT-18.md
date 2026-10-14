# SPRINT-18 — Diagnóstico con captura real + diseño estilo Traccar

## 1. Síntomas (captura del dispositivo, v1.1.0)
- **A. Teselas sin cargar**: cuadrícula gris vacía en modo HÍBRIDO (con WiFi activo
  según la barra de estado). Atribución visible (texto estático, no prueba carga).
- **B. Chips de modo ausentes**: no se ven Híbrido/Satélite/Calles (y «a veces»:
  intermitente) → sin forma de cambiar de base en el campo.
- **C. Zoom −/+ flotando a media pantalla** (controles integrados de osmdroid
  centrados en un MapView mal dimensionado) en vez de abajo-derecha.
- **D. Punto azul bajo el mapa**: casi seguro indicador táctil del sistema (nuestros
  overlays solo dibujan DENTRO del MapView); se reevaluará tras el rediseño.
- HUD, botones, slider y navegación funcionan (GPS: ±5 m, XTE vivo, NMEA 2996).

## 2. Investigación Traccar (código abierto, verificado)
- `traccar-manager-android` actual: `MainFragment` extiende `WebViewFragment` —
  el mapa es el cliente web (MapLibre) en **WebView a pantalla completa**.
- Patrón extraído (aplicable a osmdroid, sin reescribir a WebView):
  1. Superficie creada y configurada EXACTAMENTE UNA VEZ (`onViewCreated`:
     settings, clientes, listeners; `loadPage` una vez).
  2. Cambios posteriores solo por APIs estrechas (`evaluateJavascript` puntuales).
  3. Contenedor determinista (`match_parent`, sin pesos anidados ambiguos).
  4. Ciclo de vida explícito ligado al fragmento.
- Nuestro S15 ya aplica 1–2 a fuentes/cámara; falta aplicarlo al LAYOUT (pesos
  `fillMaxSize().weight()` anidados) y a los controles (chips/controles deben
  vivir DENTRO del cuadro del mapa, como los controles del visor Traccar).

## 3. Causa probable de A (teselas)
Sin dispositivo no se puede distinguir «sin red hacia Esri» de «fallo de
render». El rediseño añade **diagnóstico real**: comprobación HTTP de una tesela
de muestra por modo + píldora de estado («Satélite sin conexión: usa Calles»),
sin conmutación mágica (respeta la elección del usuario). URLs ya verificadas
por tests en Sprint 07.

## 4. Diseño SPRINT-19 (`MapaSurco`, sustituye el interior de `MapaCampo`)
- Cuadro de mapa determinista: `Box(weight 1f)` único, `AndroidView(fillMaxSize)`;
  SIN `fillMaxSize().weight()` anidados.
- Dentro del cuadro (siempre visibles): chips de modo (arriba, con fondo),
  píldora de estado de teselas, botones +/− y FAB «Mi ubicación» (abajo-derecha),
  banner offline. Atribución: `CopyrightOverlay` de osmdroid DENTRO del mapa
  (se elimina la fila externa que robaba espacio y se descolocaba).
- MapView: `setBuiltInZoomControls(false)`; fuentes + 4 overlays (referencia,
  franja, ubicación, copyright) creados UNA vez y nunca recreados; updates
  mínimos (S15 intacto).
- Lógica pura testeable: `resolverFuente(modo, esriOk): EstadoFuente`
  (base + referencia + aviso).

## 5. SPRINT-20 — QA + v1.2.0 (`versionCode 3`): suites, builds, aapt/apksigner,
  `releases/SurcoGPS-v1.2.0-release.apk`, informe.

## Decisión: PASS (documentación + diseño; implementación en SPRINT-19).
