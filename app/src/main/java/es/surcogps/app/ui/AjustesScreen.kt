package es.surcogps.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ajustes reales persistidos en DataStore (rumbo, ancho, shift, offset, puerta). */
@Composable
fun AjustesScreen(vm: CampoViewModel) {
    val a by vm.ajustes.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Text("Rumbo de trabajo: ${"%.0f".format(a.rumboDeg)}° (0=N, 90=E, 180=S, 270=O)")
        Text(
            "Se define en Campo: ancla + punto final (toque largo). Aquí solo ajuste fino.",
            style = MaterialTheme.typography.bodySmall
        )
        var rumboUi by remember(a.rumboDeg) { mutableStateOf(a.rumboDeg.toFloat()) }
        Slider(value = rumboUi,
            onValueChange = { rumboUi = it },
            onValueChangeFinished = { vm.guardarRumbo(rumboUi.toDouble()) },
            valueRange = 0f..359f, steps = 358)

        Text("Ancho del apero: ${"%.1f".format(a.anchoAperoM)} m")
        var anchoUi by remember(a.anchoAperoM) { mutableStateOf(a.anchoAperoM.toFloat()) }
        Slider(value = anchoUi,
            onValueChange = { anchoUi = it },
            onValueChangeFinished = { vm.guardarAncho(anchoUi.toDouble()) },
            valueRange = 0.5f..30f, steps = 58)

        var shiftTxt by remember(a.desplazamientoLateralM) {
            mutableStateOf("%.1f".format(a.desplazamientoLateralM))
        }
        OutlinedTextField(
            value = shiftTxt,
            onValueChange = { t ->
                shiftTxt = t
                t.toDoubleOrNull()?.let { vm.guardarShift(it) }
            },
            label = { Text("Desplazamiento lateral (m)") },
            singleLine = true
        )

        var puertaTxt by remember(a.puertaPrecisionM) {
            mutableStateOf("%.0f".format(a.puertaPrecisionM))
        }
        OutlinedTextField(
            value = puertaTxt,
            onValueChange = { t ->
                puertaTxt = t
                t.toFloatOrNull()?.let { if (it in 1f..50f) vm.guardarPuerta(it) }
            },
            label = { Text("Puerta de precisión (m, 1–50)") },
            singleLine = true
        )
        Text("Brazo antena→apero: lateral ${"%.1f".format(a.offsetLateralM)} m, " +
            "longitudinal ${"%.1f".format(a.offsetLongitudinalM)} m (editable en Sprint 08).")
    }
}
