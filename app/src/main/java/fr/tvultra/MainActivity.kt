package fr.tvultra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private val Night = Color(0xFF080E1B)
private val Panel = Color(0xFF111C30)
private val Blue = Color(0xFF2589FF)
private val Muted = Color(0xFF9BAAC2)

private data class TvItem(
    val id: String,
    val title: String,
    val poster: String,
    val extension: String = "",
    val categoryId: String = "",
    val kind: String
)

private data class XtreamSession(val server: String, val username: String, val password: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvUltraApp() }
    }
}

@Composable
private fun TvUltraApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf("http://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<XtreamSession?>(null) }
    var section by remember { mutableStateOf("Accueil") }
    var live by remember { mutableStateOf<List<TvItem>>(emptyList()) }
    var movies by remember { mutableStateOf<List<TvItem>>(emptyList()) }
    var series by remember { mutableStateOf<List<TvItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Connecte-toi avec les identifiants fournis par ton fournisseur IPTV.") }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Night, contentColor = Color.White) {
            Row(Modifier.fillMaxSize().padding(24.dp)) {
                Column(Modifier.width(185.dp).fillMaxHeight()) {
                    Text("TV ULTRA", color = Blue, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(26.dp))
                    listOf("Accueil", "TV en direct", "Films", "Séries", "Connexion").forEach { item ->
                        Surface(
                            color = if ((item == "Connexion" && session == null) || (item == section && session != null)) Blue.copy(alpha = 0.22f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                if (item == "Connexion") section = "Connexion" else if (session != null) section = item
                            }.focusable()
                        ) {
                            Text(item, Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                                color = if (item == section || (item == "Connexion" && session == null)) Color.White else Muted,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("TVULTRA • version de départ", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.width(26.dp))
                Column(Modifier.fillMaxSize()) {
                    Text(
                        when { session == null || section == "Accueil" -> "Bienvenue sur TV Ultra"; section == "TV en direct" -> "Télévision en direct"; section == "Films" -> "Films"; section == "Séries" -> "Séries"; else -> "Connexion" },
                        fontSize = 29.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))
                    if (session == null || section == "Connexion") {
                        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 650.dp).fillMaxWidth()) {
                            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Connexion Xtream Codes", color = Blue, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(server, { server = it }, label = { Text("Adresse du serveur") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Blue, unfocusedLabelColor = Muted, focusedBorderColor = Blue, unfocusedBorderColor = Muted, cursorColor = Blue))
                                OutlinedTextField(username, { username = it }, label = { Text("Nom d’utilisateur") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Blue, unfocusedLabelColor = Muted, focusedBorderColor = Blue, unfocusedBorderColor = Muted, cursorColor = Blue))
                                OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Blue, unfocusedLabelColor = Muted, focusedBorderColor = Blue, unfocusedBorderColor = Muted, cursorColor = Blue))
                                Button(
                                    onClick = {
                                        if (server.isBlank() || username.isBlank() || password.isBlank()) {
                                            message = "Renseigne le serveur, le nom d’utilisateur et le mot de passe."
                                        } else {
                                            loading = true
                                            message = "Connexion au serveur en cours…"
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    runCatching { loadXtream(XtreamSession(server.trim(), username.trim(), password)) }
                                                }
                                                loading = false
                                                result.onSuccess { data ->
                                                    session = data.session
                                                    live = data.live
                                                    movies = data.movies
                                                    series = data.series
                                                    section = "Accueil"
                                                    message = "Connexion réussie • ${live.size} chaînes • ${movies.size} films • ${series.size} séries"
                                                }.onFailure {
                                                    message = "Connexion impossible : ${it.message ?: "vérifie l’adresse et les identifiants"}"
                                                }
                                            }
                                        }
                                    },
                                    enabled = !loading,
                                    modifier = Modifier.height(48.dp).focusable()
                                ) { Text(if (loading) "Connexion…" else "Se connecter") }
                            }
                        }
                    } else if (section == "Accueil") {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            item {
                                Text("Reprendre votre découverte", color = Muted, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Films et séries de votre abonnement", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            }
                            item {
                                Text("Derniers films", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                if (movies.isEmpty()) Text("Aucun film reçu pour le moment.", color = Muted)
                                else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(movies.take(12)) { item ->
                                        PosterCard(item = item, onClick = { section = "Films" })
                                    }
                                }
                            }
                            item {
                                Text("Dernières séries", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                if (series.isEmpty()) Text("Aucune série reçue pour le moment.", color = Muted)
                                else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(series.take(12)) { item ->
                                        PosterCard(item = item, onClick = { section = "Séries" })
                                    }
                                }
                            }
                            item {
                                Text("TV en direct", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                if (live.isEmpty()) Text("Aucune chaîne reçue pour le moment.", color = Muted)
                                else LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(live.take(12)) { item ->
                                        PosterCard(item = item, onClick = { section = "TV en direct" })
                                    }
                                }
                            }
                        }
                    } else {
                        val current = when (section) { "Films" -> movies; "Séries" -> series; else -> live }
                        if (current.isEmpty()) {
                            Text("Aucun contenu reçu pour cette rubrique.", color = Muted)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 14.dp)) {
                                items(current) { item ->
                                    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.width(170.dp).height(250.dp).clickable {
                                        val active = session
                                        if (active != null && item.kind != "series") {
                                            val path = if (item.kind == "live") "live" else "movie"
                                            val ext = if (item.kind == "live") "ts" else item.extension.ifBlank { "mp4" }
                                            val stream = "${active.server.trimEnd('/')}/$path/${active.username}/${active.password}/${item.id}.$ext"
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(stream)))
                                            }.onFailure {
                                                Toast.makeText(context, "Aucun lecteur vidéo compatible trouvé.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, if (item.kind == "series") "La navigation des épisodes sera ajoutée ensuite." else "Sélectionne un contenu.", Toast.LENGTH_SHORT).show()
                                        }
                                    }.focusable()) {
                                        Column {
                                            if (item.poster.isNotBlank()) {
                                                AsyncImage(model = item.poster, contentDescription = item.title, modifier = Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF1B2940)))
                                            } else {
                                                Box(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF1B2940)), contentAlignment = Alignment.Center) {
                                                    Text("TV ULTRA", color = Blue, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(item.title, Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                                        }
                                    }
                                }
                            }
                            Text("Sélectionne un contenu pour ouvrir la lecture. La navigation des épisodes de séries reste à ajouter.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class CatalogResult(
    val session: XtreamSession,
    val live: List<TvItem>,
    val movies: List<TvItem>,
    val series: List<TvItem>
)

