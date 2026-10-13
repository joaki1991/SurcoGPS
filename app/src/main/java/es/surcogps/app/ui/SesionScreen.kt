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

/** Estado de la sesión actual (registro GPS en el SPRINT-09, 2.º plano en el 11). */
@Composable
fun SesionScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sesión", style = MaterialTheme.typography.headlineSmall)
        Text("Sin sesión activa. El registro del recorrido y la superficie trabajada llegan en el Sprint 09.")
    }
}
