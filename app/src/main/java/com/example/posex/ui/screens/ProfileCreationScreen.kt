package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.UserProfile
import com.example.posex.ui.theme.*
import java.util.UUID

@Composable
fun ProfileCreationScreen(
    onProfileCreated: (UserProfile) -> Unit,
    initialProfile: UserProfile? = null
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var age by remember { mutableStateOf(initialProfile?.age?.toString() ?: "") }
    var height by remember { mutableStateOf(initialProfile?.heightCm?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialProfile?.weightKg?.toString() ?: "") }
    var gender by remember { mutableStateOf(initialProfile?.gender ?: "Other") }

    var nameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = PoseXBackground,
        topBar = {
            Surface(color = PoseXBackground) {
                Text(
                    text = if (initialProfile == null) "CREATE PROFILE" else "UPDATE BIO-DATA",
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = PoseXSurface,
                border = androidx.compose.foundation.BorderStroke(2.dp, PoseXAccent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initialsForName(name),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = PoseXAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                color = PoseXSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("USER DETAILS", style = MaterialTheme.typography.labelSmall, color = PoseXAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = { Text("FULL NAME") },
                        isError = nameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    if (nameError != null) ErrorText(nameError!!)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("BIOMETRICS", style = MaterialTheme.typography.labelSmall, color = PoseXAccent)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("AGE") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = fieldColors()
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("HEIGHT (CM)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = fieldColors()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("WEIGHT (KG)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("GENDER CATEGORY", style = MaterialTheme.typography.labelSmall, color = PoseXAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            GenderToggle(
                                label = g.uppercase(),
                                selected = gender == g,
                                onClick = { gender = g },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (name.isBlank()) nameError = "IDENTIFICATION REQUIRED"
                    else {
                        onProfileCreated(
                            UserProfile(
                                id = initialProfile?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                age = age.toIntOrNull() ?: 0,
                                heightCm = height.toFloatOrNull() ?: 0f,
                                weightKg = weight.toFloatOrNull() ?: 0f,
                                gender = gender,
                                avatarUri = null,
                                createdAt = initialProfile?.createdAt ?: System.currentTimeMillis(),
                                lastActiveAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PoseXAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (initialProfile == null) "SAVE PROFILE" else "SAVE CHANGES",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = PoseXBackground)
                )
            }
        }
    }
}

@Composable
fun GenderToggle(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (selected) PoseXAccent else PoseXBackground.copy(alpha = 0.5f),
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, PoseXOnSurface.copy(alpha = 0.2f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) PoseXBackground else PoseXOnSurface
                )
            )
        }
    }
}

@Composable
fun ErrorText(text: String) {
    Text(text, color = PoseXError, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PoseXAccent,
    unfocusedBorderColor = PoseXOnSurface.copy(alpha = 0.3f),
    focusedLabelColor = PoseXAccent,
    unfocusedLabelColor = PoseXOnSurface,
    cursorColor = PoseXAccent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

private fun initialsForName(name: String): String {
    return name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it[0].uppercase() }
        .joinToString("")
}
