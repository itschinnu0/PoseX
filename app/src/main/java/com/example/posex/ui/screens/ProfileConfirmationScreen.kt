package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.UserProfile
import com.example.posex.ui.theme.*

@Composable
fun ProfileConfirmationScreen(
    profile: UserProfile,
    onContinue: () -> Unit,
    onSwitch: () -> Unit,
    onCreateNew: () -> Unit
) {
    Scaffold(
        containerColor = PoseXBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "WELCOME BACK",
                style = MaterialTheme.typography.labelMedium,
                color = PoseXAccent,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = PoseXSurface,
                border = androidx.compose.foundation.BorderStroke(2.dp, PoseXAccent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initialsForName(profile.name),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = PoseXAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                profile.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PoseXAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "CONTINUE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = PoseXBackground)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSwitch,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PoseXOnSurface.copy(alpha = 0.3f))
                ) {
                    Text("SWITCH", color = PoseXOnSurface)
                }
                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PoseXOnSurface.copy(alpha = 0.3f))
                ) {
                    Text("NEW", color = PoseXOnSurface)
                }
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
