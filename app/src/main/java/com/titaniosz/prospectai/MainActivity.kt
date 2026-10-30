package com.titaniosz.prospectai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

private val Background = Color(0xFF0B1020)
private val Panel = Color(0xFF151D31)
private val Accent = Color(0xFF65D6AD)
private val Muted = Color(0xFF9BA8C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProspectAIApp(this) }
    }
}

@Composable
fun ProspectAIApp(activity: MainActivity) {
    var leads by remember { mutableStateOf<List<BusinessLead>>(emptyList()) }
    var status by remember { mutableStateOf("Pronto para pesquisar em um raio de 30 km.") }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun locateAndSearch() {
        loading = true
        status = "Obtendo sua localização..."
        val token = CancellationTokenSource()
        try {
            LocationServices.getFusedLocationProviderClient(activity)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                .addOnSuccessListener { location ->
                    if (location == null) {
                        loading = false
                        status = "Não foi possível obter a localização. Ative a localização do aparelho e tente novamente."
                    } else {
                        status = "Pesquisando estabelecimentos em até 30 km..."
                        activity.lifecycleScope.launch {
                            try {
                                val found = OverpassRepository.findBusinesses(location.latitude, location.longitude)
                                leads = found
                                searched = true
                                val missing = found.count { !it.hasWebsite }
                                status = if (found.isEmpty()) "Nenhum estabelecimento foi retornado. Tente novamente mais tarde."
                                    else "Busca concluída: ${found.size} estabelecimentos; ${missing} sem site informado no OpenStreetMap."
                            } catch (error: Exception) {
                                status = error.message ?: "Não foi possível concluir a pesquisa. Verifique sua conexão."
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
                .addOnFailureListener { error ->
                    loading = false
                    status = error.localizedMessage ?: "Falha ao obter localização."
                }
        } catch (_: SecurityException) {
            loading = false
            status = "Permissão de localização necessária."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) locateAndSearch()
        else status = "A permissão de localização foi negada. Autorize para pesquisar perto de você."
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Color(0xFF06251B), background = Background,
        surface = Panel, onBackground = Color(0xFFF4F6FC), onSurface = Color(0xFFF4F6FC)
    )) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(Modifier.padding(top = 30.dp)) {
                        Text("ProspectAI", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text("Encontre oportunidades\nperto de você.", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text("Empresas que podem precisar de presença digital.", color = Muted, fontSize = 13.sp)
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("ÁREA DE BUSCA", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Localização do aparelho", fontWeight = FontWeight.SemiBold)
                                    Text("Raio fixo de pesquisa", color = Muted, fontSize = 12.sp)
                                }
                                Text("30 km", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(15.dp))
                            Button(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) locateAndSearch()
                                    else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF06251B))
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF06251B))
                                else Text("Iniciar pesquisa", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(status, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
                item {
                    Text("PAINEL", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(leads.size.toString(), "Encontradas", Modifier.weight(1f))
                        MetricCard(leads.count { !it.hasWebsite }.toString(), "Sem site informado", Modifier.weight(1f))
                    }
                }
                if (searched && leads.isEmpty()) item { Text("Nenhum resultado para exibir.", color = Muted) }
                if (leads.isNotEmpty()) {
                    item {
                        Text("POSSÍVEIS OPORTUNIDADES", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("A ausência de site no OpenStreetMap não confirma que a empresa não tenha site.", color = Muted, fontSize = 11.sp)
                    }
                    items(leads.filter { !it.hasWebsite }, key = { it.id }) { LeadCard(it) }
                    item { Text("OUTROS ESTABELECIMENTOS COM SITE INFORMADO", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    items(leads.filter { it.hasWebsite }, key = { it.id }) { LeadCard(it) }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Fonte de dados", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("Dados do OpenStreetMap via Overpass API. A cobertura varia conforme a região e os dados cadastrados pela comunidade. © OpenStreetMap contributors.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun LeadCard(lead: BusinessLead) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(3.dp))
            Text(lead.category, color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(9.dp))
            if (lead.hasWebsite) {
                Text("Site: ${lead.website}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = Color(0xFFFF4D5E),
                        content = {}
                    )
                    Text("Possível oportunidade", color = Muted, fontSize = 11.sp)
                }
            }
            Text("Endereço: ${lead.address}", color = Muted, fontSize = 11.sp)
            Text("Telefone: ${lead.phone ?: "Não informado"}", color = Muted, fontSize = 11.sp)
            Text("Coordenadas: %.5f, %.5f".format(lead.latitude, lead.longitude), color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(value, color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 11.sp)
        }
    }
}
