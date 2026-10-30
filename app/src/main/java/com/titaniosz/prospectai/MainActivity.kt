package com.titaniosz.prospectai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0B1020)
private val Surface = Color(0xFF151D31)
private val Accent = Color(0xFF65D6AD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProspectAIApp() }
    }
}

@Composable
fun ProspectAIApp() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, onPrimary = Color(0xFF06251B), background = Background, surface = Surface, onBackground = Color(0xFFF4F6FC), onSurface = Color(0xFFF4F6FC))) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Column(modifier = Modifier.padding(top = 34.dp)) {
                        Text("ProspectAI", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Encontre oportunidades\nperto de você.", fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Empresas que podem estar precisando de uma presença digital.", color = Color(0xFF9BA8C0), fontSize = 14.sp)
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("ÁREA DE BUSCA", color = Color(0xFF9BA8C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Raio de pesquisa", fontWeight = FontWeight.SemiBold)
                                    Text("Usar localização do aparelho", color = Color(0xFF9BA8C0), fontSize = 12.sp)
                                }
                                Text("30 km", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Iniciar pesquisa", fontWeight = FontWeight.Bold) }
                            Text("A busca será conectada à fonte de estabelecimentos na próxima etapa.", color = Color(0xFF9BA8C0), fontSize = 11.sp)
                        }
                    }
                }
                item {
                    Text("PAINEL", color = Color(0xFF9BA8C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("0", "Encontradas", Modifier.weight(1f))
                        MetricCard("0", "Sem site", Modifier.weight(1f))
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Como funciona", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("1. Localiza estabelecimentos em um raio de 30 km.\n2. Verifica se cada empresa informa um site.\n3. Organiza possíveis clientes para você analisar.", color = Color(0xFFB7C1D4), fontSize = 13.sp, lineHeight = 21.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, color = Accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF9BA8C0), fontSize = 12.sp)
        }
    }
}
