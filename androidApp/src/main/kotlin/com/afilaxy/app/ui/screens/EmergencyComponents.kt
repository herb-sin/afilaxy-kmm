package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Triagem passiva: helper confirma que tem o medicamento antes de aceitar.
// Reduz respostas de voluntários sem o Salbutamol, aumentando a efetividade.
@Composable
internal fun InhalerConfirmationCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF3CD)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💊 Você tem a bombinha com",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF856404),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sulfato de Salbutamol com você?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF856404),
                textAlign = TextAlign.Center
            )
        }
    }
}
