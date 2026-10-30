package es.surcogps.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.SurcoApp
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.data.Ajustes
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.gps.FuenteGps
import es.surcogps.app.mapa.DestinoCamara
import es.surcogps.app.mapa.EstadoGuia
import es.surcogps.app.mapa.EstadoUbicacion
import es.surcogps.app.mapa.MapaRetenido
import es.surcogps.app.mapa.ModoMapa
import es.surcogps.app.mapa.RumboFlecha
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Fachada mínima entre la pantalla única y el [es.surcogps.app.motor.MotorCampo].
 * Solo añade estado puramente visual (modo de mapa y seguimiento).
 */
class CampoViewModel(app: Application) : AndroidViewModel(app) {

    private val motor = (app as SurcoApp).motor

    /** Mapa retenido: sobrevive a la rotación de pantalla. */
    private val mapaLazy = lazy { MapaRetenido(getApplication()) }
    val mapa: MapaRetenido by mapaLazy

    val ajustes: StateFlow<Ajustes> = motor.ajustes
    val guiado: StateFlow<EstadoGuiado> = motor.guiado
    val ancho: StateFlow<Double> = motor.anchoApero
    val rumbo: StateFlow<Double> = motor.rumboGuia
    val ancla: StateFlow<LatLon?> = motor.ancla
    val areaHa: StateFlow<Double> = motor.areaHa
    val tramos: StateFlow<List<TramoFranja>> = motor.tramosDibujo
    val registrando: StateFlow<Boolean> = motor.registrando
    val gpsActivo: StateFlow<Boolean> = motor.gpsActivo
    val fuenteGps: StateFlow<FuenteGps?> = motor.fuenteGps
    val destinoCamara: StateFlow<DestinoCamara?> = motor.destinoCamara
    val guiaActual: StateFlow<GuidanceEngine?> = motor.guiaActual

    val brujulaDisponible: Boolean get() = motor.brujulaDisponible

    /**
     * Punto azul + flecha. El punto se muestra con cualquier fix (aunque el
     * filtro lo rechace para pintar) para no dejar al usuario sin referencia;
     * el cono de precisión dice honestamente cuánto fiarse.
     */
    val ubicacion: StateFlow<EstadoUbicacion?> = combine(
        motor.guiado, motor.rumboBrujula
    ) { g, brujula ->
        val fix = g.fix ?: return@combine null
        EstadoUbicacion(
            lat = fix.position.lat,
            lon = fix.position.lon,
            precisionM = fix.accuracyM,
            rumboDeg = RumboFlecha.resolver(
                brujulaDeg = brujula,
                rumboGpsDeg = g.rumboVehiculoDeg ?: fix.bearingDeg?.toDouble(),
                velocidadMps = fix.speedMps
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Plantilla lista para dibujar (solo si ya hay ancla). */
    val guia: StateFlow<EstadoGuia?> = combine(
        motor.guiaActual, motor.guiado, motor.ancla
    ) { engine, estado, ancla ->
        if (engine == null) return@combine null
        EstadoGuia(
            engine = engine,
            lineaActiva = estado.linea,
            centro = estado.fix?.position ?: ancla
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _modoMapa = MutableStateFlow(ModoMapa.SATELITE)
    val modoMapa: StateFlow<ModoMapa> = _modoMapa.asStateFlow()

    private val _seguirPosicion = MutableStateFlow(true)
    val seguirPosicion: StateFlow<Boolean> = _seguirPosicion.asStateFlow()

    fun tienePermisoUbicacion(): Boolean = motor.tienePermisoUbicacion()
    fun iniciarGps() = motor.iniciarGps()
    fun detenerGps() = motor.detenerGps()

    fun iniciarBrujula() = motor.iniciarBrujula()
    fun detenerBrujula() = motor.detenerBrujula()
    fun actualizarRotacionPantalla(rotacion: Int) =
        motor.actualizarRotacionPantalla(rotacion)

    fun cambiarModoMapa(modo: ModoMapa) {
        _modoMapa.value = modo
    }

    /** Alterna satélite/calles con un solo botón. */
    fun alternarModoMapa() {
        _modoMapa.value = if (_modoMapa.value == ModoMapa.CALLES) {
            ModoMapa.SATELITE
        } else {
            ModoMapa.CALLES
        }
    }

    fun alternarSeguir() {
        _seguirPosicion.value = !_seguirPosicion.value
    }

    /** El usuario movió el mapa a mano: se deja de seguir. */
    fun alGestoManual() {
        _seguirPosicion.value = false
    }

    /** Botón «Centrar»: vuelve a la posición y reactiva el seguimiento. */
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
    fun reiniciarRecorrido() = motor.reiniciarRecorrido()
    fun fijarAnclaAqui() = motor.fijarAnclaAqui()
    fun actualizarAncho(anchoM: Double) = motor.actualizarAncho(anchoM)
    fun actualizarRumbo(rumboDeg: Double) = motor.actualizarRumbo(rumboDeg)
    fun usarRumboBrujula(): Boolean = motor.usarRumboBrujula()

    override fun onCleared() {
        // Cerrar la app (back) detiene el GPS; girar la pantalla NO pasa por
        // aquí porque el ViewModel se conserva.
        motor.detenerGps()
        try {
            if (mapaLazy.isInitialized()) mapa.onDetach()
        } catch (_: Exception) {
        }
        super.onCleared()
    }
}
