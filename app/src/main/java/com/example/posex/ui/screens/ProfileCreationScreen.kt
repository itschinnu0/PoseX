package com.example.posex.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.posex.data.UserProfile
import java.util.UUID

@Composable
fun ProfileCreationScreen(
    onProfileCreated: (UserProfile) -> Unit,
    existingProfile: UserProfile? = null
) {
    val isEditMode = existingProfile != null

    var name by remember { mutableStateOf(existingProfile?.name.orEmpty()) }
    var ageText by remember { mutableStateOf(existingProfile?.age?.toString().orEmpty()) }
    var gender by remember { mutableStateOf(existingProfile?.gender.orEmpty()) }
    var weightText by remember { mutableStateOf(existingProfile?.weightKg?.takeIf { it > 0 }?.toString().orEmpty()) }
    var heightText by remember { mutableStateOf(existingProfile?.heightCm?.takeIf { it > 0 }?.toString().orEmpty()) }
    var avatarUri by remember { mutableStateOf(existingProfile?.avatarUri) }

    val scrollState = rememberScrollState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri?.toString()
    }

    val nameError = if (name.isBlank()) "Name is required" else null
    val ageValue = ageText.toIntOrNull()
    val ageError = when {
        ageText.isBlank() -> "Age is required"
        ageValue == null -> "Enter a valid age"
        ageValue !in 10..100 -> "Age must be between 10 and 100"
        else -> null
    }
    val genderError = if (gender.isBlank()) "Select a gender" else null

    val weightValue = weightText.toFloatOrNull()
    val weightError = when {
        weightText.isBlank() -> null
        weightValue == null -> "Enter a valid weight"
        weightValue <= 0f -> "Weight must be positive"
        else -> null
    }

    val heightValue = heightText.toFloatOrNull()
    val heightError = when {
        heightText.isBlank() -> null
        heightValue == null -> "Enter a valid height"
        heightValue <= 0f -> "Height must be positive"
        else -> null
    }

    val canSave = nameError == null && ageError == null && genderError == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditMode) "Edit Profile" else "Create Profile",
                color = Color(0xFF00E5FF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF112233))
                    .border(BorderStroke(1.dp, Color(0xFF1B2B40)), CircleShape)
                    .clickable { pickerLauncher.launch("image/*") }
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Profile avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = initialsForName(name),
                        color = Color(0xFF00E5FF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "📷",
                        color = Color(0xFF0A0F1E),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to change photo",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF112233), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    if (nameError != null) {
                        ErrorText(nameError)
                    }

                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Age") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    if (ageError != null) {
                        ErrorText(ageError)
                    }

                    Text(text = "Gender", color = Color.White, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GenderToggle(
                            label = "Male",
                            isSelected = gender == "Male",
                            onClick = { gender = "Male" },
                            modifier = Modifier.weight(1f)
                        )
                        GenderToggle(
                            label = "Female",
                            isSelected = gender == "Female",
                            onClick = { gender = "Female" },
                            modifier = Modifier.weight(1f)
                        )
                        GenderToggle(
                            label = "Other",
                            isSelected = gender == "Other",
                            onClick = { gender = "Other" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (genderError != null) {
                        ErrorText(genderError)
                    }

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    if (weightError != null) {
                        ErrorText(weightError)
                    }

                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                    if (heightError != null) {
                        ErrorText(heightError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val profile = if (existingProfile == null) {
                        UserProfile(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            age = ageValue ?: 0,
                            gender = gender,
                            weightKg = weightValue ?: 0f,
                            heightCm = heightValue ?: 0f,
                            avatarUri = avatarUri,
                            createdAt = now,
                            lastActiveAt = now
                        )
                    } else {
                        existingProfile.copy(
                            name = name.trim(),
                            age = ageValue ?: existingProfile.age,
                            gender = gender,
                            weightKg = weightValue ?: 0f,
                            heightCm = heightValue ?: 0f,
                            avatarUri = avatarUri,
                            lastActiveAt = now
                        )
                    }
                    onProfileCreated(profile)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Save",
                    color = Color(0xFF0A0F1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GenderToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isSelected) Color(0xFF00E5FF) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF0A0F1E) else Color.White

    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = background),
        modifier = modifier
    ) {
        Text(text = label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = Color(0xFFFF5252),
        fontSize = 12.sp
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White
)

private fun initialsForName(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "--"
    if (parts.size == 1) {
        val word = parts.first().uppercase()
        return word.take(2)
    }
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}
