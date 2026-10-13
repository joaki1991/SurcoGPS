package es.surcogps.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Sesiones de trabajo: crear, reanudar (cuadrícula intacta) y terminar. */
@Composable
fun SesionScreen(vm: CampoViewModel) {
    val sesiones by vm.sesiones.collectAsState()
    val activa by vm.sesionActivaId.collectAsState()
    val activaNombre by vm.sesionActivaNombre.collectAsState()
    val ajustes by vm.ajustes.collectAsState()
    val ha by vm.areaHa.collectAsState()
    var aviso by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sesiones", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (activa == null) "Sin sesión activa."
            else "Activa: ${activaNombre ?: activa} · ${"%.2f".format(ha)} ha en curso",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (vm.nuevaSesion()) aviso = null
                    else aviso = "Fija primero el ancla en Campo."
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Nueva sesión")
            }
            OutlinedButton(
                onClick = { vm.guardarSesionActiva() },
                enabled = activa != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
            OutlinedButton(
                onClick = { vm.terminarSesion() },
                enabled = activa != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Terminar")
            }
        }
        aviso?.let { Text(it) }
        if (ajustes.anclaLat == null) Text("Fija el ancla en Campo para poder registrar.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sesiones, key = { it.id }) { s ->
                val esActiva = s.id == activa
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        s.name + if (esActiva) " (activa)" else "",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        (if (s.finished) "Terminada" else "En curso") +
                            " · ${"%.2f".format(s.cellCount / 10_000.0)} ha",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.reanudarSesion(s.id) }) { Text("Reanudar") }
                        OutlinedButton(onClick = { vm.borrarSesion(s.id) }) { Text("Borrar") }
                    }
                }
            }
        }
    }
}