private fun loadXtream(session: XtreamSession): CatalogResult {
    val base = session.server.trim().trimEnd('/')
    require(base.startsWith("http://") || base.startsWith("https://")) { "l’adresse doit commencer par http:// ou https://" }
    val query = "username=${enc(session.username)}&password=${enc(session.password)}"
    val auth = JSONObject(httpGet("$base/player_api.php?$query"))
    val userInfo = auth.optJSONObject("user_info")
    require(userInfo != null && userInfo.optString("auth") != "0") { "identifiants refusés par le serveur" }
    return CatalogResult(
        session,
        parseItemsArray(httpGet("$base/player_api.php?$query&action=get_live_streams"), "live"),
        parseItemsArray(httpGet("$base/player_api.php?$query&action=get_vod_streams"), "movie"),
        parseItemsArray(httpGet("$base/player_api.php?$query&action=get_series"), "series")
    )
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

private fun httpGet(address: String): String {
    val connection = (URL(address).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 15000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "TVUltra/0.1")
    }
    return try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("réponse HTTP $code")
        body
    } finally {
        connection.disconnect()
    }
}

private fun parseItemsArray(jsonText: String, kind: String): List<TvItem> {
    val array = JSONArray(jsonText)
    return (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index) ?: return@mapNotNull null
        val id = obj.optString(if (kind == "series") "series_id" else "stream_id", "")
        if (id.isBlank() || id == "null") return@mapNotNull null
        val title = obj.optString(if (kind == "series") "name" else "name", "Sans titre")
        val poster = when (kind) {
            "series" -> obj.optString("cover", "")
            "movie" -> obj.optString("stream_icon", "")
            else -> obj.optString("stream_icon", "")
        }
        TvItem(
            id = id,
            title = title.ifBlank { "Sans titre" },
            poster = poster.takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty(),
            extension = obj.optString("container_extension", "mp4"),
            categoryId = obj.optString("category_id", ""),
            kind = kind
        )
    }
}


@Composable
private fun PosterCard(item: TvItem, onClick: () -> Unit) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(145.dp).height(210.dp).clickable(onClick = onClick).focusable()
    ) {
        Column {
            if (item.poster.isNotBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(158.dp).background(Color(0xFF1B2940))
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(158.dp).background(Color(0xFF1B2940)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TV ULTRA", color = Blue, fontWeight = FontWeight.Bold)
                }
            }
            Text(item.title, Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
        }
    }
}
