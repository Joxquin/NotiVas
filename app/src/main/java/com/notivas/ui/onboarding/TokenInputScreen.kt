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
fun TokenInputScreen(
    token: String,
    onTokenChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Token de Acceso",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Genera un token en la configuración de tu perfil de Canvas y pégalo aquí.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("Access Token") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(16.dp),
            enabled = token.isNotBlank()
        ) {
            Text("Verificar Conexión", style = MaterialTheme.typography.titleMedium)
        }
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Regresar")
        }
    }
}
