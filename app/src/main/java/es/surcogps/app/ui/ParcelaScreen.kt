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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Parcelas guardadas en el dispositivo (Room). Sin cuentas ni nube. */
@Composable
fun ParcelaScreen(vm: CampoViewModel) {
    val parcelas by vm.parcelas.collectAsState()
    val ajustes by vm.ajustes.collectAsState()
    var nombre by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Parcelas", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre de la parcela") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (vm.crearParcela(nombre)) {
                    nombre = ""
                    aviso = null
                } else aviso = "Fija primero el ancla en Campo y escribe un nombre."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar parcela actual")
        }
        aviso?.let { Text(it) }
        Text(
            if (ajustes.anclaLat == null) "Sin ancla: ve a Campo y pulsa «Fijar ancla aquí»."
            else "Ancla: ${"%.6f".format(ajustes.anclaLat)}, ${"%.6f".format(ajustes.anclaLon)} · " +
                "Rumbo ${"%.0f".format(ajustes.rumboDeg)}° · ${"%.1f".format(ajustes.anchoAperoM)} m",
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(parcelas, key = { it.id }) { p ->
                Column(Modifier.fillMaxWidth()) {
                    Text(p.nombre, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Rumbo ${"%.0f".format(p.rumbo)}° · ${"%.1f".format(p.ancho)} m",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.usarParcela(p) }) { Text("Usar") }
                        OutlinedButton(onClick = { vm.borrarParcela(p.id) }) { Text("Borrar") }
                    }
                }
            }
        }
    }
}
