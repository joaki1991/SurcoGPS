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
import es.surcogps.app.gps.GuiadoPipeline
import es.surcogps.app.gps.LocationTracker
import es.surcogps.app.mapa.DestinoCamara
import es.surcogps.app.mapa.EncuadreInicial
import es.surcogps.app.mapa.PoliticaEncuadre
import es.surcogps.app.servicio.TrackingService
import es.surcogps.core.FileParcelaStore
import es.surcogps.core.FileSessionStore
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.ImplementOffset
import es.surcogps.core.LatLon
import es.surcogps.core.Parcela
import es.surcogps.core.SessionSummary
import es.surcogps.core.WorkSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Resultado de tocar el mapa para definir ancla/rumbo. */
enum class ResultadoToque { ANCLA_FIJADA, RUMBO_FIJADO, IGNORADO }

/**
 * Motor de campo en ámbito de aplicación: sobrevive a la actividad (pantalla
 * apagada, navegación entre pestañas). El servicio en primer plano lo mantiene
 * vivo; el ViewModel es una fachada delgada para la UI.
 */
class MotorCampo(private val app: Application) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repo = AjustesRepository(app)
    private val tracker = LocationTracker(app)
    private val pipeline = GuiadoPipeline()
    private val sesionesStore = FileSessionStore(File(app.filesDir, "sesiones"))
    private val parcelasStore = FileParcelaStore(File(app.filesDir, "parcelas"))

    val ajustes: StateFlow<Ajustes> = repo.ajustes.stateIn(
        alcance, SharingStarted.WhileSubscribed(5_000), Ajustes()
    )
    val nmeaSentencias: StateFlow<Int> = tracker.nmeaSentencias

    // ---- Cobertura ----
    private var engine: CoberturaEngine? = null
    private var engineKey: String? = null
    private var autoAnclaHecha = false

    private val _areaHa = MutableStateFlow(0.0)
    val areaHa: StateFlow<Double> = _areaHa.asStateFlow()

    private val _tramos = MutableStateFlow<List<TramoFranja>>(emptyList())
    val tramosDibujo: StateFlow<List<TramoFranja>> = _tramos.asStateFlow()

    private val _registrando = MutableStateFlow(true)
    val registrando: StateFlow<Boolean> = _registrando.asStateFlow()

    // ---- Cámara (comandos de un solo uso; el mapa los consume) ----
    private val _destinoCamara = MutableStateFlow<DestinoCamara?>(null)
    val destinoCamara: StateFlow<DestinoCamara?> = _destinoCamara.asStateFlow()
    private var siguienteDestinoId = 1L
    private val encuadre = EncuadreInicial()

    /** Encuadra el mapa una vez (centro + zoom opcional). */
    fun solicitarDestino(punto: LatLon, zoom: Double?) {
        _destinoCamara.value = DestinoCamara(siguienteDestinoId++, punto, zoom)
    }

    fun consumirDestino(id: Long) {
        if (_destinoCamara.value?.id == id) _destinoCamara.value = null
    }

    // ---- Sesiones ----
    private val _parcelas = MutableStateFlow<List<Parcela>>(emptyList())
    val parcelas: StateFlow<List<Parcela>> = _parcelas.asStateFlow()

    private val _sesiones = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sesiones: StateFlow<List<SessionSummary>> = _sesiones.asStateFlow()

    private val _sesionActivaId = MutableStateFlow<String?>(null)
    val sesionActivaId: StateFlow<String?> = _sesionActivaId.asStateFlow()

    private val _sesionActivaNombre = MutableStateFlow<String?>(null)
    val sesionActivaNombre: StateFlow<String?> = _sesionActivaNombre.asStateFlow()

    private var ultimoAutoguardadoMs = 0L

    /** Banderillero: ajustes + fix crudo a través del pipeline. */
    val guiado: StateFlow<EstadoGuiado> =        combine(ajustes, tracker.fix) { a, fix ->
            if (fix == null) return@combine EstadoGuiado()
            pipeline.configurarPuerta(a.puertaPrecisionM)
            val guia = if (a.anclaLat != null && a.anclaLon != null) {
                GuidanceEngine(
                    anchor = LatLon(a.anclaLat, a.anclaLon),
                    headingDeg = a.rumboDeg,
                    implementWidthM = a.anchoAperoM,
                    lateralShiftM = a.desplazamientoLateralM
                )
            } else null
            pipeline.procesar(
                fix, System.currentTimeMillis(), guia,
                ImplementOffset(a.offsetLateralM, a.offsetLongitudinalM)
            )
        }.stateIn(alcance, SharingStarted.WhileSubscribed(5_000), EstadoGuiado())

    init {
        alcance.launch(Dispatchers.IO) { recargarListas() }
        alcance.launch { guiado.collect { onGuiado(it) } }
    }

    private fun onGuiado(e: EstadoGuiado) {
        val apero = e.posicionApero ?: return
        if (!e.aceptado) return
        val a = ajustes.value
        // Encuadre inicial: primer fix válido → centrar + zoom de detalle (una vez).
        val fix = e.fix
        if (fix != null && encuadre.alFix(true, fix.accuracyM, a.puertaPrecisionM)) {
            solicitarDestino(apero, PoliticaEncuadre.ZOOM_DETALLE)
        }
        if ((a.anclaLat == null || a.anclaLon == null) && !autoAnclaHecha) {
            autoAnclaHecha = true
            alcance.launch { repo.guardarAncla(apero.lat, apero.lon) }
            return
        }
        val ancla = if (a.anclaLat != null && a.anclaLon != null) {
            LatLon(a.anclaLat, a.anclaLon)
        } else return
        if (!_registrando.value) return
        val key = "${ancla.lat},${ancla.lon}|${a.anchoAperoM}"
        if (engine == null || engineKey != key) {
            engine = CoberturaEngine(ancla, a.anchoAperoM)
            engineKey = key
        }
        engine!!.registrar(apero)
        _areaHa.value = engine!!.areaHa
        _tramos.value = engine!!.tramos.toList()
        autoguardarSiToca()
    }

    fun tienePermisoUbicacion(): Boolean {
        return ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Arranca GPS + servicio en primer plano (solo con permiso). */
    fun iniciarGps() {
        if (!tienePermisoUbicacion()) return
        pipeline.reiniciar()
        encuadre.reiniciar()
        tracker.iniciar()
        alcance.launch { repo.guardarTracking(true) }
        ContextCompat.startForegroundService(app, Intent(app, TrackingService::class.java))
    }

    /** Detiene GPS + servicio. */
    fun detenerGps() {
        detenerTrackerSolo()
        alcance.launch { repo.guardarTracking(false) }
        app.stopService(Intent(app, TrackingService::class.java))
    }

    /** Detiene el tracker sin tocar el servicio (uso interno del servicio). */
    fun detenerTrackerSolo() {
        tracker.detener()
    }

    /**
     * Reanuda tras muerte del proceso o reinicio: si el tracking quedó activo y
     * hay permiso, restaura la sesión y sigue registrando.
     */
    fun restaurarTrasMuerte() {
        alcance.launch(Dispatchers.IO) {
            val activo = repo.trackingActivo.first()
            val idSesion = repo.sesionActivaId.first()
            if (!Politicas.debeReanudarServicio(activo, tienePermisoUbicacion())) return@launch
            if (idSesion.isNotEmpty()) {
                sesionesStore.load(idSesion)?.let { s ->
                    aplicarSesion(s)
                    _sesionActivaId.value = idSesion
                    _sesionActivaNombre.value = s.name
                }
            }
            pipeline.reiniciar()
            encuadre.reiniciar()
            tracker.iniciar()
        }
    }

    fun alternarRegistro() {
        val eng = engine
        if (_registrando.value) {
            eng?.pausar()
            _registrando.value = false
        } else {
            eng?.reanudar()
            _registrando.value = true
        }
    }

    fun fijarAnclaAqui() {
        val p = guiado.value.posicionApero ?: guiado.value.fix?.position ?: return
        autoAnclaHecha = true
        alcance.launch {
            repo.guardarAncla(p.lat, p.lon)
            repo.borrarPuntoFin()
        }
    }

    /** Fija el ancla en un punto elegido en el mapa (long-press). */
    fun fijarAnclaEn(p: LatLon) {
        if (p.lat !in -90.0..90.0 || p.lon !in -180.0..180.0) return
        autoAnclaHecha = true
        alcance.launch {
            repo.guardarAncla(p.lat, p.lon)
            repo.borrarPuntoFin() // El ancla nueva invalida el fin anterior.
        }
    }

    /** Punto tocado en casa: sin ancla → la fija; con ancla → define el rumbo. */
    suspend fun fijarPuntoMapa(p: LatLon): ResultadoToque {
        if (p.lat !in -90.0..90.0 || p.lon !in -180.0..180.0) return ResultadoToque.IGNORADO
        val a = ajustes.value
        if (a.anclaLat == null || a.anclaLon == null) {
            repo.guardarAncla(p.lat, p.lon)
            autoAnclaHecha = true
            return ResultadoToque.ANCLA_FIJADA
        }
        val rumbo = es.surcogps.app.mapa.rumboDesdeToque(LatLon(a.anclaLat, a.anclaLon), p)
            ?: return ResultadoToque.IGNORADO
        repo.guardarRumbo(rumbo)
        repo.guardarPuntoFin(p.lat, p.lon)
        return ResultadoToque.RUMBO_FIJADO
    }

    /** Guía actual para dibujar las paralelas (null = sin ancla). */
    val guiaActual: StateFlow<GuidanceEngine?> = ajustes.map { a ->
        if (a.anclaLat != null && a.anclaLon != null) {
            GuidanceEngine(
                anchor = LatLon(a.anclaLat, a.anclaLon),
                headingDeg = a.rumboDeg,
                implementWidthM = a.anchoAperoM,
                lateralShiftM = a.desplazamientoLateralM
            )
        } else null
    }.stateIn(alcance, SharingStarted.WhileSubscribed(5_000), null)

    fun guardarRumbo(r: Double) = alcance.launch { repo.guardarRumbo(r) }
    fun guardarAncho(a: Double) = alcance.launch { repo.guardarAncho(a) }
    fun guardarShift(s: Double) = alcance.launch { repo.guardarShift(s) }
    fun guardarOffset(lat: Double, lon: Double) = alcance.launch { repo.guardarOffset(lat, lon) }
    fun guardarPuerta(p: Float) = alcance.launch { repo.guardarPuerta(p) }

    private fun recargarListas() {
        _parcelas.value = parcelasStore.listar()
        _sesiones.value = sesionesStore.list()
    }

    fun crearParcela(nombre: String): Boolean {
        val a = ajustes.value
        if (a.anclaLat == null || a.anclaLon == null || nombre.isBlank()) return false
        alcance.launch(Dispatchers.IO) {
            parcelasStore.guardar(
                Parcela(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    ancla = LatLon(a.anclaLat, a.anclaLon),
                    rumbo = a.rumboDeg,
                    ancho = a.anchoAperoM,
                    shift = a.desplazamientoLateralM,
                    offset = ImplementOffset(a.offsetLateralM, a.offsetLongitudinalM),
                    creadaMs = System.currentTimeMillis()
                )
            )
            recargarListas()
        }
        return true
    }

    fun borrarParcela(id: String) = alcance.launch(Dispatchers.IO) {
        parcelasStore.borrar(id)
        recargarListas()
    }

    fun usarParcela(p: Parcela) = alcance.launch {
        repo.guardarAncla(p.ancla.lat, p.ancla.lon)
        repo.guardarRumbo(p.rumbo)
        repo.guardarAncho(p.ancho)
        repo.guardarShift(p.shift)
        repo.guardarOffset(p.offset.lateralM, p.offset.longitudinalM)
        autoAnclaHecha = true
    }

    fun fotoSesionActiva(): WorkSession? {
        val a = ajustes.value
        val eng = engine ?: return null
        if (a.anclaLat == null || a.anclaLon == null) return null
        val ahora = System.currentTimeMillis()
        return WorkSession(
            id = _sesionActivaId.value ?: return null,
            name = _sesionActivaNombre.value ?: "Sesión en curso",
            anchor = LatLon(a.anclaLat, a.anclaLon),
            headingDeg = a.rumboDeg,
            widthM = a.anchoAperoM,
            lateralShiftM = a.desplazamientoLateralM,
            offset = ImplementOffset(a.offsetLateralM, a.offsetLongitudinalM),
            track = eng.snapshotTrack(),
            cells = eng.snapshotCells(),
            startedMs = ahora,
            updatedMs = ahora,
            finished = false
        )
    }

    fun nuevaSesion(nombre: String? = null): Boolean {
        val a = ajustes.value
        if (a.anclaLat == null || a.anclaLon == null) return false
        val ahora = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val sesion = WorkSession(
            id = id,
            name = nombre ?: "Sesión ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ahora))}",
            anchor = LatLon(a.anclaLat, a.anclaLon),
            headingDeg = a.rumboDeg,
            widthM = a.anchoAperoM,
            lateralShiftM = a.desplazamientoLateralM,
            offset = ImplementOffset(a.offsetLateralM, a.offsetLongitudinalM),
            startedMs = ahora,
            updatedMs = ahora,
            finished = false
        )
        alcance.launch(Dispatchers.IO) {
            sesionesStore.save(sesion)
            repo.guardarSesionActivaId(id)
            recargarListas()
        }
        _sesionActivaId.value = id
        _sesionActivaNombre.value = sesion.name
        engine = CoberturaEngine(sesion.anchor, sesion.widthM)
        engineKey = "${sesion.anchor.lat},${sesion.anchor.lon}|${sesion.widthM}"
        _areaHa.value = 0.0
        _tramos.value = emptyList()
        ultimoAutoguardadoMs = ahora
        return true
    }

    fun reanudarSesion(id: String) = alcance.launch(Dispatchers.IO) {
        val s = sesionesStore.load(id) ?: return@launch
        aplicarSesion(s)
        _sesionActivaId.value = id
        _sesionActivaNombre.value = s.name
        repo.guardarSesionActivaId(id)
        pipeline.reiniciar()
        encuadre.reiniciar()
        // Al reanudar se muestra el trabajo guardado con zoom de detalle.
        solicitarDestino(s.anchor, PoliticaEncuadre.ZOOM_DETALLE)
    }

    private suspend fun aplicarSesion(s: WorkSession) {
        repo.guardarAncla(s.anchor.lat, s.anchor.lon)
        repo.guardarRumbo(s.headingDeg)
        repo.guardarAncho(s.widthM)
        repo.guardarShift(s.lateralShiftM)
        repo.guardarOffset(s.offset.lateralM, s.offset.longitudinalM)
        autoAnclaHecha = true
        val eng = CoberturaEngine(s.anchor, s.widthM)
        eng.restaurar(s.cells, s.track, s.widthM)
        engine = eng
        engineKey = "${s.anchor.lat},${s.anchor.lon}|${s.widthM}"
        _areaHa.value = eng.areaHa
        _tramos.value = eng.tramos.toList()
    }

    fun guardarSesionActiva(terminada: Boolean = false) {
        val id = _sesionActivaId.value ?: return
        val foto = fotoSesionActiva() ?: return
        ultimoAutoguardadoMs = System.currentTimeMillis()
        alcance.launch(Dispatchers.IO) {
            val previa = sesionesStore.load(id)
            sesionesStore.save(
                foto.copy(
                    name = previa?.name ?: foto.name,
                    startedMs = previa?.startedMs ?: foto.startedMs,
                    updatedMs = ultimoAutoguardadoMs,
                    finished = terminada
                )
            )
            recargarListas()
        }
    }

    fun terminarSesion() = guardarSesionActiva(terminada = true)

    fun borrarSesion(id: String) = alcance.launch(Dispatchers.IO) {
        if (_sesionActivaId.value == id) {
            _sesionActivaId.value = null
            _sesionActivaNombre.value = null
            repo.guardarSesionActivaId("")
        }
        sesionesStore.delete(id)
        recargarListas()
    }

    private fun autoguardarSiToca() {
        if (_sesionActivaId.value == null) return
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoAutoguardadoMs < AUTOGUARDADO_MS) return
        guardarSesionActiva()
    }

    companion object {
        const val AUTOGUARDADO_MS = 15_000L
    }
}
