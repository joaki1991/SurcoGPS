# SPRINT-48 — Ancho fino, pintado legible y GPS sin dependencia de Internet

## Petición del usuario
1. Poder ajustar el ancho del apero **de 0,1 en 0,1 m** (ahora va de 0,5 en 0,5).
2. Que la superficie trabajada se vea **más opaca**: el verde actual apenas se
   distingue sobre el satélite.
3. Que funcione **sin Internet**, usando el GPS del propio dispositivo, y que
   Internet sea solo una ayuda cuando esté disponible.

## Evidencia
- `AjustesScreen.kt`: `valueRange = 0.5f..30f, steps = 58` → 59 posiciones,
  incremento real de 0,5 m. El texto además muestra el valor persistido, no el
  que se está arrastrando.
- `FranjaOverlay.kt`: relleno `alpha = 110` y borde `alpha = 160`; con el verde
  0xFF2E7D32 sobre imagen satélite el contraste es bajo.
- `LocationTracker.kt`: única fuente `FusedLocationProviderClient`. En un móvil
  sin Google Play Services (o con los servicios desactivados) no se pide nunca
  ubicación al `LocationManager` del sistema: la app se queda «sin fix».
- `MapaCampo.kt`: al entrar en Híbrido/Satélite se lanza siempre una sonda HTTP
  a Esri aunque no haya red, con espera de hasta 8 s.

## Objetivo
Ancho ajustable a 0,1 m, superficie trabajada claramente visible y GPS operativo
sin Play Services ni datos móviles. Internet solo mejora las teselas.

## Alcance
- `ui/AjustesScreen.kt` + `ui/PasosAjuste.kt` (nuevo): rango 0,5–30 m con paso
  0,1 (`steps = 294`), redondeo a 1 decimal al guardar y valor en vivo durante
  el arrastre.
- `mapa/FranjaOverlay.kt`: relleno y borde mucho más opacos, sin cambiar colores
  ni geometría.
- `gps/FuenteGps.kt` (nuevo): decisión pura y testeable de fuente primaria y de
  activación del respaldo.
- `gps/LocationTracker.kt`: Fused solo si Play Services responde; si no,
  `LocationManager` (GPS + red) desde el primer segundo. Si Fused está pero no
  entrega fix en 20 s, se añade el GPS del sistema como respaldo. Se publica la
  fuente del último fix para diagnóstico y se registra en el log local.
- `mapa/MapaCampo.kt`: sin red no se lanza la sonda HTTP (fallo inmediato y
  aviso de caché).
- `motor/MotorCampo.kt`, `ui/CampoViewModel.kt`, `ui/PantallaDiagnostico.kt`:
  mostrar la fuente GPS activa en el diagnóstico autónomo.
- Tests JUnit5/JUnit4 de las funciones puras nuevas.

## Fuera de alcance
- Cambiar el formato de sesiones, parcelas o ajustes.
- Sustituir OSMDroid ni las fuentes de teselas.
- Descarga offline de mapas por zonas (no solicitada).
- Conducción automática (la app es un banderillero visual).

## Aceptación
- El slider permite 0,5; 0,6; …; 29,9; 30,0 m y persiste exactamente 1 decimal.
- El relleno de la franja es claramente visible sobre satélite (alfa ≥ 200).
- Con Play Services no disponible, el tracker pide ubicación al `LocationManager`
  sin esperar 20 s; con Play Services, si no hay fix en 20 s arranca el respaldo.
- Sin red, el mapa no espera 8 s de HTTP y mantiene el aviso de caché.
- Diagnóstico muestra la fuente GPS real del último fix.
- `:core:test` y `:app:testDebugUnitTest` verdes; debug y release compilan;
  APK v1.9.0 firmada y verificada.
- La verificación en móvil físico (fix real, campo, gestos) queda **pendiente**
  y así se declara.
