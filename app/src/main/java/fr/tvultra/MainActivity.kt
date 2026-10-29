package fr.tvultra

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private val Bg = Color(0xFF070B16)
private val Panel = Color(0xFF121D31)
private val Blue = Color(0xFF1769FF)
private val Cyan = Color(0xFF35D9FF)
private val Muted = Color(0xFFB0BCD0)
private data class TvItem(val id:String,val title:String,val poster:String,val extension:String="",val categoryId:String="",val kind:String,val rating:String="",val archive:Boolean=false,val archiveDuration:Int=0)
private data class EpgProgram(val title:String,val start:String,val end:String,val description:String,val startTimestamp:Long,val durationMinutes:Int)
private data class TvCategory(val id:String,val name:String)
private data class XtreamSession(val server:String,val username:String,val password:String)
private data class CatalogResult(val session:XtreamSession,val live:List<TvItem>,val movies:List<TvItem>,val series:List<TvItem>,val liveCats:List<TvCategory>,val movieCats:List<TvCategory>,val seriesCats:List<TvCategory>)

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{TvUltraApp()}}}

@Composable private fun TvUltraApp(){
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var server by remember{mutableStateOf("http://")}
 var username by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 var session by remember{mutableStateOf<XtreamSession?>(null)}
 var section by remember{mutableStateOf("Accueil")}
 var live by remember{mutableStateOf<List<TvItem>>(emptyList())}
 var movies by remember{mutableStateOf<List<TvItem>>(emptyList())}
 var series by remember{mutableStateOf<List<TvItem>>(emptyList())}
 var liveCats by remember{mutableStateOf<List<TvCategory>>(emptyList())}
 var movieCats by remember{mutableStateOf<List<TvCategory>>(emptyList())}
 var seriesCats by remember{mutableStateOf<List<TvCategory>>(emptyList())}
 var category by remember{mutableStateOf("")}
 var selectedSeries by remember{mutableStateOf<TvItem?>(null)}
 var episodes by remember{mutableStateOf<List<TvItem>>(emptyList())}
 var playing by remember{mutableStateOf<TvItem?>(null)}
 var favorites by remember{mutableStateOf(setOf<String>())}
 var search by remember{mutableStateOf("")}
 var loading by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("Connecte-toi avec les identifiants de ton fournisseur IPTV.")}
 var selectedLive by remember{mutableStateOf<TvItem?>(null)}
 var epg by remember{mutableStateOf<List<EpgProgram>>(emptyList())}
 var epgLoading by remember{mutableStateOf(false)}
 val prefs = remember { context.getSharedPreferences("tvultra_ui", android.content.Context.MODE_PRIVATE) }
 var hiddenGroups by remember { mutableStateOf(prefs.getStringSet("hidden_live_groups", emptySet())?.toSet() ?: emptySet()) }
 var groupOrder by remember { mutableStateOf(prefs.getString("live_group_order", "")!!.split("|").filter { it.isNotBlank() }) }
 MaterialTheme{
  Surface(Modifier.fillMaxSize(),color=Bg,contentColor=Color.White){
   Row(Modifier.fillMaxSize().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)){
    Column(Modifier.width(170.dp).fillMaxHeight()){
     Text("TV ULTRA",color=Cyan,fontSize=26.sp,fontWeight=FontWeight.Black)
     Text("CINÉMA • SÉRIES • LIVE",color=Muted,fontSize=9.sp)
     Spacer(Modifier.height(24.dp))
     listOf("Accueil","TV en direct","Guide TV","Films","Séries","Favoris","Recherche","Paramètres","Connexion").forEach{label->
      Surface(color=if(section==label)Blue else Color.Transparent,shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{if(label=="Connexion"||session!=null){section=label;category="";if(label!="Séries")selectedSeries=null}}.focusable()){
       Text(label,Modifier.padding(horizontal=13.dp,vertical=12.dp),color=if(section==label)Color.White else Muted,fontSize=14.sp,fontWeight=if(section==label)FontWeight.Bold else FontWeight.Medium)
      }
     }
     Spacer(Modifier.weight(1f))
     Text(if(session!=null)"Catalogue connecté" else "Fire TV • TV Ultra",color=Muted,fontSize=10.sp)
    }
    Column(Modifier.fillMaxSize()){
     if(section!="Lecteur"){
      Row(verticalAlignment=Alignment.CenterVertically){
       Column(Modifier.weight(1f)){
        Text(when(section){"Accueil"->"Votre univers, en un clic";"TV en direct"->"Télévision en direct";"Films"->"Tous vos films";"Séries"->selectedSeries?.title?:"Vos séries";"Favoris"->"Vos favoris";"Recherche"->"Rechercher";"Guide TV"->"Guide TV • programmes et replay";"Paramètres"->"Personnaliser TV Ultra";else->"Connexion au service"},fontSize=26.sp,fontWeight=FontWeight.Bold)
        Text(if(session==null)message else live.size.toString()+" chaînes • "+movies.size+" films • "+series.size+" séries",color=Muted,fontSize=12.sp)
       }
       if(session!=null)Text("● CONNECTÉ",color=Cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
      }
      Spacer(Modifier.height(14.dp))
     }
     when{
      session==null||section=="Connexion"->Surface(color=Panel,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().widthIn(max=650.dp)){
       Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Connexion Xtream Codes",color=Cyan,fontSize=20.sp,fontWeight=FontWeight.Bold)
        OutlinedTextField(server,{server=it},label={Text("Adresse du serveur")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fieldColors())
        OutlinedTextField(username,{username=it},label={Text("Nom d’utilisateur")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fieldColors())
        OutlinedTextField(password,{password=it},label={Text("Mot de passe")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),colors=fieldColors())
        Button(onClick={
         if(server.isBlank()||username.isBlank()||password.isBlank())message="Renseigne les trois champs."
         else{loading=true;message="Connexion et chargement du catalogue…";scope.launch{
          val result=withContext(Dispatchers.IO){runCatching{loadXtream(XtreamSession(server.trim(),username.trim(),password))}}
          loading=false
          result.onSuccess{d->session=d.session;live=d.live;movies=d.movies;series=d.series;liveCats=d.liveCats;movieCats=d.movieCats;seriesCats=d.seriesCats;section="Accueil";message="Connexion réussie"}
           .onFailure{message="Connexion impossible : "+(it.message?:"vérifie les identifiants")}
         }}
        },enabled=!loading,modifier=Modifier.height(48.dp).focusable()){Text(if(loading)"Chargement…" else "Se connecter")}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Cyan)
        Text(message,color=Muted,fontSize=12.sp)
       }
      }
      section=="Lecteur"->{
       val item=playing
       val active=session
       if(item!=null&&active!=null)PlayerScreen(item,active) else Text("Aucun contenu sélectionné.",color=Muted)
      }
      section=="Accueil"->LazyColumn(verticalArrangement=Arrangement.spacedBy(20.dp)){
       item{
        val hero=movies.firstOrNull()?:series.firstOrNull()
        Box(Modifier.fillMaxWidth().height(275.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Blue,Panel,Bg)))){
         if(hero!=null&&hero.poster.isNotBlank())AsyncImage(model=hero.poster,contentDescription=hero.title,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
         Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg.copy(alpha=.97f),Bg.copy(alpha=.7f),Color.Transparent))))
         Column(Modifier.align(Alignment.CenterStart).padding(22.dp).widthIn(max=450.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
          Text("À LA UNE",color=Cyan,fontWeight=FontWeight.Bold,fontSize=12.sp)
          Text(hero?.title?:"Bienvenue sur TV Ultra",fontSize=30.sp,fontWeight=FontWeight.Black,maxLines=2)
          Text("Films, séries et télévision en direct réunis au même endroit.",color=Muted,fontSize=13.sp)
          Button(onClick={if(hero!=null){if(hero.kind=="series"){selectedSeries=hero;section="Séries";loadEpisodes(hero,session,scope){episodes=it}}else{playing=hero;section="Lecteur"}}},colors=ButtonDefaults.buttonColors(containerColor=Blue)){Text("▶  Lire")}
         }
        }
       }
       item{CatalogSection("Films récemment ajoutés",movies.takeLast(20),favorites,{favorites=favorites.toggle(it)},{playing=it;section="Lecteur"})}
       item{CatalogSection("Séries TV récemment ajoutées",series.takeLast(20),favorites,{favorites=favorites.toggle(it)},{selectedSeries=it;episodes=emptyList();section="Séries";loadEpisodes(it,session,scope){episodes=it}})}
       item{CatalogSection("Télévision en direct",live.take(20),favorites,{favorites=favorites.toggle(it)},{playing=it;section="Lecteur"})}
      }
      section=="TV en direct"->{CategoryChips(liveCats.filter{!hiddenGroups.contains(it.id)}.sortedBy { groupOrder.indexOf(it.id).let { n -> if(n<0) Int.MAX_VALUE else n } },category){category=it};Spacer(Modifier.height(10.dp));CatalogRows(live.filter{(category.isBlank()||it.categoryId==category)&&!hiddenGroups.contains(it.categoryId)},favorites,{favorites=favorites.toggle(it)},{playing=it;section="Lecteur"})}
      section=="Guide TV"->{
       Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
        LazyColumn(Modifier.width(300.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(live.filter{!hiddenGroups.contains(it.categoryId)},key={key(it)}){channel->Surface(color=if(selectedLive?.id==channel.id)Blue else Panel,shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth().clickable{selectedLive=channel;epgLoading=true;scope.launch{val result=withContext(Dispatchers.IO){runCatching{loadEpg(session!!,channel.id)}};epg=result.getOrDefault(emptyList());epgLoading=false}}.focusable()){Text(channel.title,Modifier.padding(10.dp),fontSize=12.sp,maxLines=2)}}}
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
         Text(selectedLive?.title ?: "Choisis une chaîne",fontSize=20.sp,fontWeight=FontWeight.Bold)
         if(epgLoading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Cyan)
         if(selectedLive==null)Text("Sélectionne une chaîne à gauche pour voir le programme en cours et les émissions suivantes.",color=Muted)
         else if(epg.isEmpty())Text("Aucun guide disponible pour cette chaîne. Ton fournisseur ne transmet peut-être pas les données EPG.",color=Muted)
         LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(epg){program->Surface(color=Panel,shape=RoundedCornerShape(10.dp)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(program.start+" – "+program.end,color=Cyan,fontSize=11.sp);Text(program.title,fontSize=15.sp,fontWeight=FontWeight.Bold);if(program.description.isNotBlank())Text(program.description,color=Muted,fontSize=12.sp);if(selectedLive?.archive==true&&program.startTimestamp>0)Button(onClick={val ch=selectedLive!!;val active=session!!;val start=java.time.Instant.ofEpochSecond(program.startTimestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm"));playing=ch.copy(id=ch.id,kind="replay",extension="ts");section="Lecteur";replayUrlOverride=active.server.trimEnd("/")+"/timeshift/"+enc(active.username)+"/"+enc(active.password)+"/"+program.durationMinutes+"/"+start+"/"+ch.id+".ts"}){Text("▶ Replay")}}}}}
        }
       }
      }
      section=="Paramètres"->{
       LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Personnaliser les groupes de chaînes",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Masque les groupes inutiles et déplace-les avec les flèches. Les réglages sont conservés sur cet appareil.",color=Muted,fontSize=12.sp)}
        items(liveCats.sortedBy { groupOrder.indexOf(it.id).let { n -> if(n<0) Int.MAX_VALUE else n } },key={it.id}){group->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Checkbox(checked=!hiddenGroups.contains(group.id),onCheckedChange={checked->hiddenGroups=if(checked)hiddenGroups-group.id else hiddenGroups+group.id;prefs.edit().putStringSet("hidden_live_groups",hiddenGroups).apply()});Text(group.name,Modifier.weight(1f),fontSize=13.sp);Text("↑",Modifier.clickable{val current=(groupOrder.filter{it in liveCats.map{c->c.id}}+liveCats.map{it.id}.filter{it !in groupOrder}).toMutableList();val i=current.indexOf(group.id);if(i>0){val t=current[i-1];current[i-1]=current[i];current[i]=t;groupOrder=current;prefs.edit().putString("live_group_order",current.joinToString("|")).apply()}}.padding(8.dp).focusable(),color=Cyan,fontSize=20.sp);Text("↓",Modifier.clickable{val current=(groupOrder.filter{it in liveCats.map{c->c.id}}+liveCats.map{it.id}.filter{it !in groupOrder}).toMutableList();val i=current.indexOf(group.id);if(i>=0&&i<current.lastIndex){val t=current[i+1];current[i+1]=current[i];current[i]=t;groupOrder=current;prefs.edit().putString("live_group_order",current.joinToString("|")).apply()}}.padding(8.dp).focusable(),color=Cyan,fontSize=20.sp)}}
        item{Text("Replay et EPG dépendent des fonctions activées par ton fournisseur IPTV.",color=Muted,fontSize=12.sp)}
       }
      }
      section=="Films"->{CategoryChips(movieCats,category){category=it};Spacer(Modifier.height(10.dp));CatalogRows(movies.filter{(category.isBlank()||it.categoryId==category)&&it.title.contains(search,true)},favorites,{favorites=favorites.toggle(it)},{playing=it;section="Lecteur"})}
      section=="Séries"->{
       val chosen=selectedSeries
       if(chosen!=null){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onClick={selectedSeries=null;episodes=emptyList()}){Text("← Toutes les séries")};Text(chosen.title,fontSize=20.sp,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(10.dp))
        if(episodes.isEmpty())Text("Aucun épisode retourné ou chargement en cours.",color=Muted)
        CatalogRows(episodes,favorites,{favorites=favorites.toggle(it)},{playing=it;section="Lecteur"})
       }else{CategoryChips(seriesCats,category){category=it};Spacer(Modifier.height(10.dp));CatalogRows(series.filter{category.isBlank()||it.categoryId==category},favorites,{favorites=favorites.toggle(it)},{selectedSeries=it;episodes=emptyList();loadEpisodes(it,session,scope){episodes=it}})}
      }
      section=="Favoris"->CatalogRows((live+movies+series).filter{favorites.contains(key(it))},favorites,{favorites=favorites.toggle(it)},{item->if(item.kind=="series"){selectedSeries=item;section="Séries";loadEpisodes(item,session,scope){episodes=it}}else{playing=item;section="Lecteur"}})
      section=="Recherche"->{
       OutlinedTextField(search,{search=it},label={Text("Titre d’un film, d’une série ou d’une chaîne")},modifier=Modifier.fillMaxWidth(),singleLine=true,colors=fieldColors())
       Spacer(Modifier.height(10.dp))
       CatalogRows((live+movies+series).filter{it.title.contains(search,true)},favorites,{favorites=favorites.toggle(it)},{item->if(item.kind=="series"){selectedSeries=item;section="Séries";loadEpisodes(item,session,scope){episodes=it}}else{playing=item;section="Lecteur"}})
      }
     }
    }
   }
  }
 }
}

