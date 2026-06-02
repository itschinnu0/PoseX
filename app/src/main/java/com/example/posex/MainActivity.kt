package com.example.posex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.WorkoutConfig
import com.example.posex.ui.screens.HomeScreen
import com.example.posex.ui.screens.StatsScreen
import com.example.posex.ui.screens.SummaryScreen
import com.example.posex.ui.theme.PoseXTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.posex.data.PersonalBest
import com.example.posex.data.SessionRecord
import com.example.posex.data.StorageService
import com.example.posex.ui.screens.WorkoutConfigScreen
import com.example.posex.ui.screens.WorkoutScreen
import com.example.posex.data.ProfileStorageService
import com.example.posex.data.UserProfile
import com.example.posex.ui.screens.ProfileConfirmationScreen
import com.example.posex.ui.screens.ProfileCreationScreen
import com.example.posex.ui.screens.ProfileSelectionScreen
import com.example.posex.ui.screens.SettingsScreen

private enum class HomeFlow {
    HOME,
    CONFIG,
    WORKOUT,
    SUMMARY
}

private sealed class AppDestination {
    object Loading : AppDestination()
    object CreateProfile : AppDestination()
    object ConfirmProfile : AppDestination()
    object SelectProfile : AppDestination()
    object MainApp : AppDestination()
}

