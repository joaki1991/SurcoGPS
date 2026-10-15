# SPRINT-42 — Caché de teselas en directorio garantizado
- Causa raíz (informe del propietario, v1.6.1): 0 errores de descarga + 2087 de
  guardado + sonda `ENOENT` en la carpeta de teselas → lo descargado se pierde
  y el mapa queda gris. El directorio por defecto de osmdroid no es escribible
  en ese dispositivo.
- Alcance: `resolverDirCache` puro + sonda real en el arranque del mapa (solo
  cambia a `<cacheDir>/teselas` si el defecto falla de verdad) + registro del
  cambio + `ResolverDirCacheTest`.
- No se borra nada (ni siquiera la carpeta defectuosa); sesiones y ajustes intactos.
- Aceptación: suite verde; build verde.
- Pruebas: `ResolverDirCacheTest` 2/2 + suite 74/74 + `assembleDebug`.
