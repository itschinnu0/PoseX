package com.example.posex.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.ProfileStorageService
import com.example.posex.data.UserProfile
import com.example.posex.ui.theme.*

@Composable
fun SettingsScreen(
    activeProfile: UserProfile?,
    profiles: List<UserProfile>,
    profileStorageService: ProfileStorageService,
    onEditProfile: (UserProfile) -> Unit,
    onSelectProfile: (UserProfile) -> Unit,
    onCreateNewProfile: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = PoseXBackground,
        topBar = {
            Surface(color = PoseXBackground) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            item {
                SectionHeader("ACTIVE PROFILE")
                activeProfile?.let { profile ->
                    ProfileCard(
                        profile = profile,
                        action = {
                            IconButton(onClick = { onEditProfile(profile) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PoseXAccent)
                            }
                        },
                        onClick = {}
                    )
                }
            }

            item {
                SectionHeader("SAVED PROFILES")
            }

            items(profiles.filter { it.id != activeProfile?.id }) { profile ->
                DeletableProfileCard(
                    profile = profile,
                    onDelete = { profileStorageService.deleteProfile(profile.id) },
                    onClick = { onSelectProfile(profile) }
                )
            }

            item {
                Button(
                    onClick = onCreateNewProfile,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PoseXSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PoseXAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE NEW PROFILE", color = PoseXAccent, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = PoseXAccent,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun ProfileCard(profile: UserProfile, action: @Composable (() -> Unit)?, onClick: () -> Unit) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(profile, 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("BIO: ${profile.age}Y | ${profile.heightCm}CM | ${profile.weightKg}KG", style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
            }
            action?.invoke()
        }
    }
}

@Composable
fun DeletableProfileCard(profile: UserProfile, onDelete: () -> Unit, onClick: () -> Unit) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(profile, 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name.uppercase(), style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PoseXError.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AvatarCircle(profile: UserProfile, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = PoseXBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, PoseXAccent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                initialsForName(profile.name),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = PoseXAccent)
            )
        }
    }
}

private fun initialsForName(name: String): String {
    return name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it[0].uppercase() }
        .joinToString("")
}