@Composable
fun PoseXApp(
    activeProfileId: String?,
    onSettingsTapped: () -> Unit,
    onExitAppFlow: () -> Unit
) {
    val context = LocalContext.current
    val storageService = remember(context) { StorageService(context) }

    var selectedTab by remember { mutableStateOf(0) }
    var flowState by remember { mutableStateOf(HomeFlow.HOME) }
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var workoutConfig by remember { mutableStateOf<WorkoutConfig?>(null) }
    var lastSessionRecord by remember { mutableStateOf<SessionRecord?>(null) }
    var lastPersonalBest by remember { mutableStateOf<PersonalBest?>(null) }

    val showBottomBar = selectedTab == 1 ||
        (selectedTab == 0 && flowState != HomeFlow.WORKOUT && flowState != HomeFlow.SUMMARY)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                1 -> StatsScreen(
                    storageService = storageService,
                    activeProfileId = activeProfileId,
                    onSettingsTapped = onSettingsTapped
                )
                else -> {
                    when (flowState) {
                        HomeFlow.HOME -> {
                            HomeScreen(
                                onExerciseSelected = { exercise ->
                                    selectedExercise = exercise
                                    flowState = HomeFlow.CONFIG
                                }
                            )
                        }
                        HomeFlow.CONFIG -> {
                            val exercise = selectedExercise
                            if (exercise == null) {
                                flowState = HomeFlow.HOME
                            } else {
                                WorkoutConfigScreen(
                                    exerciseType = exercise,
                                    onStartWorkout = { config ->
                                        workoutConfig = config
                                        flowState = HomeFlow.WORKOUT
                                    },
                                    onBack = {
                                        selectedExercise = null
                                        workoutConfig = null
                                        flowState = HomeFlow.HOME
                                    }
                                )
                            }
                        }
                        HomeFlow.WORKOUT -> {
                            val exercise = selectedExercise
                            val config = workoutConfig
                            if (exercise == null || config == null) {
                                flowState = HomeFlow.HOME
                            } else {
                                WorkoutScreen(
                                    exerciseType = exercise,
                                    config = config,
                                    activeProfileId = activeProfileId,
                                    onExit = { sessionId ->
                                        val session = storageService
                                            .getAllSessions()
                                            .firstOrNull { it.id == sessionId }
                                        lastSessionRecord = session
                                        lastPersonalBest = session?.let {
                                            storageService.getPersonalBest(it.exerciseType, it.profileId)
                                        }
                                        flowState = if (session != null) {
                                            HomeFlow.SUMMARY
                                        } else {
                                            HomeFlow.HOME
                                        }
                                    }
                                )
                            }
                        }
                        HomeFlow.SUMMARY -> {
                            val session = lastSessionRecord
                            if (session == null) {
                                flowState = HomeFlow.HOME
                            } else {
                                SummaryScreen(
                                    sessionRecord = session,
                                    personalBest = lastPersonalBest,
                                    onDone = {
                                        selectedTab = 0
                                        selectedExercise = null
                                        workoutConfig = null
                                        lastSessionRecord = null
                                        lastPersonalBest = null
                                        flowState = HomeFlow.HOME
                                        onExitAppFlow()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PoseXApp() {
    val context = LocalContext.current
    val profileStorageService = remember(context) { ProfileStorageService(context) }

    var activeProfile by remember { mutableStateOf(profileStorageService.getActiveProfile()) }
    var profiles by remember { mutableStateOf(profileStorageService.getAllProfiles()) }
    var appDestination by remember {
        val initialProfile = profileStorageService.getActiveProfile()
        mutableStateOf<AppDestination>(
            if (initialProfile != null) AppDestination.ConfirmProfile else AppDestination.CreateProfile
        )
    }
    var showSettings by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UserProfile?>(null) }
    var returnToSettingsAfterCreate by remember { mutableStateOf(false) }

    @Composable
    fun InsetScreen(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            content()
        }
    }

    when (appDestination) {
        is AppDestination.CreateProfile -> {
            InsetScreen {
                ProfileCreationScreen(
                    onProfileCreated = { profile ->
                        profileStorageService.saveProfile(profile)
                        profileStorageService.setActiveProfile(profile.id)
                        profileStorageService.updateLastActive(profile.id)
                        profiles = profileStorageService.getAllProfiles()
                        activeProfile = profile
                        appDestination = AppDestination.MainApp
                        showSettings = returnToSettingsAfterCreate
                        returnToSettingsAfterCreate = false
                    }
                )
            }
        }
        is AppDestination.ConfirmProfile -> {
            val profile = activeProfile
            if (profile == null) {
                appDestination = AppDestination.CreateProfile
            } else {
                InsetScreen {
                    ProfileConfirmationScreen(
                        profile = profile,
                        onContinue = {
                            profileStorageService.updateLastActive(profile.id)
                            appDestination = AppDestination.MainApp
                        },
                        onSwitch = { appDestination = AppDestination.SelectProfile },
                        onCreateNew = { appDestination = AppDestination.CreateProfile }
                    )
                }
            }
        }
        is AppDestination.SelectProfile -> {
            InsetScreen {
                ProfileSelectionScreen(
                    profiles = profiles,
                    activeProfileId = activeProfile?.id,
                    onProfileSelected = { profile ->
                        profileStorageService.setActiveProfile(profile.id)
                        profileStorageService.updateLastActive(profile.id)
                        activeProfile = profile
                        appDestination = AppDestination.MainApp
                    },
                    onCreateNew = { appDestination = AppDestination.CreateProfile }
                )
            }
        }
        is AppDestination.MainApp -> {
            when {
                editTarget != null -> {
                    InsetScreen {
                        ProfileCreationScreen(
                            existingProfile = editTarget,
                            onProfileCreated = { profile ->
                                profileStorageService.saveProfile(profile)
                                profiles = profileStorageService.getAllProfiles()
                                if (activeProfile?.id == profile.id) {
                                    activeProfile = profile
                                }
                                editTarget = null
                                showSettings = true
                            }
                        )
                    }
                }
                returnToSettingsAfterCreate -> {
                    InsetScreen {
                        ProfileCreationScreen(
                            onProfileCreated = { profile ->
                                profileStorageService.saveProfile(profile)
                                profileStorageService.setActiveProfile(profile.id)
                                profileStorageService.updateLastActive(profile.id)
                                profiles = profileStorageService.getAllProfiles()
                                activeProfile = profile
                                returnToSettingsAfterCreate = false
                                showSettings = true
                            }
                        )
                    }
                }
                showSettings -> {
                    InsetScreen {
                        SettingsScreen(
                            activeProfile = activeProfile,
                            allProfiles = profiles,
                            profileStorageService = profileStorageService,
                            onProfileSwitched = { profile ->
                                profileStorageService.setActiveProfile(profile.id)
                                profileStorageService.updateLastActive(profile.id)
                                activeProfile = profile
                            },
                            onEditProfile = { profile -> editTarget = profile },
                            onCreateNewProfile = { returnToSettingsAfterCreate = true },
                            onBack = {
                                profiles = profileStorageService.getAllProfiles()
                                activeProfile = profileStorageService.getActiveProfile()
                                showSettings = false
                            }
                        )
                    }
                }
                else -> {
                    PoseXApp(
                        activeProfileId = activeProfile?.id,
                        onSettingsTapped = { showSettings = true },
                        onExitAppFlow = { }
                    )
                }
            }
        }
        is AppDestination.Loading -> Unit
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoseXTheme {
                PoseXApp()
            }
        }
    }
}
