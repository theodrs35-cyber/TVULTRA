package fr.tvultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF080E1B)
private val Panel = Color(0xFF111C30)
private val Blue = Color(0xFF2589FF)
private val Muted = Color(0xFF9BAAC2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvUltraHome() }
    }
}

@Composable
fun TvUltraHome() {
    var selected by remember { mutableStateOf("Accueil") }
    val nav = listOf("Accueil", "TV en direct", "Guide TV", "Films", "Séries", "Replay")
    val groups = listOf("Sport", "Chaînes françaises", "Cinéma", "Divertissement")
    Surface(modifier = Modifier.fillMaxSize(), color = Night, contentColor = Color.White) {
        Row(Modifier.fillMaxSize().padding(28.dp)) {
            Column(Modifier.width(190.dp).fillMaxHeight()) {
                Text("TV ULTRA", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Blue)
                Spacer(Modifier.height(32.dp))
                nav.forEach { item ->
                    Surface(
                        color = if (selected == item) Blue.copy(alpha = 0.22f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(item, modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                            color = if (selected == item) Color.White else Muted,
                            fontSize = 15.sp, fontWeight = if (selected == item) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.fillMaxSize()) {
                Text("Bienvenue sur TV Ultra", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Votre télévision, au même endroit.", color = Muted, fontSize = 16.sp)
                Spacer(Modifier.height(24.dp))
                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(145.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.Center) {
                        Text("VOTRE UNIVERS TV", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("En direct • Films • Séries", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Connectez votre abonnement depuis les paramètres (à venir).", color = Muted, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Explorer les catégories", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(groups) { group ->
                        Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.width(170.dp).height(92.dp).focusable()) {
                            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterStart) {
                                Text(group, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Pour commencer", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Cette première version pose la base de l’interface. Les chaînes, le guide TV et les films apparaîtront après connexion à une playlist compatible.", color = Muted, fontSize = 15.sp)
            }
        }
    }
}
