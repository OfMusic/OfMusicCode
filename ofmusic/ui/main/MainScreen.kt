package com.haziel.ofmusic.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haziel.ofmusic.R
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.domain.tier.UpgradeReason
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.auth.AuthScreen
import com.haziel.ofmusic.ui.main.components.*
import com.haziel.ofmusic.ui.main.screens.*
import com.haziel.ofmusic.ui.theme.liquidGlass

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

enum class NavigationTab { HOME, SEARCH, PLAYLISTS, LIBRARY, RADIO }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    var startAuthInRegisterMode by remember { mutableStateOf(false) }
    var showUpgradeFor by remember { mutableStateOf<UpgradeReason?>(null) }
    var showPlans by remember { mutableStateOf(false) }
    var detailSong by remember { mutableStateOf<Song?>(null) }
    val conexion = viewModel.conexion
    val context = LocalContext.current
    var showQualitySheet by remember { mutableStateOf(false) }
    var showRefreshRateSheet by remember { mutableStateOf(false) }
    var showStorageScreen by remember { mutableStateOf(false) }
    var showAccountScreen by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showNuevaPlaylistScreen by remember { mutableStateOf(false) }
    var showArtistasScreen by remember { mutableStateOf(false) }
    var showAlbumesScreen by remember { mutableStateOf(false) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var newPlaylistToAddSongsId by remember { mutableStateOf<Long?>(null) }
    var editingPlaylistId by remember { mutableStateOf<Long?>(null) }

    val isBackStackActive = isPlayerExpanded || showSettings || showAuth || showAccountScreen ||
            showPlans || showStorageScreen || showArtistasScreen || showAlbumesScreen ||
            selectedArtistName != null || showNuevaPlaylistScreen || editingPlaylistId != null ||
            selectedTab != NavigationTab.HOME

    androidx.activity.compose.BackHandler(enabled = isBackStackActive) {
        when {
            isPlayerExpanded -> isPlayerExpanded = false
            selectedArtistName != null -> selectedArtistName = null
            editingPlaylistId != null -> editingPlaylistId = null
            showNuevaPlaylistScreen -> showNuevaPlaylistScreen = false
            showAccountScreen -> showAccountScreen = false
            showStorageScreen -> showStorageScreen = false
            showPlans -> showPlans = false
            showAuth -> showAuth = false
            showSettings -> showSettings = false
            showArtistasScreen -> showArtistasScreen = false
            showAlbumesScreen -> showAlbumesScreen = false
            selectedTab == NavigationTab.PLAYLISTS -> selectedTab = NavigationTab.LIBRARY
            selectedTab != NavigationTab.HOME -> selectedTab = NavigationTab.HOME
        }
    }

    var isScrolledDown by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isScrolledDown = true
                } else if (available.y > 12f) {
                    isScrolledDown = false
                }
                return Offset.Zero
            }
        }
    }

    // 👇 ESTO CIERRA LA PANTALLA DE LOGIN CUANDO EL USUARIO SE LOGUEA O REGISTRA CON ÉXITO
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn && showAuth) {
            showAuth = false // Cierra el login
            // startAuthInRegisterMode = false // Opcional: resetear el modo
        }
    }

    // ─── Lanzar instalador cuando el APK esté listo ───
    LaunchedEffect(state.apkReadyToInstall) {
        if (state.apkReadyToInstall != null) {
            viewModel.installApk(context)
        }
    }

    SharedTransitionLayout {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
        ) {
            // ─── Contenido Principal ───
            AnimatedVisibility(
                visible = !showSettings && !showPlans && !showAuth,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(280)) + slideInHorizontally(tween(320), initialOffsetX = { it / 8 })) togetherWith fadeOut(tween(200))
                        },
                        label = "TabTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 90.dp) // Espacio para TopBar
                    ) { tab ->
                        when (tab) {
                            NavigationTab.HOME -> HomeScreenContent(
                                state = state, viewModel = viewModel,
                                onExpandPlayer = { isPlayerExpanded = true },
                                onShowDetails = { detailSong = it },
                                userTier = state.authProfile?.asUserTier() ?: UserTier.FREE,
                                onUpgradeClick = { showPlans = true }
                            )
                            NavigationTab.SEARCH -> SearchScreenContent(state, viewModel, { isPlayerExpanded = true }, { detailSong = it })
                            NavigationTab.LIBRARY -> LibraryScreenContent(
                                state = state,
                                viewModel = viewModel,
                                onExpandPlayer = { isPlayerExpanded = true },
                                onShowDetails = { detailSong = it },
                                onOpenPlaylists = { selectedTab = NavigationTab.PLAYLISTS },
                                onOpenArtistas = { showArtistasScreen = true },
                                onOpenAlbumes = { showAlbumesScreen = true },
                                onOpenSettings = { showSettings = true }
                            )
                            NavigationTab.PLAYLISTS -> PlaylistsScreenContent(
                                state = state,
                                viewModel = viewModel,
                                onBack = { selectedTab = NavigationTab.LIBRARY },
                                onExpandPlayer = { isPlayerExpanded = true },
                                onShowDetails = { detailSong = it },
                                onOpenNewPlaylist = { showNuevaPlaylistScreen = true },
                                onOpenEditPlaylist = { playlist -> editingPlaylistId = playlist.id }
                            )
                            NavigationTab.RADIO -> {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                                    contentPadding = PaddingValues(bottom = 140.dp)
                                ) {
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Radio, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Text("Radio Inteligente", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                            Text("Emisora automática en vivo basada en tus búsquedas y caché", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))

                                            Spacer(Modifier.height(16.dp))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.12f)
                                                    .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                                    .padding(20.dp)
                                            ) {
                                                Column {
                                                    Text("⚡ Emisora Automática", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    Text("Radio Infinita Personal", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Mezcla basada en los géneros y artistas de tus Me Gusta", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                                                    Spacer(Modifier.height(16.dp))

                                                    Button(
                                                        onClick = { viewModel.generateLikedBasedRadio() },
                                                        modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = primaryColor.copy(alpha = 0.25f),
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(50),
                                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Iniciar Emisora de Radio", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Text(
                                            text = "Canciones en Caché e Historial (${state.songs.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                        )
                                    }

                                    if (state.songs.isEmpty()) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                                Text("Realiza búsquedas para acumular canciones en tu historial de Radio.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                            }
                                        }
                                    } else {
                                        items(state.songs, key = { it.id }) { song ->
                                            val isCurrent = state.currentSong?.id == song.id
                                            SongItemRow(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && state.isPlaying,
                                                isLiked = state.likedSongIds.contains(song.id),
                                                isDownloaded = song.isDownloaded,
                                                isDownloading = state.downloadingSongIds.contains(song.id),
                                                onClick = {
                                                    val idx = state.songs.indexOfFirst { it.id == song.id }
                                                    viewModel.playSongsFromList(state.songs, idx.coerceAtLeast(0))
                                                },
                                                onShowDetails = { detailSong = song },
                                                onLikeToggle = { viewModel.toggleLike(song) },
                                                onDownloadToggle = { viewModel.downloadSong(song) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── Vista de Editar Playlist ───
                    AnimatedVisibility(
                        visible = editingPlaylistId != null,
                        enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                        exit = slideOutHorizontally(tween(280), targetOffsetX = { it }),
                        modifier = Modifier.fillMaxSize().padding(top = 90.dp)
                    ) {
                        editingPlaylistId?.let { id ->
                            EditarPlaylistScreen(
                                playlistId = id,
                                state = state,
                                viewModel = viewModel,
                                onBack = { editingPlaylistId = null },
                                onExpandPlayer = { isPlayerExpanded = true }
                            )
                        }
                    }

                    // ─── TopBar Exacto a Figma ───
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Izquierda: Avatar + Saludo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                // Avatar con anillo
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            showSettings = true
                                        }
                                ) {
                                    ProfileAvatar(photoUri = state.userProfile.photoUri, name = state.userProfile.name, size = 40.dp)
                                }
                                // Punto de estado
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFBB86FC), CircleShape)
                                        .border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.background,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // Centro: Logo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "OfMusic",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Derecha: Online/Offline Badge
                        Box(
                            modifier = Modifier
                                .liquidGlass(RoundedCornerShape(50), alpha = 0.12f)
                                .border(
                                    1.dp,
                                    if (conexion) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else Color(0xFFFF5252).copy(alpha = 0.4f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (conexion) MaterialTheme.colorScheme.primary else Color(0xFFFF5252), CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (conexion) "Online" else "Offline",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (conexion) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = "Estado Wi-Fi",
                                    tint = if (conexion) MaterialTheme.colorScheme.primary else Color(0xFFFF5252),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // ─── Bottom Bar & MiniPlayer ───
                    val isMiniPlayerActive = state.currentSong != null
                    val shouldCollapse = isScrolledDown && isMiniPlayerActive
                    val isAnyOverlayOpen = showSettings || showAuth || showAccountScreen || showPlans ||
                            showStorageScreen || showNuevaPlaylistScreen

                    AnimatedVisibility(
                        visible = !isAnyOverlayOpen && !isPlayerExpanded,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200)),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = shouldCollapse,
                            transitionSpec = {
                                if (targetState) {
                                    (fadeIn(tween(450, easing = LinearOutSlowInEasing)) + scaleIn(tween(450, easing = LinearOutSlowInEasing), initialScale = 0.94f)) togetherWith
                                            (fadeOut(tween(350, easing = FastOutLinearInEasing)) + scaleOut(tween(350, easing = FastOutLinearInEasing), targetScale = 0.94f))
                                } else {
                                    (fadeIn(tween(450, easing = LinearOutSlowInEasing)) + scaleIn(tween(450, easing = LinearOutSlowInEasing), initialScale = 0.94f)) togetherWith
                                            (fadeOut(tween(350, easing = FastOutLinearInEasing)) + scaleOut(tween(350, easing = FastOutLinearInEasing), targetScale = 0.94f))
                                }
                            },
                            label = "BottomBarCollapseTransition",
                            modifier = Modifier.fillMaxWidth()
                        ) { isCollapsed ->
                        if (!isCollapsed) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (state.currentSong != null) {
                                    AnimatedVisibility(
                                        visible = !isPlayerExpanded,
                                        enter = fadeIn(animationSpec = tween(300)),
                                        exit = fadeOut(animationSpec = tween(300))
                                    ) {
                                        UnifiedMiniPlayer(
                                            song = state.currentSong!!,
                                            isPlaying = state.isPlaying,
                                            isBuffering = state.isBuffering,
                                            playbackPositionMs = state.currentPositionMs,
                                            playbackDurationMs = state.durationMs,
                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                            onNext = { viewModel.playNext() },
                                            onClick = { isPlayerExpanded = true },
                                            onLikeToggle = { viewModel.toggleLike(state.currentSong!!) },
                                            isLiked = state.likedSongIds.contains(state.currentSong!!.id),
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedVisibility,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // Navegación flotante
                                BottomNavigationBar(selectedTab, { selectedTab = it })
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Botón Compacto de Navegación (Pestaña Activa)
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .liquidGlass(CircleShape, alpha = 0.15f)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                        .clickable { isScrolledDown = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val painter = when (selectedTab) {
                                        NavigationTab.HOME -> painterResource(id = R.drawable.ico_home)
                                        NavigationTab.SEARCH -> painterResource(id = R.drawable.ico_explorar)
                                        NavigationTab.RADIO -> painterResource(id = R.drawable.ico_radio)
                                        NavigationTab.LIBRARY -> painterResource(id = R.drawable.ico_biblioteca)
                                        else -> painterResource(id = R.drawable.ico_buscar)
                                    }
                                    Icon(
                                        painter = painter,
                                        contentDescription = "Pestaña activa",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Reproductor Minimizado Ajustado al Lado
                                AnimatedVisibility(
                                    visible = !isPlayerExpanded,
                                    enter = fadeIn(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(300)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    UnifiedMiniPlayer(
                                        song = state.currentSong!!,
                                        isPlaying = state.isPlaying,
                                        isBuffering = state.isBuffering,
                                        playbackPositionMs = state.currentPositionMs,
                                        playbackDurationMs = state.durationMs,
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.playNext() },
                                        onClick = { isPlayerExpanded = true },
                                        onLikeToggle = { viewModel.toggleLike(state.currentSong!!) },
                                        isLiked = state.likedSongIds.contains(state.currentSong!!.id),
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedVisibility,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    }

                    val playerEase = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
                    state.currentSong?.let { current ->
                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = fadeIn(tween(500, easing = playerEase)),
                            exit = fadeOut(tween(450, easing = playerEase))
                        ) {
                            UnifiedFullPlayerSheet(
                                song = current, userTier = state.authProfile?.asUserTier() ?: UserTier.FREE,
                                onCollapse = { isPlayerExpanded = false },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedVisibility,
                                isPlaying = state.isPlaying, currentPositionMs = state.currentPositionMs, durationMs = state.durationMs,
                                queue = state.queue, volume = state.volume, onVolumeChange = { viewModel.setVolume(it) },
                                queueIndex = state.queueIndex, isShuffleEnabled = state.isShuffleEnabled, repeatMode = state.repeatMode,
                                videoUrlMap = state.videoUrlMap, isLiked = state.likedSongIds.contains(current?.id),
                                isDownloaded = state.downloadedSongMap.containsKey(current?.id), isDownloading = state.downloadingSongIds.contains(current?.id),
                                lyrics = state.currentLyrics, isLoadingLyrics = state.isLoadingLyrics, showLyrics = state.showLyrics,
                                onToggleLyrics = { viewModel.toggleLyricsVisibility() },
                                onTogglePlayPause = { viewModel.togglePlayPause() }, onNext = { viewModel.playNext() }, onPrevious = { viewModel.playPrevious() },
                                onSeekTo = { posMs -> viewModel.seekTo(posMs) }, onToggleShuffle = { viewModel.toggleShuffle() }, onToggleRepeat = { viewModel.toggleRepeat() },
                                onLikeToggle = { song -> viewModel.toggleLike(song) }, onDownloadToggle = { song -> viewModel.downloadSong(song) },
                                onSelectQueueSong = { selected -> val idx = state.queue.indexOfFirst { it.id == selected.id }; if (idx >= 0) viewModel.seekToQueueIndex(idx) },
                                onFetchVideoUrl = { song -> viewModel.loadVideoStreamUrl(song, false) }, onForceRefreshVideoUrl = { song -> viewModel.forceRefreshVideoStreamUrl(song) },
                                onToggleAudioPro = { viewModel.toggleSpatialAudio(!state.isSpatialAudioEnabled) }
                            )
                        }
                    }

                }
            }

            // Overlays (Settings, Plans, Auth, FullPlayer)
            AnimatedVisibility(visible = showSettings, enter = slideInHorizontally(tween(320), initialOffsetX = { it }), exit = slideOutHorizontally(tween(280), targetOffsetX = { it })) {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showSettings = false },
                    onEditProfile = { showEditProfile = true },
                    onQuality = { showQualitySheet = true },
                    onRefreshRate = { showRefreshRateSheet = true },
                    onGoToLibrary = { showSettings = false; selectedTab = NavigationTab.LIBRARY },
                    onOpenAuth = { isRegister ->
                        startAuthInRegisterMode = isRegister
                        showSettings = false // Cierra ajustes
                        showAuth = true      // Abre el Auth
                    },
                    onOpenPlans = { showPlans = true },
                    onOpenStorage = { showStorageScreen = true },
                    onOpenAccount = { showAccountScreen = true },
                )
            }

            // 👇 VISTA DE AUTENTICACIÓN
            AnimatedVisibility(
                visible = showAuth,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }) + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it }) + fadeOut(tween(180))
            ) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AuthScreen(
                        errorMessage = state.authError,
                        isLoading = state.isAuthLoading,
                        onLogin = { email, pass -> viewModel.signIn(email, pass) },
                        onRegister = { name, email, pass, photoUri -> viewModel.signUp(email, pass, name, photoUri) },
                        onClearError = { viewModel.clearAuthError() },
                        onContinueAsGuest = { showAuth = false },
                        onBack = { showAuth = false }, // 👈 ESTO HACE QUE FUNCIONE EL BOTÓN DE REGRESAR
                        isRegisteringA = startAuthInRegisterMode // 👈 LE PASAMOS EL MODO
                    )
                }
            }

            AnimatedVisibility(
                visible = showAccountScreen,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                AccountScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showAccountScreen = false },
                    onEditProfile = { showEditProfile = true },
                    onOpenPlans = { showPlans = true }
                )
            }

            AnimatedVisibility(visible = showPlans, enter = slideInHorizontally(tween(320), initialOffsetX = { it }), exit = slideOutHorizontally(tween(280), targetOffsetX = { it })) {
                PlansScreen(
                    currentTier = state.authProfile?.asUserTier() ?: UserTier.FREE,
                    onBack = { showPlans = false },
                    onSelectPlan = { tier ->
                        viewModel.simulateTierPurchase(tier)
                        showPlans = false
                    }
                )
            }

            AnimatedVisibility(
                visible = showStorageScreen,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                StorageScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showStorageScreen = false },
                    onOpenQuality = { showQualitySheet = true }
                )
            }

            AnimatedVisibility(
                visible = showArtistasScreen,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                ArtistasScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showArtistasScreen = false },
                    onExpandPlayer = { isPlayerExpanded = true },
                    onShowDetails = { detailSong = it },
                    onOpenArtistDetail = { artistName -> selectedArtistName = artistName }
                )
            }

            AnimatedVisibility(
                visible = selectedArtistName != null,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                selectedArtistName?.let { artistName ->
                    ArtistaDetailScreen(
                        artistName = artistName,
                        state = state,
                        viewModel = viewModel,
                        onBack = { selectedArtistName = null },
                        onExpandPlayer = { isPlayerExpanded = true },
                        onShowDetails = { detailSong = it }
                    )
                }
            }

            AnimatedVisibility(
                visible = showAlbumesScreen,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                AlbumesScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { showAlbumesScreen = false },
                    onExpandPlayer = { isPlayerExpanded = true },
                    onShowDetails = { detailSong = it }
                )
            }

            AnimatedVisibility(
                visible = showNuevaPlaylistScreen,
                enter = slideInHorizontally(tween(320), initialOffsetX = { it }),
                exit = slideOutHorizontally(tween(280), targetOffsetX = { it })
            ) {
                NuevaPlaylistScreen(
                    viewModel = viewModel,
                    onBack = { showNuevaPlaylistScreen = false },
                    onCreateSuccess = { showNuevaPlaylistScreen = false },
                    onAddSongsNow = { playlistId ->
                        showNuevaPlaylistScreen = false
                        newPlaylistToAddSongsId = playlistId
                    }
                )
            }
        }

        newPlaylistToAddSongsId?.let { playlistId ->
            state.playlists.find { it.id == playlistId }?.let { targetPlaylist ->
                AddSongsToPlaylistDialog(
                    playlist = targetPlaylist,
                    allSongs = state.songs,
                    likedSongIds = state.likedSongIds,
                    downloadedSongMap = state.downloadedSongMap,
                    onDismiss = { newPlaylistToAddSongsId = null },
                    onAddSong = { song ->
                        viewModel.addSongsToPlaylist(playlistId, listOf(song))
                    },
                    onConfirm = { songs ->
                        viewModel.addSongsToPlaylist(playlistId, songs)
                    }
                )
            }
        }

        // Diálogos (SongDetailSheet, UpgradeDialog)
        detailSong?.let { song ->
            val currentSongData = state.songs.find { it.id == song.id } ?: song
            SongDetailSheet(
                song = currentSongData, isLiked = state.likedSongIds.contains(song.id),
                isDownloaded = state.downloadedSongMap.containsKey(song.id), isDownloading = state.downloadingSongIds.contains(song.id),
                playlists = state.playlists, onDismiss = { detailSong = null },
                onLikeToggle = { viewModel.toggleLike(currentSongData) }, onPlayNow = { viewModel.playSongSingle(currentSongData); detailSong = null; isPlayerExpanded = true },
                onAddToQueue = { viewModel.addToQueue(currentSongData) }, onAddToPlaylist = { id -> viewModel.addSongsToPlaylist(id, listOf(currentSongData)) },
                onDownload = { viewModel.downloadSong(currentSongData) }, onRepair = { viewModel.repairSong(currentSongData); detailSong = null }
            )
        }
        showUpgradeFor?.let { reason ->
            UpgradeDialog(reason = reason, onDismiss = { showUpgradeFor = null; viewModel.dismissUpgrade() }, onSeePlans = { showUpgradeFor = null; viewModel.dismissUpgrade(); showPlans = true })
        }

        // ─── Diálogo de actualización ───
        state.updateInfo?.let { info ->
            UpdateDialog(
                updateInfo = info,
                isDownloading = state.isDownloadingUpdate,
                downloadProgress = state.updateDownloadProgress,
                errorMessage = state.updateErrorMessage,
                onDismiss = { viewModel.dismissUpdateDialog() },
                onDownload = { viewModel.downloadAndInstallUpdate() }
            )
        }

        if (showQualitySheet) {
            QualitySelectionSheet(
                currentQuality = state.downloadQuality,
                wifiOnly = state.wifiOnlyDownload,
                autoDownload = state.autoDownloadFavorites,
                onDismiss = { showQualitySheet = false },
                onSave = { quality, wifi, auto ->
                    viewModel.setDownloadPreferences(quality, wifi, auto)
                    showQualitySheet = false
                }
            )
        }

        if (showRefreshRateSheet) {
            RefreshRateSelectionSheet(
                currentHz = state.refreshRate,
                onDismiss = { showRefreshRateSheet = false },
                onSave = { hz ->
                    viewModel.setRefreshRate(hz)
                    showRefreshRateSheet = false
                }
            )
        }

        if (showEditProfile) {
            EditProfileDialog(
                currentProfile = state.userProfile,
                onDismiss = { showEditProfile = false },
                onSave = { name, email, birthDate, country, photoUri ->
                    viewModel.updateProfile(name, email, birthDate, country, photoUri)
                    showEditProfile = false
                }
            )
        }
    }
}
@Composable
private fun BottomNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .liquidGlass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), alpha = 0.16f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.22f),
                        Color(0xFF0B0814)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        primaryColor.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(selectedTab == NavigationTab.HOME, "Inicio", painterResource(id = R.drawable.ico_home)) { onTabSelected(NavigationTab.HOME) }
            NavItem(selectedTab == NavigationTab.RADIO, "Radio", painterResource(id = R.drawable.ico_radio)) { onTabSelected(NavigationTab.RADIO) }
            NavItem(selectedTab == NavigationTab.LIBRARY, "Biblioteca", painterResource(id = R.drawable.ico_biblioteca)) { onTabSelected(NavigationTab.LIBRARY) }
            NavItem(selectedTab == NavigationTab.SEARCH, "Buscar", painterResource(id = R.drawable.ico_buscar)) { onTabSelected(NavigationTab.SEARCH) }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    painter: Painter,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val activeColor = if (primaryColor == Color.White) Color.White else primaryColor
    val inactiveColor = Color.White.copy(alpha = 0.60f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) Modifier
                    .background(primaryColor.copy(alpha = 0.18f))
                    .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) activeColor else inactiveColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}