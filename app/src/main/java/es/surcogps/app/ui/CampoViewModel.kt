package es.surcogps.app.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.data.Ajustes
import es.surcogps.app.data.AjustesRepository
import es.surcogps.core.FileParcelaStore
import es.surcogps.core.FileSessionStore
import es.surcogps.core.Parcela
import es.surcogps.app.cobertura.CoberturaEngine
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.gps.GuiadoPipeline
import es.surcogps.app.gps.LocationTracker
import es.surcogps.app.mapa.ModoMapa
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.ImplementOffset
import es.surcogps.core.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado compartido: ajustes + GPS real + banderillero (el mapa llega en el Sprint 07...09). */
class CampoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AjustesRepository(app)
    private val tracker = LocationTracker(app)
    private val pipeline = GuiadoPipeline()

    val ajustes: StateFlow<Ajustes> = repo.ajustes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes()
    )

    private val _modoMapa = MutableStateFlow(ModoMapa.HIBRIDO)
    val modoMapa: StateFlow<ModoMapa> = _modoMapa.asStateFlow()

    private val _seguirPosicion = MutableStateFlow(false)
    val seguirPosicion: StateFlow<Boolean> = _seguirPosicion.asStateFlow()

    val nmeaSentencias: StateFlow<Int> = tracker.nmeaSentencias

    // ---- Cobertura de la sesión en curso (memoria; persistencia en Sprint 10) ----
    private var engine: CoberturaEngine? = null
    private var engineKey: String? = null
    private var autoAnclaHecha = false

    private val _areaHa = MutableStateFlow(0.0)
    val areaHa: StateFlow<Double> = _areaHa.asStateFlow()

    private val _tramos = MutableStateFlow<List<TramoFranja>>(emptyList())
    val tramosDibujo: StateFlow<List<TramoFranja>> = _tramos.asStateFlow()

    private val _registrando = MutableStateFlow(true)
    val registrando: StateFlow<Boolean> = _registrando.asStateFlow()

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

    init {
        viewModelScope.launch {
            guiado.collect { onGuiado(it) }
        }
    }

    private fun onGuiado(e: EstadoGuiado) {
        val apero = e.posicionApero ?: return
        if (!e.aceptado) return
        val a = ajustes.value
        // Sin ancla: se fija automáticamente en la primera posición válida para
        // que moverse con fix válido empiece a registrar de inmediato.
        if ((a.anclaLat == null || a.anclaLon == null) && !autoAnclaHecha) {
            autoAnclaHecha = true
            viewModelScope.launch { repo.guardarAncla(apero.lat, apero.lon) }
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

    /** Banderillero: combina ajustes + fix crudo a través del pipeline. */
    val guiado: StateFlow<EstadoGuiado> =
        combine(ajustes, tracker.fix) { a, fix ->
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoGuiado())

    fun tienePermisoUbicacion(): Boolean {
        val ctx = getApplication<Application>()
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Arranca el GPS real (llamar solo con permiso concedido). */
    fun iniciarGps() {
        pipeline.reiniciar()
        tracker.iniciar()
    }

    fun detenerGps() = tracker.detener()

    fun cambiarModoMapa(modo: ModoMapa) {
        _modoMapa.value = modo
    }

    fun alternarSeguir() {
        _seguirPosicion.value = !_seguirPosicion.value
    }

    /** Fija el ancla de la guía en la posición actual del apero. */
    fun fijarAnclaAqui() {
        val p = guiado.value.posicionApero ?: guiado.value.fix?.position ?: return
        viewModelScope.launch { repo.guardarAncla(p.lat, p.lon) }
    }

    fun guardarRumbo(r: Double) = viewModelScope.launch { repo.guardarRumbo(r) }
    fun guardarAncho(a: Double) = viewModelScope.launch { repo.guardarAncho(a) }
    fun guardarShift(s: Double) = viewModelScope.launch { repo.guardarShift(s) }
    fun guardarOffset(lat: Double, lon: Double) = viewModelScope.launch { repo.guardarOffset(lat, lon) }
    fun guardarPuerta(p: Float) = viewModelScope.launch { repo.guardarPuerta(p) }

    // ---- Parcelas y sesiones (ficheros locales vía :core; ver ADR-05) ----

    private val sesionesStore: FileSessionStore by lazy {
        FileSessionStore(java.io.File(getApplication<Application>().filesDir, "sesiones"))
    }
    private val parcelasStore: FileParcelaStore by lazy {
        FileParcelaStore(java.io.File(getApplication<Application>().filesDir, "parcelas"))
    }

    private val _parcelas = MutableStateFlow<List<Parcela>>(emptyList())
    val parcelas: StateFlow<List<Parcela>> = _parcelas.asStateFlow()

    private val _sesiones = MutableStateFlow<List<es.surcogps.core.SessionSummary>>(emptyList())
    val sesiones: StateFlow<List<es.surcogps.core.SessionSummary>> = _sesiones.asStateFlow()

    private val _sesionActivaId = MutableStateFlow<String?>(null)
    val sesionActivaId: StateFlow<String?> = _sesionActivaId.asStateFlow()

    private val _sesionActivaNombre = MutableStateFlow<String?>(null)
    val sesionActivaNombre: StateFlow<String?> = _sesionActivaNombre.asStateFlow()

    private var ultimoAutoguardadoMs = 0L

    private fun recargarListas() {
        _parcelas.value = parcelasStore.listar()
        _sesiones.value = sesionesStore.list()
    }

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { recargarListas() }
    }

    /** Crea una parcela con la configuración actual. Requiere ancla fijada. */
    fun crearParcela(nombre: String): Boolean {
        val a = ajustes.value
        if (a.anclaLat == null || a.anclaLon == null || nombre.isBlank()) return false
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            parcelasStore.guardar(
                Parcela(
                    id = java.util.UUID.randomUUID().toString(),
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

    fun borrarParcela(id: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        // Las sesiones son independientes (guardan su propia configuración).
        parcelasStore.borrar(id)
        recargarListas()
    }

    /** Carga la parcela en los ajustes (ancla + guía) para trabajar sobre ella. */
    fun usarParcela(p: Parcela) = viewModelScope.launch {
        repo.guardarAncla(p.ancla.lat, p.ancla.lon)
        repo.guardarRumbo(p.rumbo)
        repo.guardarAncho(p.ancho)
        repo.guardarShift(p.shift)
        repo.guardarOffset(p.offset.lateralM, p.offset.longitudinalM)
        autoAnclaHecha = true
    }

    /** Foto de la sesión en curso para persistir (null si no hay ancla). */
    fun fotoSesionActiva(): es.surcogps.core.WorkSession? {
        val a = ajustes.value
        val eng = engine ?: return null
        if (a.anclaLat == null || a.anclaLon == null) return null
        val ahora = System.currentTimeMillis()
        return es.surcogps.core.WorkSession(
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

    /** Inicia una sesión. Requiere ancla fijada. */
    fun nuevaSesion(nombre: String? = null): Boolean {
        val a = ajustes.value
        if (a.anclaLat == null || a.anclaLon == null) return false
        val ahora = System.currentTimeMillis()
        val id = java.util.UUID.randomUUID().toString()
        val sesion = es.surcogps.core.WorkSession(
            id = id,
            name = nombre ?: "Sesión ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ahora))}",
            anchor = LatLon(a.anclaLat, a.anclaLon),
            headingDeg = a.rumboDeg,
            widthM = a.anchoAperoM,
            lateralShiftM = a.desplazamientoLateralM,
            offset = ImplementOffset(a.offsetLateralM, a.offsetLongitudinalM),
            startedMs = ahora,
            updatedMs = ahora,
            finished = false
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            sesionesStore.save(sesion)
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

    /** Reanuda una sesión guardada: ajustes + cuadrícula + dibujo intactos. */
    fun reanudarSesion(id: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val s = sesionesStore.load(id) ?: return@launch
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
        _sesionActivaId.value = id
        _sesionActivaNombre.value = s.name
        _areaHa.value = eng.areaHa
        _tramos.value = eng.tramos.toList()
        pipeline.reiniciar()
    }

    /** Guarda la sesión activa (foto actual). */
    fun guardarSesionActiva(terminada: Boolean = false) {
        val id = _sesionActivaId.value ?: return
        val foto = fotoSesionActiva() ?: return
        ultimoAutoguardadoMs = System.currentTimeMillis()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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

    fun borrarSesion(id: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        if (_sesionActivaId.value == id) {
            _sesionActivaId.value = null
            _sesionActivaNombre.value = null
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

    override fun onCleared() {
        guardarSesionActiva()
        tracker.detener()
        super.onCleared()
    }

    companion object {
        /** Intervalo mínimo entre autoguardados (ms). */
        const val AUTOGUARDADO_MS = 15_000L
    }
}
