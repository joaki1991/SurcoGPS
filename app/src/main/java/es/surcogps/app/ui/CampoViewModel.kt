package es.surcogps.app.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.data.Ajustes
import es.surcogps.app.data.AjustesRepository
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

    override fun onCleared() {
        tracker.detener()
        super.onCleared()
    }
}
