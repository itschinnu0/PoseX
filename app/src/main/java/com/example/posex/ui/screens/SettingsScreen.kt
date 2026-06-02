package com.example.posex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.posex.data.ProfileStorageService
import com.example.posex.data.UserProfile

@Composable
fun SettingsScreen(
    activeProfile: UserProfile?,
    allProfiles: List<UserProfile>,
    profileStorageService: ProfileStorageService,
    onProfileSwitched: (UserProfile) -> Unit,
    onEditProfile: (UserProfile) -> Unit,
    onCreateNewProfile: () -> Unit,
    onBack: () -> Unit
) {
    var profiles by remember { mutableStateOf(allProfiles) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF112233)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "←", color = Color.White, fontSize = 16.sp)
            }
            Text(
                text = "Settings",
                color = Color(0xFF00E5FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 44.dp)
            )
        }

        SectionDivider()

        SectionHeader("Active Profile")

        if (activeProfile == null) {
            Text(
                text = "No profile selected",
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp
            )
        } else {
            ProfileCard(
                profile = activeProfile,
                trailingContent = {
                    OutlinedButton(
                        onClick = { onEditProfile(activeProfile) },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Edit")
                    }
                },
                onClick = {}
            )
        }

        SectionDivider()
        val otherProfiles = profiles.filter { it.id != activeProfile?.id }
        if (otherProfiles.isEmpty()) {
        SectionHeader("Switch Profile")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                otherProfiles.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        onClick = {
                            profileStorageService.setActiveProfile(profile.id)
                            onProfileSwitched(profile)
                        }
                    )
                }
            }
        }

        SectionDivider()

        SectionHeader("Manage Profiles")

        Button(
            onClick = onCreateNewProfile,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Create New Profile",
                color = Color(0xFF0A0F1E),
                fontWeight = FontWeight.Bold
            )
        }

        if (profiles.size >= 2) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfile?.id
                    DeletableProfileCard(
                        profile = profile,
                        isDeleteEnabled = !isActive,
                        onDelete = {
                            if (!isActive) {
                                profileStorageService.deleteProfile(profile.id)
                                profiles = profiles.filterNot { it.id == profile.id }
                            }
                        }
                    )
                }
            }
        }

        SectionDivider()

        SectionHeader("About")

        InfoRow(label = "Version", value = "1.0.0")
        InfoRow(label = "Build", value = "MVP")
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF1B2B40))
    )
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF112233), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(profile = profile, size = 56.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${profile.age} · ${profile.gender}",
                color = Color(0xFFB0BEC5),
                fontSize = 13.sp
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
private fun DeletableProfileCard(
    profile: UserProfile,
    isDeleteEnabled: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF112233), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(profile = profile, size = 48.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${profile.age} · ${profile.gender}",
                color = Color(0xFFB0BEC5),
                fontSize = 13.sp
            )
        }
        Text(
            text = "🗑",
            color = if (isDeleteEnabled) Color(0xFFFF5252) else Color(0xFF555555),
            fontSize = 18.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .let { base ->
                    if (isDeleteEnabled) base.clickable { onDelete() } else base
                }
        )
    }
}

@Composable
private fun AvatarCircle(profile: UserProfile, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF112233))
            .border(1.dp, Color(0xFF1B2B40), CircleShape)
    ) {
        if (!profile.avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = profile.avatarUri,
                contentDescription = "Profile avatar",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initialsForName(profile.name),
                color = Color(0xFF00E5FF),
                fontSize = if (size >= 56.dp) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFB0BEC5), fontSize = 13.sp)
        Text(text = value, color = Color(0xFFB0BEC5), fontSize = 13.sp)
    }
}

private fun initialsForName(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "--"
    if (parts.size == 1) {
        val word = parts.first().uppercase()
        return word.take(2)
    }
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}

