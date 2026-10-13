package es.surcogps.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla de campo: resumen de guía + estado. El mapa se integra en el SPRINT-07. */
@Composable
fun CampoScreen(vm: CampoViewModel) {
    val a by vm.ajustes.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Campo", style = MaterialTheme.typography.headlineSmall)
        Text("Rumbo: ${"%.0f".format(a.rumboDeg)}°  ·  Ancho: ${"%.1f".format(a.anchoAperoM)} m")
        Text("Shift lateral: ${"%.1f".format(a.desplazamientoLateralM)} m")
        Text("Puerta de precisión: ${"%.0f".format(a.puertaPrecisionM)} m (GPS de móvil: precisión métrica, no centimétrica)")
        Text("Mapa híbrido: se integra en el Sprint 07. Cobertura GPS: Sprint 08.")
        Spacer(Modifier.height(8.dp))
        Text("Ajuste rápido de rumbo")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.guardarRumbo(a.rumboDeg - 1) }, modifier = Modifier.weight(1f)) {
                Text("−1°")
            }
            OutlinedButton(onClick = { vm.guardarRumbo(a.rumboDeg + 1) }, modifier = Modifier.weight(1f)) {
                Text("+1°")
            }
        }
        Text("Ancho del apero: ${"%.1f".format(a.anchoAperoM)} m")
        Slider(
            value = a.anchoAperoM.toFloat(),
            onValueChange = { vm.guardarAncho(it.toDouble()) },
            valueRange = 0.5f..30f,
            steps = 58
        )
        Button(onClick = { vm.guardarShift(0.0) }, enabled = a.desplazamientoLateralM != 0.0) {
            Text("Centrar shift lateral")
        }
    }
}
