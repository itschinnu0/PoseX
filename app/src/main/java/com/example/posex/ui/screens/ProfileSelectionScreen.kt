package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.UserProfile
import com.example.posex.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileSelectionScreen(
    profiles: List<UserProfile>,
    activeProfileId: String?,
    onProfileSelected: (UserProfile) -> Unit,
    onAddNew: () -> Unit
) {
    Scaffold(
        containerColor = PoseXBackground,
        topBar = {
            Surface(color = PoseXBackground) {
                Text(
                    "SELECT PROFILE",
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(24.dp),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = PoseXAccent,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNew,
                containerColor = PoseXAccent,
                contentColor = PoseXBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            items(profiles) { profile ->
                ProfileSelectionCard(
                    profile = profile,
                    isActive = profile.id == activeProfileId,
                    onClick = { onProfileSelected(profile) }
                )
            }
        }
    }
}

@Composable
fun ProfileSelectionCard(profile: UserProfile, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (isActive) PoseXAccent.copy(alpha = 0.1f) else PoseXSurface,
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, PoseXAccent) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (isActive) PoseXAccent else PoseXBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initialsForName(profile.name),
                        color = if (isActive) PoseXBackground else PoseXAccent,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    profile.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    "LINKED: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(profile.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PoseXOnSurface
                )
            }
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
