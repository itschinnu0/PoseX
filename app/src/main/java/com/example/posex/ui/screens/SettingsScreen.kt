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
import com.example.posex.data.BmiCategory
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(PoseXSurface, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "PROFILE SETTINGS",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 16.dp)
        ) {
            // Active Profile Section
            item {
                SectionHeader("ACTIVE OPERATOR")
                activeProfile?.let { profile ->
                    ActiveProfileCard(
                        profile = profile,
                        onEdit = { onEditProfile(profile) }
                    )
                } ?: run {
                    EmptyProfileCard(onCreateNewProfile)
                }
            }

            // Saved Profiles Section
            item {
                SectionHeader("PROFILE REGISTRY")
            }

            val otherProfiles = profiles.filter { it.id != activeProfile?.id }
            if (otherProfiles.isEmpty()) {
                item {
                    Text(
                        "NO ADDITIONAL PROFILES DETECTED",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoseXOnSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(otherProfiles) { profile ->
                    RegistryProfileCard(
                        profile = profile,
                        onDelete = { profileStorageService.deleteProfile(profile.id) },
                        onClick = { onSelectProfile(profile) }
                    )
                }
            }

            // Add New Profile Button
            item {
                Button(
                    onClick = onCreateNewProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PoseXAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PoseXBackground)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "INITIALIZE NEW PROFILE",
                        color = PoseXBackground,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        ),
        color = PoseXAccent,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ActiveProfileCard(profile: UserProfile, onEdit: () -> Unit) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(2.dp, PoseXAccent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(profile, 64.dp)
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "BIO: ${profile.age}Y | ${profile.heightCm.toInt()}CM | ${profile.weightKg.toInt()}KG",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoseXOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "BMI: ${"%.1f".format(profile.currentBmi)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PoseXAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HealthStatusBadge(profile.healthStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.background(PoseXAccent.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PoseXAccent)
            }
        }
    }
}

@Composable
fun RegistryProfileCard(profile: UserProfile, onDelete: () -> Unit, onClick: () -> Unit) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(profile, 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${profile.heightCm.toInt()}cm • ${profile.weightKg.toInt()}kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoseXOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PoseXError.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun EmptyProfileCard(onClick: () -> Unit) {
    Surface(
        color = PoseXSurface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, PoseXOnSurface.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("NO ACTIVE PROFILE. TAP TO INITIALIZE.", color = PoseXOnSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun HealthStatusBadge(status: BmiCategory) {
    val (color, label) = when (status) {
        BmiCategory.NORMAL -> PoseXSuccess to "FIT"
        BmiCategory.UNDERWEIGHT -> Color(0xFFFFB300) to "UNDERWEIGHT"
        else -> PoseXError to status.label.uppercase()
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = color
            )
        )
    }
}

@Composable
fun AvatarCircle(profile: UserProfile, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = PoseXBackground,
        border = androidx.compose.foundation.BorderStroke(2.dp, PoseXAccent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                initialsForName(profile.name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black, 
                    color = PoseXAccent,
                    fontSize = if (size > 50.dp) 20.sp else 14.sp
                )
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
