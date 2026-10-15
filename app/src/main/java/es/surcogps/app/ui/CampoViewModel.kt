package es.surcogps.app.ui

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.SurcoApp
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.data.Ajustes
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.mapa.DestinoCamara
import es.surcogps.app.mapa.DiagnosticoTeselas
import es.surcogps.app.mapa.EstadoUbicacion
import es.surcogps.app.mapa.FuentesMapa
import es.surcogps.app.mapa.ModoMapa
import es.surcogps.app.mapa.SuavizadorRumbo
import es.surcogps.app.soporte.ChequeoFuente
import es.surcogps.app.soporte.CrashReporter
import es.surcogps.app.soporte.EstadoCache
import es.surcogps.app.soporte.InformeDiag
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.app.soporte.StatsFuente
import es.surcogps.app.soporte.zonaGruesa
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.Parcela
import es.surcogps.core.SessionSummary
import es.surcogps.core.WorkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.util.MapTileIndex
import java.io.File

/**
 * Fachada de UI sobre el [es.surcogps.app.motor.MotorCampo] (ámbito de app).
 * Solo conserva estado puramente visual (modo de mapa, seguir).
 */
class CampoViewModel(app: Application) : AndroidViewModel(app) {

    private val motor = (app as SurcoApp).motor

    val ajustes: StateFlow<Ajustes> = motor.ajustes
    val nmeaSentencias: StateFlow<Int> = motor.nmeaSentencias
    val guiado: StateFlow<EstadoGuiado> = motor.guiado
    val areaHa: StateFlow<Double> = motor.areaHa
    val tramosDibujo: StateFlow<List<TramoFranja>> = motor.tramosDibujo
    val registrando: StateFlow<Boolean> = motor.registrando
    val parcelas: StateFlow<List<Parcela>> = motor.parcelas
    val sesiones: StateFlow<List<SessionSummary>> = motor.sesiones
    val sesionActivaId: StateFlow<String?> = motor.sesionActivaId
    val sesionActivaNombre: StateFlow<String?> = motor.sesionActivaNombre
    val destinoCamara: StateFlow<DestinoCamara?> = motor.destinoCamara
    val guiaActual: StateFlow<GuidanceEngine?> = motor.guiaActual

    // Suavizado del rumbo de marcha para la flecha (con estado entre fixes).
    private val suavizador = SuavizadorRumbo()

    /** Indicador de posición para el mapa (null = sin fix). */
    val ubicacion: StateFlow<EstadoUbicacion?> = motor.guiado.map { g ->
        val fix = g.fix ?: return@map null
        if (!g.aceptado) return@map null
        val enMarcha = (fix.speedMps ?: 0f) > UMBRAL_MARCHA_MS
        EstadoUbicacion(
            lat = fix.position.lat,
            lon = fix.position.lon,
            precisionM = fix.accuracyM,
            rumboDeg = suavizador.actualizar(g.rumboVehiculoDeg, enMarcha)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Estado puramente visual (no pertenece al motor).
    private val _modoMapa = MutableStateFlow(ModoMapa.HIBRIDO)
    val modoMapa: StateFlow<ModoMapa> = _modoMapa.asStateFlow()

    private val _seguirPosicion = MutableStateFlow(false)
    val seguirPosicion: StateFlow<Boolean> = _seguirPosicion.asStateFlow()

    // Informe del último cierre inesperado (si existe), para compartirlo.
    private val _informeCierre = MutableStateFlow<String?>(null)
    val informeCierre: StateFlow<String?> = _informeCierre.asStateFlow()

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ultimo = CrashReporter.listar(getApplication<Application>().filesDir).firstOrNull()
            if (ultimo != null) _informeCierre.value = CrashReporter.leer(ultimo)
        }
    }

