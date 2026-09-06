package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundCategory
import com.example.audio.SoundChoice
import com.example.audio.StudyBellSoundManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyBellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSoundPickerScreen(
    viewModel: StudyBellViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentSoundSetting = uiState.settings.defaultAlarmSound

    var playingSoundId by remember { mutableStateOf<String?>(null) }
    var systemSounds by remember { mutableStateOf<List<SoundChoice>>(emptyList()) }
    var customSounds by remember { mutableStateOf<List<SoundChoice>>(emptyList()) }

    // Load device sounds on launch
    LaunchedEffect(Unit) {
        systemSounds = StudyBellSoundManager.getDeviceSystemSounds(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            StudyBellSoundManager.stopAllSounds()
        }
    }

    // Audio picker for local voice recording or music file
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedChoice = StudyBellSoundManager.saveCustomAudio(context, uri)
            if (savedChoice != null) {
                customSounds = customSounds + savedChoice
                val soundIdentifier = "${savedChoice.name}|${savedChoice.uriString}"
                viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = soundIdentifier))
                Toast.makeText(context, "Selected: ${savedChoice.name}", Toast.LENGTH_SHORT).show()
                // Play preview
                playingSoundId = savedChoice.id
                StudyBellSoundManager.playSound(context, savedChoice, loop = false) {
                    playingSoundId = null
                }
            } else {
                Toast.makeText(context, "Could not load audio file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Alarm Sound",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
        ) {
            // Header Info Card: 100% Local Phone Audio
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Mobile Device & Local Storage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Pick device ringtones or your own voice recordings & music. Stored 100% locally.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Choose from Device (Music / Voice recordings)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(GoldAccent, PurplePrimary)),
                            RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            audioPickerLauncher.launch("audio/*")
                        }
                        .testTag("choose_from_device_card"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(GoldAccent, GoldDark))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Choose from Device",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Voice memos, MP3s, local songs",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Pick Audio",
                            tint = GoldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Custom User Audios (if any added)
            if (customSounds.isNotEmpty()) {
                item {
                    Text(
                        "Your Voice & Music Files",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(customSounds) { sound ->
                    SoundRowItem(
                        soundChoice = sound,
                        isSelected = currentSoundSetting.contains(sound.name),
                        isPlaying = playingSoundId == sound.id,
                        onSelect = {
                            val soundIdentifier = "${sound.name}|${sound.uriString}"
                            viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = soundIdentifier))
                        },
                        onPlayToggle = {
                            if (playingSoundId == sound.id) {
                                StudyBellSoundManager.stopAllSounds()
                                playingSoundId = null
                            } else {
                                playingSoundId = sound.id
                                StudyBellSoundManager.playSound(context, sound, loop = false) {
                                    playingSoundId = null
                                }
                            }
                        }
                    )
                }
            }

            // Preset Sounds (Image 2 Screen 21)
            item {
                Text(
                    "StudyBell Presets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(StudyBellSoundManager.PRESET_SOUNDS) { preset ->
                SoundRowItem(
                    soundChoice = preset,
                    isSelected = currentSoundSetting == preset.name,
                    isPlaying = playingSoundId == preset.id,
                    onSelect = {
                        viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = preset.name))
                    },
                    onPlayToggle = {
                        if (playingSoundId == preset.id) {
                            StudyBellSoundManager.stopAllSounds()
                            playingSoundId = null
                        } else {
                            playingSoundId = preset.id
                            StudyBellSoundManager.playSound(context, preset, loop = false) {
                                playingSoundId = null
                            }
                        }
                    }
                )
            }

            // Mobile Device System Sounds
            if (systemSounds.isNotEmpty()) {
                item {
                    Text(
                        "Device System Sounds",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(systemSounds) { sysSound ->
                    SoundRowItem(
                        soundChoice = sysSound,
                        isSelected = currentSoundSetting.contains(sysSound.name),
                        isPlaying = playingSoundId == sysSound.id,
                        onSelect = {
                            val soundIdentifier = "${sysSound.name}|${sysSound.uriString}"
                            viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = soundIdentifier))
                        },
                        onPlayToggle = {
                            if (playingSoundId == sysSound.id) {
                                StudyBellSoundManager.stopAllSounds()
                                playingSoundId = null
                            } else {
                                playingSoundId = sysSound.id
                                StudyBellSoundManager.playSound(context, sysSound, loop = false) {
                                    playingSoundId = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SoundRowItem(
    soundChoice: SoundChoice,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlayToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                PurplePrimary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, PurplePrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Play / Stop Icon Button
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) CoralPink.copy(alpha = 0.2f) else PurplePrimary.copy(alpha = 0.12f)
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Preview",
                        tint = if (isPlaying) CoralPink else PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = soundChoice.name,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (soundChoice.category) {
                            SoundCategory.PRESET -> "Preset Chime"
                            SoundCategory.DEVICE_SYSTEM -> "System Ringtone"
                            SoundCategory.USER_CUSTOM -> "Local Audio File"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PurplePrimary
                )
            )
        }
    }
}
