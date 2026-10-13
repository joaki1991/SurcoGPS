package es.surcogps.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Gestión de parcelas y sesiones (Room + reanudación en el SPRINT-10). */
@Composable
fun ParcelaScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Parcelas y sesiones", style = MaterialTheme.typography.headlineSmall)
        Text("Aquí se crearán, guardarán y reanudarán las sesiones de trabajo (Sprint 10).")
        Text("El contrato de persistencia ya está definido y probado en :core (SessionStore).")
    }
}