    fun descartarCierres() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            CrashReporter.borrarTodos(getApplication<Application>().filesDir)
        }
        _informeCierre.value = null
    }

    fun tienePermisoUbicacion(): Boolean = motor.tienePermisoUbicacion()
    fun iniciarGps() = motor.iniciarGps()
    fun detenerGps() = motor.detenerGps()
    fun cambiarModoMapa(modo: ModoMapa) {
        _modoMapa.value = modo
    }
    fun alternarSeguir() {
        _seguirPosicion.value = !_seguirPosicion.value
    }
    /** El usuario movió el mapa a mano: se deja de seguir (sin pelear la cámara). */
    fun alGestoManual() {
        _seguirPosicion.value = false
    }
    /** Botón «Mi ubicación»: centra en la posición actual con zoom de detalle. */
    fun irAUbicacion() {
        val p = motor.guiado.value.posicionApero
            ?: motor.guiado.value.fix?.position ?: return
        motor.solicitarDestino(p, es.surcogps.app.mapa.PoliticaEncuadre.ZOOM_DETALLE)
        _seguirPosicion.value = true
    }
    fun consumirDestino() {
        val d = destinoCamara.value ?: return
        motor.consumirDestino(d.id)
    }
    fun alternarRegistro() = motor.alternarRegistro()
    fun fijarAnclaAqui() = motor.fijarAnclaAqui()
    fun fijarAnclaEn(p: es.surcogps.core.LatLon) = motor.fijarAnclaEn(p)
    /** Toque en casa: ancla o rumbo. Con aviso (Toast) del resultado. */
    fun fijarPuntoMapa(p: es.surcogps.core.LatLon) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ctx = getApplication<Application>().applicationContext
            val msg = when (motor.fijarPuntoMapa(p)) {
                es.surcogps.app.motor.ResultadoToque.ANCLA_FIJADA -> "Ancla fijada: toca otro punto para el rumbo"
                es.surcogps.app.motor.ResultadoToque.RUMBO_FIJADO -> {
                    val a = motor.ajustes.value
                    "Rumbo ${"%.0f".format(a.rumboDeg)}° fijado"
                }
                es.surcogps.app.motor.ResultadoToque.IGNORADO -> "Demasiado cerca del ancla"
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun guardarRumbo(r: Double) = motor.guardarRumbo(r)
    fun guardarAncho(a: Double) = motor.guardarAncho(a)
    fun guardarShift(s: Double) = motor.guardarShift(s)
    fun guardarOffset(lat: Double, lon: Double) = motor.guardarOffset(lat, lon)
    fun guardarPuerta(p: Float) = motor.guardarPuerta(p)
    fun crearParcela(nombre: String): Boolean = motor.crearParcela(nombre)
    fun borrarParcela(id: String) = motor.borrarParcela(id)
    fun usarParcela(p: Parcela) = motor.usarParcela(p)
    fun fotoSesionActiva(): WorkSession? = motor.fotoSesionActiva()
    fun nuevaSesion(nombre: String? = null): Boolean = motor.nuevaSesion(nombre)
    fun reanudarSesion(id: String) = motor.reanudarSesion(id)
    fun guardarSesionActiva(terminada: Boolean = false) = motor.guardarSesionActiva(terminada)
    fun terminarSesion() = motor.terminarSesion()
    fun borrarSesion(id: String) = motor.borrarSesion(id)

    // ---- Diagnóstico autónomo (sin ADB; no toca el mapa) ----

    /** Foto del estado de diagnóstico para la pantalla. */
    data class EstadoDiagnostico(
        val permisoUbicacion: Boolean,
        val gpsActivo: Boolean,
        val ultimoFix: String?,
        val stats: StatsFuente?,
        val cache: EstadoCache?,
        val eventos: List<es.surcogps.app.soporte.EventoDiag>,
        val cierres: Int,
        val chequeos: List<ChequeoFuente>
    )

    private val _diag = MutableStateFlow<EstadoDiagnostico?>(null)
    val diagnostico: StateFlow<EstadoDiagnostico?> = _diag.asStateFlow()

    private val _chequeos = mutableListOf<ChequeoFuente>()

    private val _colaTeselas = MutableStateFlow(-1L)
    /** Última cola de teselas publicada por el mapa (para diagnóstico). */
    val colaTeselas: StateFlow<Long> = _colaTeselas.asStateFlow()

    fun informarCola(n: Long) {
        _colaTeselas.value = n
    }

    private fun dirDiag(): File = getApplication<Application>().filesDir

    /** Lee el estado actual (permisos, GPS, contadores osmdroid, caché, registros). */
    fun refrescarDiagnostico() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val permiso = tienePermisoUbicacion()
            var gps = false
            try {
                val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) {
            }
            val fix = motor.guiado.value.fix
            val ultimoFix = fix?.let {
                val haceS = (System.currentTimeMillis() - it.timeMs) / 1000
                "hace ${haceS}s · ±${"%.0f".format(it.accuracyM)} m · zona ${zonaGruesa(it.position.lat, it.position.lon)}"
            }
            val stats = try {
                StatsFuente(
                    Counters.tileDownloadErrors,
                    Counters.fileCacheHit,
                    Counters.fileCacheMiss,
                    Counters.fileCacheSaveErrors,
                    Counters.countOOM + Counters.fileCacheOOM,
                    _colaTeselas.value,
                    null
                )
            } catch (_: Exception) {
                null
            }
            val cache = medirCache()
            val eventos = RegistroDiag.ultimos(app.filesDir, 60)
            val cierres = try {
                CrashReporter.listar(app.filesDir).size
            } catch (_: Exception) {
                0
            }
            _diag.value = EstadoDiagnostico(
                permiso, gps, ultimoFix, stats, cache, eventos, cierres,
                _chequeos.toList()
            )
        }
    }

    /** Comprobación manual limitada: 3 teselas de muestra como máximo. */
    fun ejecutarDiagnostico() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            RegistroDiag.registrar(app.filesDir, "diag", "chequeo manual iniciado")
            val idx = MapTileIndex.getTileIndex(10, 500, 395)
            val pruebas = listOf(
                "Esri satélite" to FuentesMapa.esriImagery().getTileURLString(idx),
                "Esri referencia" to FuentesMapa.esriReference().getTileURLString(idx),
                "OSM Mapnik" to TileSourceFactory.MAPNIK.getTileURLString(
                    MapTileIndex.getTileIndex(6, 31, 24)
                )
            )
            _chequeos.clear()
            for ((nombre, url) in pruebas) {
                val r = DiagnosticoTeselas.comprobarDetalle(url)
                val c = ChequeoFuente(nombre, url, r.ok, r.codigo, r.error, r.ms)
                _chequeos.add(c)
                RegistroDiag.registrar(
                    app.filesDir, "diag",
                    "$nombre: ${if (r.ok) "OK" else "FALLO"} HTTP ${r.codigo ?: "?"} ${r.ms}ms ${r.error ?: ""}"
                )
            }
            RegistroDiag.registrar(app.filesDir, "diag", "chequeo manual terminado")
            refrescarDiagnostico()
        }
    }

    /** Genera el TXT del informe en caché (para compartir). Null si falla. */
    suspend fun exportarInforme(): File? = withContext(Dispatchers.IO) {
        try {
            val app = getApplication<Application>()
            val estado = _diag.value
            @Suppress("DEPRECATION")
            val pi = app.packageManager.getPackageInfo(app.packageName, 0)
            val version = "${pi.versionName} (${pi.versionCode})"
            val eventos = RegistroDiag.ultimos(app.filesDir, 120)
            val textosCierres = try {
                CrashReporter.listar(app.filesDir).take(3).map { CrashReporter.leer(it) }
            } catch (_: Exception) {
                emptyList()
            }
            val chequeos = _chequeos.toList()
            val stats = estado?.stats
            val cache = estado?.cache ?: medirCache()
            val texto = InformeDiag.construir(
                version, eventos, textosCierres, chequeos, stats, cache,
                tienePermisoUbicacion(), estado?.gpsActivo ?: false, estado?.ultimoFix
            )
            val f = File(app.cacheDir, "informe-surcogps.txt")
            f.writeText(texto, Charsets.UTF_8)
            RegistroDiag.registrar(app.filesDir, "diag", "informe exportado (${f.length()} B)")
            f
        } catch (_: Exception) {
            null
        }
    }

    /** Borra registros de diagnóstico y cierres (jamás parcelas/sesiones/ajustes). */
    fun limpiarRegistros() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            RegistroDiag.limpiar(app.filesDir)
            try {
                CrashReporter.borrarTodos(app.filesDir)
            } catch (_: Exception) {
            }
            _chequeos.clear()
            refrescarDiagnostico()
        }
    }

    private fun medirCache(): EstadoCache? {
        return try {
            val dir = Configuration.getInstance().osmdroidTileCache
            if (!dir.isDirectory) return EstadoCache(0, 0)
            var bytes = 0L
            var n = 0
            dir.walkTopDown().take(6000).forEach {
                if (it.isFile) {
                    bytes += it.length()
                    n++
                }
            }
            EstadoCache(bytes, n)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /** Velocidad mínima para considerar marcha fiable (m/s). */
        const val UMBRAL_MARCHA_MS = 0.8f
    }

    override fun onCleared() {
        // El tracking lo posee el motor+servicio: aquí solo se persiste la foto.
        guardarSesionActiva()
        super.onCleared()
    }
}
