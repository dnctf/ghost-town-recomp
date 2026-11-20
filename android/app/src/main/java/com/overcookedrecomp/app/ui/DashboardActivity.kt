package com.overcookedrecomp.app.ui

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalDensity
import androidx.documentfile.provider.DocumentFile
import com.overcookedrecomp.app.ui.theme.GhostTownColors as C
import com.overcookedrecomp.app.ui.theme.GhostTownSpace
import com.overcookedrecomp.app.ui.theme.GhostTownTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DashboardTab(val label: String) { GAMES("Moving Out 2"), LIBRARY("Game Files"), RUNTIME("Runtime"), SETTINGS("Settings") }
private data class TargetGame(val title: String, val initials: String, val genre: String, val lastPlayed: String, val readiness: String, val runtime: String, val colors: List<Color>)
private data class Notice(val id: Int, val title: String, val detail: String)

private val targetGames = listOf(
    TargetGame("Moving Out 2", "MO2", "UNITY IL2CPP / WINDOWS x64", "Belum dimainkan", "Belum divalidasi", "Runtime belum tersedia", listOf(Color(0xFFCE8D45), Color(0xFF263B4C)))
)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { GhostTownTheme { ConsoleDashboard() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConsoleDashboard() {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DashboardTab.GAMES) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var tabsFocused by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }
    var contextMenuOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var reduceMotion by remember { mutableStateOf(false) }
    var uiScale by remember { mutableFloatStateOf(1f) }
    var uiSounds by remember { mutableStateOf(false) }
    var controlsMap by remember { mutableStateOf("Default · Gamepad") }
    var libraryUri by remember { mutableStateOf(context.getSharedPreferences("moving-out-2", Context.MODE_PRIVATE).getString("game_folder_uri", null)) }
    var mo2Validation by remember { mutableStateOf(MovingOut2InstallationValidator.Report(false, false, "Folder game belum dipilih.", listOf("Pilih folder Moving Out 2"), emptyList())) }
    var timeText by remember { mutableStateOf("--:--") }
    var battery by remember { mutableIntStateOf(-1) }
    var thermal by remember { mutableStateOf("TERMAL: —") }
    var gate by remember { mutableStateOf(RuntimeLaunchGate.read(context)) }
    var notices by remember { mutableStateOf(listOf(
        Notice(1, "MOVING OUT 2 ONLY", "Game khusus · runtime compatibility belum terverifikasi"),
        Notice(2, "RUNTIME PENDING", "Pemasangan belum tersedia")
    )) }
    var nextNoticeId by remember { mutableIntStateOf(3) }
    val selected = targetGames[selectedIndex.coerceIn(0, targetGames.lastIndex)].copy(readiness = mo2Validation.summary)
    val playState = PlayLaunchPolicy.resolve(gate.runtimeInstalled, gate.displaySelfTestPassed)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            libraryUri = uri.toString()
            context.getSharedPreferences("moving-out-2", Context.MODE_PRIVATE).edit().putString("game_folder_uri", libraryUri).apply()
            mo2Validation = MovingOut2InstallationValidator.validate(context, uri)
val validationNotice = if (mo2Validation.detected) "File baseline Moving Out 2 terdeteksi; runtime belum teruji." else mo2Validation.summary
notices = (notices + Notice(nextNoticeId, "VALIDASI FILE", validationNotice)).takeLast(2)
            nextNoticeId++
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            battery = (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            thermal = if (android.os.Build.VERSION.SDK_INT >= 29 && pm != null) {
                val status = runCatching { pm.currentThermalStatus }.getOrDefault(0)
                when (status) {
                    0 -> "TERMAL: NORMAL"
                    1 -> "TERMAL: RINGAN"
                    2 -> "TERMAL: SEDANG"
                    3, 4 -> "TERMAL: TINGGI"
                    else -> "TERMAL: KRITIS"
                }
            } else "TERMAL: —"
            gate = RuntimeLaunchGate.read(context)
            delay(3000)
        }
    }
    LaunchedEffect(notices.lastOrNull()?.id) {
        val latestId = notices.lastOrNull()?.id
        if (latestId != null) {
            delay(5500)
            notices = notices.filterNot { it.id == latestId }
        }
    }
    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocusRequester.requestFocus() } }
    BackHandler(enabled = detailsOpen || contextMenuOpen || searchOpen || activeTab != DashboardTab.GAMES) {
        when {
            searchOpen -> searchOpen = false
            contextMenuOpen -> contextMenuOpen = false
            detailsOpen -> detailsOpen = false
            else -> activeTab = DashboardTab.GAMES
        }
    }

    val transition = rememberInfiniteTransition(label = "storm-drift")
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "storm-phase"
    )
    val phase = if (reduceMotion) 0f else drift

    Box(
        Modifier.fillMaxSize().background(C.Storm)
            .focusRequester(rootFocusRequester).focusable().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || searchOpen) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (tabsFocused || activeTab != DashboardTab.GAMES) {
                            activeTab = DashboardTab.entries[(activeTab.ordinal - 1).coerceAtLeast(0)]
                        } else if (!detailsOpen) selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        true
                    }
                    Key.DirectionRight -> {
                        if (tabsFocused || activeTab != DashboardTab.GAMES) {
                            activeTab = DashboardTab.entries[(activeTab.ordinal + 1).coerceAtMost(DashboardTab.entries.lastIndex)]
                        } else if (!detailsOpen) selectedIndex = (selectedIndex + 1).coerceAtMost(targetGames.lastIndex)
                        true
                    }
                    Key.DirectionUp -> { tabsFocused = true; true }
                    Key.DirectionDown -> { tabsFocused = false; true }
                    Key.ButtonA, Key.Enter -> {
                        if (!tabsFocused && activeTab == DashboardTab.GAMES && !detailsOpen) detailsOpen = true
                        else tabsFocused = false
                        true
                    }
                    Key.ButtonB, Key.Escape -> {
                        if (detailsOpen) detailsOpen = false else if (activeTab != DashboardTab.GAMES) activeTab = DashboardTab.GAMES
                        tabsFocused = false
                        true
                    }
                    Key.ButtonY, Key.Y -> { searchOpen = true; tabsFocused = false; true }
                    Key.Menu -> { contextMenuOpen = true; true }
                    else -> false
                }
            }
    ) {
        StormBackdrop(phase = phase)
        Column(Modifier.fillMaxSize().padding(horizontal = (GhostTownSpace.Page * uiScale).dp, vertical = 16.dp)) {
            TopBar(
                activeTab = activeTab,
                onTab = { activeTab = it },
                battery = battery,
                thermal = thermal,
                time = timeText,
                runtimeReady = gate.runtimeInstalled
            )
            Spacer(Modifier.height(14.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    DashboardTab.GAMES -> GamesHome(
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it; detailsOpen = true },
                        onLongSelect = { selectedIndex = it; contextMenuOpen = true },
                        onAddGame = { picker.launch(null); activeTab = DashboardTab.LIBRARY },
                        onRuntime = { activeTab = DashboardTab.RUNTIME },
                        onSearch = { searchOpen = true },
                        selected = selected,
                        onDetails = { detailsOpen = true }
                    )
                    DashboardTab.LIBRARY -> LibraryScreen(
                        libraryUri = libraryUri,
                        validation = mo2Validation,
                        onPick = { picker.launch(null) }
                    )
                    DashboardTab.RUNTIME -> RuntimeScreen(gate = gate)
                    DashboardTab.SETTINGS -> SettingsScreen(
                        reduceMotion = reduceMotion, onReduceMotion = { reduceMotion = it },
                        uiScale = uiScale, onUiScale = { uiScale = it },
                        uiSounds = uiSounds, onUiSounds = { uiSounds = it },
                        controlsMap = controlsMap, onControlsMap = { controlsMap = it }
                    )
                }
                Column(
                    Modifier.align(Alignment.TopEnd).widthIn(max = 270.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    notices.takeLast(2).forEach { NoticePill(it) }
                }
            }
            if (activeTab == DashboardTab.GAMES) {
                BottomHints()
            } else {
                BottomHints(label = "A PILIH    /    B KEMBALI    /    MENU PENGATURAN")
            }
        }

        if (detailsOpen) {
            ModalBottomSheet(
                onDismissRequest = { detailsOpen = false },
                containerColor = C.StormRaised,
                contentColor = C.TextPrimary,
                dragHandle = { BottomSheetDefaults.DragHandle(color = C.TextMuted) }
            ) {
                GameDetailsSheet(
                    game = selected,
                    state = playState,
                    runtimeInstalled = gate.runtimeInstalled,
                    displayPassed = gate.displaySelfTestPassed,
                    onClose = { detailsOpen = false }
                )
            }
        }
        if (searchOpen) {
            val matches = targetGames.withIndex().filter { it.value.title.contains(searchQuery, ignoreCase = true) }
            AlertDialog(
                onDismissRequest = { searchOpen = false },
                containerColor = C.Surface,
                title = { Text("Moving Out 2", color = C.TextPrimary) },
                text = {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            label = { Text("Cari Moving Out 2") }
                        )
                        Spacer(Modifier.height(8.dp))
                        matches.forEach { match ->
                            TextButton(onClick = {
                                selectedIndex = match.index
                                activeTab = DashboardTab.GAMES
                                searchOpen = false
                                detailsOpen = true
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(match.value.title, color = C.TextPrimary)
                            }
                        }
                        if (matches.isEmpty()) Text("Moving Out 2 tidak ditemukan.", color = C.TextMuted, fontSize = 11.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { searchOpen = false }) { Text("TUTUP") } }
            )
        }
        if (contextMenuOpen) {
            AlertDialog(
                onDismissRequest = { contextMenuOpen = false },
                containerColor = C.Surface,
                title = { Text("Menu · ${selected.title}", color = C.TextPrimary) },
                text = { Text("Moving Out 2 · Verifikasi game, folder data, dan kesiapan runtime.", color = C.TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { contextMenuOpen = false; detailsOpen = true }) { Text("DETAIL") }
                },
                dismissButton = { TextButton(onClick = { contextMenuOpen = false }) { Text("TUTUP") } }
            )
        }
    }
}

