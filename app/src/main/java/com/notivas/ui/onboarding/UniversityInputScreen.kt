package com.notivas.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversityInputScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Bienvenido a NotiVas",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 44.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ingresa el dominio de tu institución para comenzar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("Dominio (ej: canvas.instructure.com)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(16.dp),
            enabled = url.isNotBlank()
        ) {
            Text("Siguiente", style = MaterialTheme.typography.titleMedium)
        }
    }
}
