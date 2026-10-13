package es.surcogps.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import es.surcogps.app.ui.AjustesScreen
import es.surcogps.app.ui.CampoScreen
import es.surcogps.app.ui.CampoViewModel
import es.surcogps.app.ui.ParcelaScreen
import es.surcogps.app.ui.SesionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SurcoGPSApp()
            }
        }
    }
}

private data class Destino(val ruta: String, val etiqueta: String)

@Composable
fun SurcoGPSApp() {
    val nav = rememberNavController()
    val destinos = listOf(
        Destino("campo", "Campo"),
        Destino("parcela", "Parcela"),
        Destino("sesion", "Sesión"),
        Destino("ajustes", "Ajustes")
    )
    val iconos = mapOf(
        "campo" to Icons.Filled.Home,
        "parcela" to Icons.AutoMirrored.Filled.List,
        "sesion" to Icons.Filled.PlayArrow,
        "ajustes" to Icons.Filled.Settings
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val actual by nav.currentBackStackEntryAsState()
                val ruta = actual?.destination?.route
                destinos.forEach { d ->
                    NavigationBarItem(
                        selected = ruta == d.ruta,
                        onClick = { nav.navigate(d.ruta) { launchSingleTop = true } },
                        icon = { Icon(iconos[d.ruta]!!, contentDescription = d.etiqueta) },
                        label = { Text(d.etiqueta) }
                    )
                }
            }
        }
    ) { inner ->
        val vm: CampoViewModel = viewModel()
        NavHost(nav, startDestination = "campo", Modifier.padding(inner)) {
            composable("campo") { CampoScreen(vm) }
            composable("parcela") { ParcelaScreen() }
            composable("sesion") { SesionScreen() }
            composable("ajustes") { AjustesScreen(vm) }
        }
    }
}
