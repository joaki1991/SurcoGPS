package es.surcogps.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.data.Ajustes
import es.surcogps.app.data.AjustesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado compartido de ajustes + resumen de campo (el mapa/GPS llegan en sprints 07/08). */
class CampoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AjustesRepository(app)

    val ajustes: StateFlow<Ajustes> = repo.ajustes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes()
    )

    fun guardarRumbo(r: Double) = viewModelScope.launch { repo.guardarRumbo(r) }
    fun guardarAncho(a: Double) = viewModelScope.launch { repo.guardarAncho(a) }
    fun guardarShift(s: Double) = viewModelScope.launch { repo.guardarShift(s) }
    fun guardarOffset(lat: Double, lon: Double) = viewModelScope.launch { repo.guardarOffset(lat, lon) }
    fun guardarPuerta(p: Float) = viewModelScope.launch { repo.guardarPuerta(p) }
}