@Composable private fun fieldColors()=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedLabelColor=Cyan,unfocusedLabelColor=Muted,focusedBorderColor=Cyan,unfocusedBorderColor=Muted,cursorColor=Cyan)
@Composable private fun CategoryChips(categories:List<TvCategory>,selected:String,onSelect:(String)->Unit){
 LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){item{Chip("Toutes",selected.isBlank()){onSelect("")}};items(categories){c->Chip(c.name,selected==c.id){onSelect(c.id)}}}
}
@Composable private fun Chip(name:String,active:Boolean,onClick:()->Unit){Surface(color=if(active)Blue else Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.clickable(onClick=onClick).focusable()){Text(name,Modifier.padding(horizontal=14.dp,vertical=9.dp),fontSize=12.sp,color=Color.White)}}
@Composable private fun CatalogSection(title:String,list:List<TvItem>,favorites:Set<String>,onFavorite:(String)->Unit,onOpen:(TvItem)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title+" ("+list.size+")",fontSize=19.sp,fontWeight=FontWeight.Bold)
  if(list.isEmpty())Text("Aucun contenu reçu pour cette rubrique.",color=Muted,fontSize=12.sp)
  else LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(list,key={key(it)}){item->PosterCard(item,favorites.contains(key(item)),{onFavorite(key(item))},{onOpen(item)})}}
 }
}
@Composable private fun CatalogRows(list:List<TvItem>,favorites:Set<String>,onFavorite:(String)->Unit,onOpen:(TvItem)->Unit){
 if(list.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Aucun contenu dans cette rubrique.",color=Muted)}
 else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){items(list,key={key(it)}){item->
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).clickable{onOpen(item)}.focusable().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   if(item.poster.isNotBlank())AsyncImage(model=item.poster,contentDescription=item.title,modifier=Modifier.size(78.dp,96.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop)
   else Box(Modifier.size(78.dp,96.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF24324A)),contentAlignment=Alignment.Center){Text("TV",color=Cyan,fontWeight=FontWeight.Black)}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(item.title,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2);Text(when(item.kind){"live"->"Télévision en direct";"movie"->"Film";"series"->"Série";else->"Épisode"},color=Muted,fontSize=12.sp);Text("Sélectionner pour ouvrir",color=Cyan,fontSize=11.sp)}
   Text(if(favorites.contains(key(item)))"♥" else "♡",color=Cyan,fontSize=23.sp,modifier=Modifier.clickable{onFavorite(key(item))}.padding(10.dp).focusable())
  }
 }}
}
@Composable private fun PosterCard(item:TvItem,favorite:Boolean,onFavorite:()->Unit,onClick:()->Unit){
 Column(Modifier.width(145.dp).clickable(onClick=onClick).focusable(),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Panel)){
   if(item.poster.isNotBlank())AsyncImage(model=item.poster,contentDescription=item.title,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
   else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("TV ULTRA",color=Cyan,fontWeight=FontWeight.Black)}
   if(item.rating.isNotBlank())Text(item.rating,Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(12.dp)).background(Blue).padding(horizontal=7.dp,vertical=4.dp),fontSize=10.sp,fontWeight=FontWeight.Bold)
   Text(if(favorite)"♥" else "♡",Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(20.dp)).background(Bg.copy(alpha=.85f)).clickable{onFavorite()}.padding(horizontal=8.dp,vertical=4.dp).focusable(),color=Cyan,fontSize=16.sp)
  }
  Text(item.title,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
 }
}
@Composable private fun PlayerScreen(item:TvItem,session:XtreamSession){
 val context=LocalContext.current
 val url=if(item.kind=="replay") replayUrlOverride ?: streamUrl(session,item) else streamUrl(session,item)
 val exo=remember(url){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(Uri.parse(url)));prepare();playWhenReady=true}}
 DisposableEffect(exo){onDispose{exo.release()}}
 Column(Modifier.fillMaxSize().background(Color.Black)){Text(item.title,Modifier.padding(12.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
  AndroidView(modifier=Modifier.fillMaxWidth().weight(1f),factory={ctx->PlayerView(ctx).apply{player=exo;useController=true}},update={it.player=exo})
 }
}
private fun loadEpisodes(item:TvItem,active:XtreamSession?,scope:kotlinx.coroutines.CoroutineScope,onLoaded:(List<TvItem>)->Unit){
 if(active==null)return
 scope.launch{val result=withContext(Dispatchers.IO){runCatching{seriesEpisodes(active,item.id)}};onLoaded(result.getOrDefault(emptyList()))}
}
private fun seriesEpisodes(s:XtreamSession,id:String):List<TvItem>{
 val q="username="+enc(s.username)+"&password="+enc(s.password)+"&action=get_series_info&series_id="+enc(id)
 val obj=JSONObject(httpGet(s.server.trimEnd('/')+"/player_api.php?"+q))
 val seasons=obj.optJSONObject("episodes")?:return emptyList()
 val out=mutableListOf<TvItem>();val keys=seasons.keys()
 while(keys.hasNext()){val season=keys.next();val arr=seasons.optJSONArray(season)?:continue
  for(i in 0 until arr.length()){val ep=arr.optJSONObject(i)?:continue;val epId=ep.optString("id","");if(epId.isBlank()||epId=="null")continue
   out+=TvItem(epId,"Saison "+season+" • Épisode "+ep.optString("episode_num",(i+1).toString())+" • "+ep.optString("title","Épisode"),ep.optJSONObject("info")?.optString("movie_image","").orEmpty(),ep.optString("container_extension","mp4"),kind="episode")
  }
 }
 return out
}
private var replayUrlOverride:String?=null
private fun streamUrl(s:XtreamSession,item:TvItem):String{
 val type=when(item.kind){"live"->"live";"series","episode"->"series";else->"movie"}
 val ext=if(type=="live")"ts" else item.extension.ifBlank{"mp4"}
 return s.server.trimEnd('/')+"/"+type+"/"+enc(s.username)+"/"+enc(s.password)+"/"+item.id+"."+ext
}
private fun loadXtream(s:XtreamSession):CatalogResult{
 val base=s.server.trim().trimEnd('/');require(base.startsWith("http://")||base.startsWith("https://")){"l’adresse doit commencer par http:// ou https://"}
 val q="username="+enc(s.username)+"&password="+enc(s.password)
 val auth=JSONObject(httpGet(base+"/player_api.php?"+q));val user=auth.optJSONObject("user_info")
 require(user!=null&&user.optString("auth")!="0"){"identifiants refusés par le serveur"}
 val live=parseItems(httpGet(base+"/player_api.php?"+q+"&action=get_live_streams"),"live")
 val movies=parseItems(httpGet(base+"/player_api.php?"+q+"&action=get_vod_streams"),"movie")
 val series=parseItems(httpGet(base+"/player_api.php?"+q+"&action=get_series"),"series")
 val lc=runCatching{parseCategories(httpGet(base+"/player_api.php?"+q+"&action=get_live_categories"))}.getOrDefault(emptyList())
 val mc=runCatching{parseCategories(httpGet(base+"/player_api.php?"+q+"&action=get_vod_categories"))}.getOrDefault(emptyList())
 val sc=runCatching{parseCategories(httpGet(base+"/player_api.php?"+q+"&action=get_series_categories"))}.getOrDefault(emptyList())
 return CatalogResult(s,live,movies,series,lc,mc,sc)
}
private fun loadEpg(s:XtreamSession,streamId:String):List<EpgProgram>{
 val q="username="+enc(s.username)+"&password="+enc(s.password)+"&action=get_short_epg&stream_id="+enc(streamId)+"&limit=20"
 val obj=JSONArray(httpGet(s.server.trimEnd('/')+"/player_api.php?"+q));return (0 until obj.length()).mapNotNull{i->val p=obj.optJSONObject(i)?:return@mapNotNull null;val ts=p.optString("start_timestamp","0").toLongOrNull()?:0L;val duration=((p.optString("stop_timestamp","0").toLongOrNull()?:ts)-ts).coerceAtLeast(0)/60;EpgProgram(p.optString("title","Programme"),p.optString("start",""),p.optString("end",""),p.optString("description",""),ts,duration.toInt().coerceAtLeast(1))}
}
private fun parseCategories(text:String):List<TvCategory>{
 val a=JSONArray(text);return (0 until a.length()).mapNotNull{i->val o=a.optJSONObject(i)?:return@mapNotNull null;val id=o.optString("category_id","");if(id.isBlank()||id=="null")null else TvCategory(id,o.optString("category_name","Autres"))}
}
private fun parseItems(text:String,kind:String):List<TvItem>{
 val a=JSONArray(text);return (0 until a.length()).mapNotNull{i->
  val o=a.optJSONObject(i)?:return@mapNotNull null
  val id=o.optString(if(kind=="series")"series_id" else "stream_id","")
  if(id.isBlank()||id=="null")return@mapNotNull null
  val poster=if(kind=="series")o.optString("cover","") else o.optString("stream_icon","")
  TvItem(id,o.optString("name","Sans titre"),poster.takeIf{it.startsWith("http://")||it.startsWith("https://")}.orEmpty(),o.optString("container_extension","mp4"),o.optString("category_id",""),kind,o.optString("rating","").takeIf{it.isNotBlank()&&it!="0"}.orEmpty(),o.optInt("tv_archive",0)>0,o.optString("tv_archive_duration","0").toIntOrNull()?:0)
 }
}
private fun httpGet(address:String):String{
 val c=(URL(address).openConnection() as HttpURLConnection).apply{connectTimeout=15000;readTimeout=30000;requestMethod="GET";setRequestProperty("User-Agent","TVUltra/0.3")}
 return try{val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val body=stream?.bufferedReader()?.use{it.readText()}.orEmpty();if(code !in 200..299)error("réponse HTTP "+code);body}finally{c.disconnect()}
}
private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
private fun key(item:TvItem)=item.kind+":"+item.id
private fun Set<String>.toggle(k:String):Set<String> = if(contains(k))this-k else this+k
