package com.ccpro.captioneditor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaptionEditorApp() }
    }
}

data class Caption(val text: String, val startMs: Long, val endMs: Long)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionEditorApp() {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var captions by remember {
        mutableStateOf(listOf(Caption("Welcome", 0, 3000), Caption("to CC Caption Editor", 3000, 6000)))
    }
    var selectedIndex by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            videoUri = it
        }
    }

    MaterialTheme(colorScheme = darkScheme()) {
        Scaffold(containerColor = Color(0xFF111216), topBar = {
            TopAppBar(
                title = { Text("CC Caption Editor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF17191F), titleContentColor = Color.White),
                actions = {
                    TextButton(onClick = { /* Export with FFmpeg */ }) {
                        Text("Export", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                    if (videoUri == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Text("Import Video") }
                        }
                    } else {
                        VideoPlayer(videoUri!!) 
                        captions.getOrNull(selectedIndex)?.let {
                            Text(it.text, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).background(Color(0xCC000000), RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                CaptionTools(captions, selectedIndex, onCaptionChange = { index, text ->
                    captions = captions.mapIndexed { i, caption -> if (i == index) caption.copy(text = text) else caption }
                }, onSelect = { selectedIndex = it }, onAdd = {
                    captions = captions + Caption("New caption", captions.lastOrNull()?.endMs ?: 0, (captions.lastOrNull()?.endMs ?: 0) + 2000)
                    selectedIndex = captions.lastIndex
                })
            }
        }
    }
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = false } }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize())
}

@Composable
fun CaptionTools(captions: List<Caption>, selectedIndex: Int, onCaptionChange: (Int, String) -> Unit, onSelect: (Int) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF1B1D23)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Captions", color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = Color(0xFF22C55E)) }
        }
        captions.getOrNull(selectedIndex)?.let { caption ->
            OutlinedTextField(value = caption.text, onValueChange = { onCaptionChange(selectedIndex, it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF22C55E), unfocusedBorderColor = Color(0xFF4B5563)))
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(captions) { index, caption ->
                FilterChip(selected = index == selectedIndex, onClick = { onSelect(index) }, label = { Text(caption.text) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF22C55E), selectedLabelColor = Color.Black, containerColor = Color(0xFF30333B), labelColor = Color.White))
            }
        }
    }
}

fun darkScheme() = darkColorScheme(primary = Color(0xFF22C55E), surface = Color(0xFF17191F), background = Color(0xFF111216))
