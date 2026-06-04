package com.example.posex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.posex.data.*
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.WorkoutConfig
import com.example.posex.ui.screens.*
import com.example.posex.ui.theme.*

enum class HomeFlow { HOME, CONFIG, WORKOUT, SUMMARY }

sealed class AppDestination {
    object Loading : AppDestination()
    object CreateProfile : AppDestination()
    data class EditProfile(val profile: UserProfile) : AppDestination()
    object ConfirmProfile : AppDestination()
    object SelectProfile : AppDestination()
    object MainApp : AppDestination()
}

@Composable
fun PoseXApp(
    activeProfile: UserProfile?,
    profiles: List<UserProfile>,
    onSelectProfile: (UserProfile) -> Unit,
    onSettingsTapped: () -> Unit,
    onExitAppFlow: () -> Unit
) {
    val context = LocalContext.current
    val storageService = remember(context) { StorageService(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var flowState by remember { mutableStateOf(HomeFlow.HOME) }
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var workoutConfig by remember { mutableStateOf<WorkoutConfig?>(null) }
    var lastSessionRecord by remember { mutableStateOf<SessionRecord?>(null) }
    var lastPersonalBest by remember { mutableStateOf<PersonalBest?>(null) }

    val showBottomBar = selectedTab == 1 || (selectedTab == 0 && flowState != HomeFlow.WORKOUT && flowState != HomeFlow.SUMMARY)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = PoseXBackground,
                    contentColor = PoseXAccent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("WORKOUTS") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PoseXBackground,
                            selectedTextColor = PoseXAccent,
                            indicatorColor = PoseXAccent,
                            unselectedIconColor = PoseXOnSurface,
                            unselectedTextColor = PoseXOnSurface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("HISTORY") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PoseXBackground,
                            selectedTextColor = PoseXAccent,
                            indicatorColor = PoseXAccent,
                            unselectedIconColor = PoseXOnSurface,
                            unselectedTextColor = PoseXOnSurface
                        )
                    )
                }
            }
        },
        containerColor = PoseXBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                1 -> StatsScreen(
                    storageService = storageService,
                    activeProfileId = activeProfile?.id,
                    onSettingsTapped = onSettingsTapped
                )
                else -> {
                    when (flowState) {
                        HomeFlow.HOME -> HomeScreen(onExerciseSelected = { exercise ->
                            selectedExercise = exercise
                            flowState = HomeFlow.CONFIG
                        })
                        HomeFlow.CONFIG -> {
                            selectedExercise?.let { exercise ->
                                WorkoutConfigScreen(
                                    exerciseType = exercise,
                                    activeProfile = activeProfile,
                                    onStartWorkout = { config ->
                                        workoutConfig = config
                                        flowState = HomeFlow.WORKOUT
                                    },
                                    onBack = {
                                        selectedExercise = null
                                        flowState = HomeFlow.HOME
                                    }
                                )
                            } ?: run { flowState = HomeFlow.HOME }
                        }
                        HomeFlow.WORKOUT -> {
                            val config = workoutConfig
                            val exercise = selectedExercise
                            if (exercise != null && config != null) {
                                WorkoutScreen(
                                    exerciseType = exercise,
                                    config = config,
                                    activeProfileId = activeProfile?.id,
                                    onExit = { sessionId ->
                                        val session = storageService.getAllSessions().firstOrNull { it.id == sessionId }
                                        lastSessionRecord = session
                                        lastPersonalBest = session?.let { storageService.getPersonalBest(it.exerciseType, it.profileId) }
                                        flowState = if (session != null) HomeFlow.SUMMARY else HomeFlow.HOME
                                    }
                                )
                            } else { flowState = HomeFlow.HOME }
                        }
                        HomeFlow.SUMMARY -> {
                            lastSessionRecord?.let { session ->
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
                            } ?: run { flowState = HomeFlow.HOME }
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

    PoseXTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                showSettings -> {
                    SettingsScreen(
                        activeProfile = activeProfile,
                        profiles = profiles,
                        profileStorageService = profileStorageService,
                        onEditProfile = { profile ->
                            appDestination = AppDestination.EditProfile(profile)
                            showSettings = false
                        },
                        onSelectProfile = { profile ->
                            profileStorageService.setActiveProfile(profile.id)
                            activeProfile = profile
                            showSettings = false
                        },
                        onCreateNewProfile = {
                            appDestination = AppDestination.CreateProfile
                            showSettings = false
                        },
                        onBack = { showSettings = false }
                    )
                }
                else -> {
                val dest = appDestination
                when (dest) {
                    AppDestination.CreateProfile -> {
                        ProfileCreationScreen(onProfileCreated = { profile ->
                            profileStorageService.saveProfile(profile)
                            profileStorageService.setActiveProfile(profile.id)
                            activeProfile = profile
                            profiles = profileStorageService.getAllProfiles()
                            appDestination = AppDestination.MainApp
                        })
                    }
                    is AppDestination.EditProfile -> {
                        ProfileCreationScreen(
                            initialProfile = dest.profile,
                            onProfileCreated = { profile ->
                                profileStorageService.saveProfile(profile)
                                if (activeProfile?.id == profile.id) {
                                    activeProfile = profile
                                }
                                profiles = profileStorageService.getAllProfiles()
                                appDestination = AppDestination.MainApp
                            }
                        )
                    }
                    AppDestination.ConfirmProfile -> {
                            activeProfile?.let { profile ->
                                ProfileConfirmationScreen(
                                    profile = profile,
                                    onContinue = { appDestination = AppDestination.MainApp },
                                    onSwitch = { appDestination = AppDestination.SelectProfile },
                                    onCreateNew = { appDestination = AppDestination.CreateProfile }
                                )
                            } ?: run { appDestination = AppDestination.CreateProfile }
                        }
                        AppDestination.SelectProfile -> {
                            ProfileSelectionScreen(
                                profiles = profiles,
                                activeProfileId = activeProfile?.id,
                                onProfileSelected = { profile ->
                                    profileStorageService.setActiveProfile(profile.id)
                                    activeProfile = profile
                                    appDestination = AppDestination.MainApp
                                },
                                onAddNew = { appDestination = AppDestination.CreateProfile }
                            )
                        }
                        AppDestination.MainApp -> {
                            PoseXApp(
                                activeProfile = activeProfile,
                                profiles = profiles,
                                onSelectProfile = { profile ->
                                    profileStorageService.setActiveProfile(profile.id)
                                    activeProfile = profile
                                },
                                onSettingsTapped = { showSettings = true },
                                onExitAppFlow = {
                                    // Refresh profiles if needed
                                    profiles = profileStorageService.getAllProfiles()
                                }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoseXApp()
        }
    }
}