@Composable
private fun TopBar(
    activeTab: DashboardTab,
    onTab: (DashboardTab) -> Unit,
    battery: Int,
    thermal: String,
    time: String,
    runtimeReady: Boolean
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.weight(1.05f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(C.Accent, C.AccentDeep))), contentAlignment = Alignment.Center) {
                Text("GT", color = C.Storm, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("MOVING OUT 2", color = C.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("PROFIL LOKAL  ·  ${if (runtimeReady) "RUNTIME: SIAP" else "RUNTIME: BELUM TERPASANG"}", color = if (runtimeReady) C.Accent else C.Warning, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Row(Modifier.weight(1.3f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            DashboardTab.entries.forEach { tab ->
                val selected = activeTab == tab
                TextButton(
                    onClick = { onTab(tab) },
                    modifier = Modifier.padding(horizontal = 2.dp).heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = if (selected) C.Storm else C.TextSecondary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(if (selected) C.Accent else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(tab.label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
        Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${if (battery >= 0) "BAT $battery%" else "BAT —"}   ·   $thermal", color = C.TextSecondary, fontSize = 9.sp, maxLines = 1)
                Text(time, color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StormBackdrop(phase: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF263640), Color(0xFF101B25), C.Storm), start = Offset(size.width * phase, 0f), end = Offset(size.width, size.height)))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF5A8585).copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width * (0.72f - phase * 0.08f), size.height * 0.32f), radius = size.width * 0.52f), radius = size.width * 0.52f, center = Offset(size.width * (0.72f - phase * 0.08f), size.height * 0.32f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF607080).copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width * 0.12f, size.height * 0.82f), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(size.width * 0.12f, size.height * 0.82f))
        for (i in 0..38) {
            val x = ((i * 137.0f) % size.width) + phase * 8f
            val y = ((i * 83.0f) % size.height)
            drawCircle(Color.White.copy(alpha = 0.025f), radius = if (i % 3 == 0) 1.8f else 1f, center = Offset(x, y))
        }
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, C.Storm.copy(alpha = 0.72f)), startY = size.height * 0.3f, endY = size.height))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GamesHome(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onLongSelect: (Int) -> Unit,
    onAddGame: () -> Unit,
    onRuntime: () -> Unit,
    onSearch: () -> Unit,
    selected: TargetGame,
    onDetails: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DEDICATED GAME  /  MOVING OUT 2", color = C.Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Windows PC game · Android ARM64 target", color = C.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("GAME-SPECIFIC RUNTIME", color = C.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                targetGames.forEachIndexed { index, game ->
                    GameCard(
                        game = game,
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        onLongClick = { onLongSelect(index) }
                    )
                }
                UtilityCard("＋", "Import Moving Out 2", "Pilih folder game", onAddGame)
                UtilityCard("◈", "Runtime", "Komponen & unduhan", onRuntime)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(selected.genre.uppercase(), color = C.Accent, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Text(selected.title, color = C.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    StatBlock("TERAKHIR DIMAINKAN", selected.lastPlayed)
                    StatBlock("KESIAPAN", selected.readiness)
                    StatBlock("VERSI RUNTIME", selected.runtime)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSearch, modifier = Modifier.heightIn(min = 48.dp)) { Text("CARI  Y", color = C.TextSecondary, fontSize = 10.sp) }
                Button(onClick = onDetails, colors = ButtonDefaults.buttonColors(containerColor = C.Accent, contentColor = C.Storm), modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("DETAIL MOVING OUT 2  →", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(game: TargetGame, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val width = if (selected) 252.dp else 174.dp
    val height = if (selected) 188.dp else 148.dp
    val animatedWidth by androidx.compose.animation.core.animateDpAsState(targetValue = width, animationSpec = tween(GhostTownSpace.FocusDuration), label = "card-width")
    val animatedHeight by androidx.compose.animation.core.animateDpAsState(targetValue = height, animationSpec = tween(GhostTownSpace.FocusDuration), label = "card-height")
    val borderColor = if (selected) C.FocusBorder else Color.White.copy(alpha = 0.10f)
    val elevation = if (selected) 16.dp else 2.dp
    Box(
        Modifier.width(animatedWidth).height(animatedHeight).shadow(elevation, RoundedCornerShape(18.dp), ambientColor = C.Accent.copy(alpha = if (selected) 0.22f else 0f), spotColor = C.Accent.copy(alpha = if (selected) 0.24f else 0f))
            .clip(RoundedCornerShape(18.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(game.colors, start = Offset.Zero, end = Offset(size.width, size.height)))
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.minDimension * 0.48f, center = Offset(size.width * 0.85f, size.height * 0.15f))
            drawCircle(Color.Black.copy(alpha = 0.14f), radius = size.minDimension * 0.52f, center = Offset(size.width * 0.08f, size.height * 0.9f))
            val path = Path().apply {
                moveTo(0f, size.height * 0.74f)
                cubicTo(size.width * 0.25f, size.height * 0.55f, size.width * 0.54f, size.height * 0.98f, size.width, size.height * 0.62f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Color.Black.copy(alpha = 0.22f))
        }
        Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("MOVING OUT 2", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(game.initials, color = Color.White.copy(alpha = if (selected) 0.96f else 0.72f), fontSize = if (selected) 48.sp else 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
            Text(game.title, color = Color.White, fontSize = if (selected) 15.sp else 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(game.genre, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1)
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(9.dp).clip(CircleShape).background(C.Accent))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UtilityCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp).height(138.dp).clip(RoundedCornerShape(18.dp))
            .background(C.Surface.copy(alpha = 0.88f)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = {}),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = C.Accent, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text(title, color = C.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = C.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = C.TextMuted, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        Text(value, color = C.TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun NoticePill(notice: Notice) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.SurfaceElevated.copy(alpha = 0.96f))
            .border(1.dp, C.Accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(C.Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Text("i", color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column {
            Text(notice.title, color = C.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text(notice.detail, color = C.TextSecondary, fontSize = 9.sp, maxLines = 2)
        }
    }
}

@Composable
private fun BottomHints(label: String = "A PILIH    /    B KEMBALI    /    MENU OPSI") {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MOVING OUT 2 ANDROID   ·   COMPATIBILITY BUILD", color = C.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        Text(label, color = C.TextSecondary, fontSize = 9.sp, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun GameDetailsSheet(
    game: TargetGame,
    state: PlayLaunchState,
    runtimeInstalled: Boolean,
    displayPassed: Boolean,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("GAME PROFILE  /  MOVING OUT 2", color = C.Accent, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        Text(game.title, color = C.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Kartu ini memakai artwork abstrak buatan sendiri. Tidak ada file game atau aset komersial yang disertakan.", color = C.TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatBlock("DATA GAME", game.readiness)
            StatBlock("RUNTIME", if (runtimeInstalled) "Terdeteksi" else "Belum terpasang")
            StatBlock("DISPLAY SELF-TEST", if (displayPassed) "Lolos" else "Belum lolos")
        }
        val reason = when (state) {
            PlayLaunchState.READY -> "Runtime/display gate terdeteksi, tetapi integrasi launch game belum tersedia."
            PlayLaunchState.BLOCKED_RUNTIME -> "Runtime belum terpasang."
            PlayLaunchState.BLOCKED_DISPLAY -> "Display belum lolos tes."
            PlayLaunchState.PROCESSING -> "Memeriksa kesiapan..."
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (state == PlayLaunchState.READY) C.Accent.copy(alpha = 0.12f) else C.Surface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                PlayLaunchState.PROCESSING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = C.Accent, strokeWidth = 2.dp)
                else -> Text(if (state == PlayLaunchState.READY) "✓" else "⊘", color = if (state == PlayLaunchState.READY) C.Accent else C.Warning, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        PlayLaunchState.READY -> "SIAP"
                        PlayLaunchState.PROCESSING -> "MEMPROSES"
                        else -> "BELUM SIAP"
                    },
                    color = C.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
                Text(reason, color = C.TextSecondary, fontSize = 11.sp)
            }
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = C.Accent, contentColor = C.Storm,
                    disabledContainerColor = C.SurfaceElevated, disabledContentColor = C.TextMuted
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("PLAY  ·  BELUM TERSEDIA", fontWeight = FontWeight.Bold) }
        }
        Text("Status UI tidak membuktikan game dapat berjalan. Launch dinonaktifkan sampai runtime terintegrasi.", color = C.TextMuted, fontSize = 10.sp)
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("TUTUP") }
    }
}

@Composable
private fun RuntimeScreen(gate: RuntimeLaunchGate.Snapshot) {
    Column(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("RUNTIME", color = C.Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("Komponen dan kesiapan sistem", color = C.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Column(
            Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(18.dp)).background(C.Surface.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RuntimeRow("Runtime bundle", if (gate.runtimeInstalled) "TERPASANG" else "BELUM TERPASANG", gate.runtimeInstalled)
            RuntimeRow("Display self-test", if (gate.displaySelfTestPassed) "LOLOS" else "BELUM LOLOS", gate.displaySelfTestPassed)
            RuntimeRow("Wine / Box64", "MENUNGGU VALIDASI", false)
            RuntimeRow("Display / VirGL", "MENUNGGU VALIDASI", false)
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("PASANG RUNTIME  ·  BELUM TERSEDIA")
            }
            Text("Manifest runtime_pack berstatus pending. Tombol pemasangan sengaja dinonaktifkan.", color = C.Warning, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RuntimeRow(label: String, status: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(status, color = if (ready) C.Accent else C.Warning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LibraryScreen(libraryUri: String?, validation: MovingOut2InstallationValidator.Report, onPick: () -> Unit) {
    val context = LocalContext.current
    val folderName = libraryUri?.let { uri -> runCatching { DocumentFile.fromTreeUri(context, android.net.Uri.parse(uri))?.name }.getOrNull() ?: uri } ?: "Belum ada folder yang dipilih"
    Column(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("LIBRARY", color = C.Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("Folder data game milikmu", color = C.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Column(
            Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(18.dp)).background(C.Surface.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PILIH LOKASI DATA", color = C.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Text(folderName, color = C.TextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(onClick = onPick, modifier = Modifier.heightIn(min = 52.dp)) { Text("PILIH FOLDER  ·  SAF") }
            Text(validation.summary, color = if (validation.detected) C.Accent else C.Warning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
if (validation.missing.isNotEmpty()) Text("Missing: " + validation.missing.joinToString(", "), color = C.TextMuted, fontSize = 10.sp)
Text("Pemeriksaan file bersifat read-only. File game tidak dieksekusi, disalin, atau didistribusikan oleh validator.", color = C.TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsScreen(
    reduceMotion: Boolean, onReduceMotion: (Boolean) -> Unit,
    uiScale: Float, onUiScale: (Float) -> Unit,
    uiSounds: Boolean, onUiSounds: (Boolean) -> Unit,
    controlsMap: String, onControlsMap: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(top = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SETTINGS", color = C.Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("Preferensi dashboard", color = C.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        SettingToggle("Kurangi animasi", "Hentikan gerakan latar dekoratif", reduceMotion, onReduceMotion)
        SettingToggle("Suara UI", "Suara navigasi (nonaktif pada preview)", uiSounds, onUiSounds)
        Column(Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(16.dp)) {
            Text("SKALA UI  ·  ${(uiScale * 100).toInt()}%", color = C.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Slider(value = uiScale, onValueChange = onUiScale, valueRange = 0.85f..1.2f)
        }
        Column(Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(16.dp)) {
            Text("PETA KONTROL", color = C.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            listOf("Default · Gamepad", "Keyboard · WASD", "Layout aksesibel").forEach { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = controlsMap == option, onClick = { onControlsMap(option) })
                    Text(option, color = C.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = C.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = C.TextMuted, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
