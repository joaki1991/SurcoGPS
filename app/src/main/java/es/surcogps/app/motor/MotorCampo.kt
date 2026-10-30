package es.surcogps.app.motor

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import es.surcogps.app.cobertura.CoberturaEngine
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.data.Ajustes
import es.surcogps.app.data.AjustesRepository
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.gps.FuenteGps
import es.surcogps.app.gps.GuiadoPipeline
import es.surcogps.app.gps.LocationTracker
import es.surcogps.app.gps.RumboSensor
import es.surcogps.app.mapa.DestinoCamara
import es.surcogps.app.mapa.EncuadreInicial
import es.surcogps.app.mapa.PoliticaEncuadre
import es.surcogps.app.servicio.TrackingService
import es.surcogps.app.ui.PasosAjuste
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.ImplementOffset
import es.surcogps.core.LatLon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Motor de campo mínimo (ámbito de aplicación).
 *
 * Hace solo lo que la app necesita: GPS + brújula, filtrado, cobertura en
 * memoria, plantilla de líneas y cámara. **No** carga sesiones, parcelas ni
 * coberturas antiguas al arrancar: la app siempre empieza limpia y los datos
 * que hubiera en disco se quedan intactos.
 */
class MotorCampo(private val app: Application) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repo = AjustesRepository(app)
    private val tracker = LocationTracker(app)
    private val brujula = RumboSensor(app)
    private val pipeline = GuiadoPipeline()
    private val encuadre = EncuadreInicial()

    val ajustes: StateFlow<Ajustes> = repo.ajustes.stateIn(
        alcance, SharingStarted.WhileSubscribed(5_000), Ajustes()
    )
    val fuenteGps: StateFlow<FuenteGps?> = tracker.fuente
    val rumboBrujula: StateFlow<Double?> = brujula.rumbo
    val brujulaDisponible: Boolean get() = brujula.disponible

    // ---- Ajustes de trabajo (en memoria para que la plantilla responda ya) ----
    private val _ancho = MutableStateFlow(6.0)
    val anchoApero: StateFlow<Double> = _ancho.asStateFlow()

    private val _rumbo = MutableStateFlow(0.0)
    val rumboGuia: StateFlow<Double> = _rumbo.asStateFlow()

    private val _ancla = MutableStateFlow<LatLon?>(null)
    val ancla: StateFlow<LatLon?> = _ancla.asStateFlow()

    private var guardadoAncho: Job? = null
    private var guardadoRumbo: Job? = null

    // ---- Cobertura (solo memoria; nunca se recarga del disco) ----
    private var engine: CoberturaEngine? = null

    private val _areaHa = MutableStateFlow(0.0)
    val areaHa: StateFlow<Double> = _areaHa.asStateFlow()

    private val _tramos = MutableStateFlow<List<TramoFranja>>(emptyList())
    val tramosDibujo: StateFlow<List<TramoFranja>> = _tramos.asStateFlow()

    private val _registrando = MutableStateFlow(true)
    val registrando: StateFlow<Boolean> = _registrando.asStateFlow()

    private val _gpsActivo = MutableStateFlow(false)
    val gpsActivo: StateFlow<Boolean> = _gpsActivo.asStateFlow()

    // ---- Cámara (comandos de un solo uso) ----
    private val _destinoCamara = MutableStateFlow<DestinoCamara?>(null)
    val destinoCamara: StateFlow<DestinoCamara?> = _destinoCamara.asStateFlow()
    private var siguienteDestinoId = 1L

    private var gpsIniciado = false
    private var encuadreHecho = false

    init {
        alcance.launch {
            val a = repo.ajustes.first()
            _ancho.value = PasosAjuste.redondearAncho(a.anchoAperoM)
            _rumbo.value = normalizar(a.rumboDeg)
        }
    }

    /** Pipeline Fix → filtro → posición del apero → XTE. */
    val guiado: StateFlow<EstadoGuiado> = combine(
        ajustes, tracker.fix, _ancla, _ancho, _rumbo
    ) { a, fix, ancla, ancho, rumbo ->
        if (fix == null) return@combine EstadoGuiado()
        pipeline.configurarPuerta(a.puertaPrecisionM)
        val guia = ancla?.let {
            GuidanceEngine(it, rumbo, ancho, lateralShiftM = 0.0)
        }
        pipeline.procesar(
            fix, System.currentTimeMillis(), guia,
            ImplementOffset(lateralM = 0.0, longitudinalM = 0.0)
        )
    }.stateIn(alcance, SharingStarted.WhileSubscribed(5_000), EstadoGuiado())

    /** Familia de líneas para el mapa (null si aún no hay ancla). */
    val guiaActual: StateFlow<GuidanceEngine?> = combine(
        _ancla, _ancho, _rumbo
    ) { ancla, ancho, rumbo ->
        ancla?.let { GuidanceEngine(it, rumbo, ancho, lateralShiftM = 0.0) }
    }.stateIn(alcance, SharingStarted.WhileSubscribed(5_000), null)

    /*
     * La colección se arranca DESPUÉS de inicializar `guiado`: en Kotlin las
     * propiedades se inicializan en orden de declaración y un `init` anterior
     * veía `guiado` como null (NPE al abrir la app, detectado en emulador).
     */
    init {
        alcance.launch { guiado.collect { onGuiado(it) } }
    }

    private fun onGuiado(e: EstadoGuiado) {
        val fix = e.fix ?: return
        // La declinación se recalcula con la posición, aunque el fix no sea
        // válido para pintar.
        brujula.actualizarPosicion(fix.position)
        if (!e.aceptado) return

        val p = e.posicionApero ?: fix.position

        // Primer fix válido de este arranque: ancla de la plantilla.
        if (_ancla.value == null) _ancla.value = p

        if (!encuadreHecho &&
            encuadre.alFix(true, fix.accuracyM, ajustes.value.puertaPrecisionM)
        ) {
            encuadreHecho = true
            solicitarDestino(p, PoliticaEncuadre.ZOOM_DETALLE)
        }

        val eng = engine ?: CoberturaEngine(p, _ancho.value).also { engine = it }
        if (!_registrando.value) return
        eng.registrar(p, _ancho.value)
        _areaHa.value = eng.areaHa
        // Solo se publica una lista nueva cuando ha crecido de verdad.
        val lista = eng.tramos
        if (lista.size != _tramos.value.size) _tramos.value = lista.toList()
    }

    fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Arranca GPS + brújula + servicio en primer plano. Idempotente. */
    fun iniciarGps() {
        if (!tienePermisoUbicacion() || gpsIniciado) return
        gpsIniciado = true
        pipeline.reiniciar()
        encuadre.reiniciar()
        encuadreHecho = false
        tracker.iniciar()
        _gpsActivo.value = true
        alcance.launch { repo.guardarTracking(true) }
        ContextCompat.startForegroundService(app, Intent(app, TrackingService::class.java))
    }

    /** Detiene GPS y servicio (la cobertura en pantalla se conserva). */
    fun detenerGps() {
        gpsIniciado = false
        tracker.detener()
        _gpsActivo.value = false
        alcance.launch { repo.guardarTracking(false) }
        app.stopService(Intent(app, TrackingService::class.java))
    }

    /** El servicio se está cerrando: suelta el tracker sin recursión. */
    fun detenerTrackerSolo() {
        gpsIniciado = false
        tracker.detener()
        _gpsActivo.value = false
    }

    fun iniciarBrujula() = brujula.iniciar()
    fun detenerBrujula() = brujula.detener()
    fun actualizarRotacionPantalla(rotacion: Int) =
        brujula.actualizarRotacionPantalla(rotacion)

    fun alternarRegistro() {
        if (_registrando.value) {
            engine?.pausar()
            _registrando.value = false
        } else {
            engine?.reanudar()
            _registrando.value = true
        }
    }

    /**
     * Nuevo recorrido: borra SOLO la cobertura en memoria y recoloca la
     * plantilla en la posición actual. Los ficheros de sesiones/parcelas del
     * dispositivo no se tocan.
     */
    fun reiniciarRecorrido() {
        val p = guiado.value.posicionApero ?: guiado.value.fix?.position
        engine?.reiniciar()
        engine = null
        _tramos.value = emptyList()
        _areaHa.value = 0.0
        pipeline.reiniciar()
        if (p != null) {
            _ancla.value = p
            engine = CoberturaEngine(p, _ancho.value)
        }
    }

    /** Recoloca la plantilla de líneas en la posición actual. */
    fun fijarAnclaAqui() {
        val p = guiado.value.posicionApero ?: guiado.value.fix?.position ?: return
        _ancla.value = p
    }

    /** Cambia el ancho con respuesta inmediata y guardado diferido. */
    fun actualizarAncho(anchoM: Double) {
        val v = PasosAjuste.redondearAncho(anchoM)
        if (v == _ancho.value) return
        _ancho.value = v
        guardadoAncho?.cancel()
        guardadoAncho = alcance.launch {
            delay(GUARDADO_DIFERIDO_MS)
            repo.guardarAncho(v)
        }
    }

    /** Cambia el rumbo de la plantilla con respuesta inmediata. */
    fun actualizarRumbo(rumboDeg: Double) {
        val v = normalizar(rumboDeg)
        if (v == _rumbo.value) return
        _rumbo.value = v
        guardadoRumbo?.cancel()
        guardadoRumbo = alcance.launch {
            delay(GUARDADO_DIFERIDO_MS)
            repo.guardarRumbo(v)
        }
    }

    /** Copia la orientación actual de la brújula al rumbo de la plantilla. */
    fun usarRumboBrujula(): Boolean {
        val r = brujula.rumbo.value ?: return false
        actualizarRumbo(r)
        return true
    }

    fun solicitarDestino(punto: LatLon, zoom: Double? = null) {
        _destinoCamara.value = DestinoCamara(siguienteDestinoId++, punto, zoom)
    }

    fun consumirDestino(id: Long) {
        if (_destinoCamara.value?.id == id) _destinoCamara.value = null
    }

    private fun normalizar(grados: Double): Double =
        ((grados % 360.0) + 360.0) % 360.0

    companion object {
        /** Espera para no escribir en DataStore en cada píxel del slider. */
        const val GUARDADO_DIFERIDO_MS = 300L
    }
}
