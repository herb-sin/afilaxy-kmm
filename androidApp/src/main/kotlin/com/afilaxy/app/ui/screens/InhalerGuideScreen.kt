package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Indicação do "Guia Prático de Dispositivos Inalatórios" (LEDAR/UFAL Arapiraca), autorizada
 * pela coordenação do LEDAR por e-mail em 14/11/2026 nestas condições:
 *  - só link para a página oficial (DOI), sem hospedar cópia (licença CC BY-NC-ND 4.0);
 *  - crédito de AUTORIA, nunca apresentar autores, LEDAR ou UFAL como parceiros/apoiadores;
 *  - material educativo complementar, reforçando a orientação do médico;
 *  - acesso sempre gratuito, sem vínculo com conteúdo ou plano pago.
 * Espelho no iOS: InhalerGuideView.swift. Mesmo texto no site (página /guia-inalatorio).
 */
object InhalerGuide {
    const val URL = "https://doi.org/10.46898/rfb.musghvgdt331"
    const val TITLE = "Guia Prático de Dispositivos Inalatórios"
    const val SUBTITLE = "Principais dispositivos utilizados no Brasil no manejo das doenças pulmonares"
    const val CONTENT = "Passo a passo ilustrado da técnica correta de uso dos principais " +
        "dispositivos inalatórios disponíveis no Brasil, com cuidados de higienização e " +
        "conservação e técnicas de lavagem nasal."
    const val NOTICE = "Material educativo complementar. Ele não substitui a orientação do seu " +
        "médico: siga sempre a prescrição, as doses e as instruções que você recebeu."
    const val AUTHORS = "Ana Dora Alécio Virtuoso Costa, Emylle Victória Cavalcante Costa, " +
        "Lívia Soares Bezerra, Jorge Samuel de Oliveira Júnior, Katiane de Lima Pinheiro, " +
        "Ryan Lucas Barbosa Oliveira, Thaysa Karlla Albuquerque Jatobá, Thalyta de Souza " +
        "Rodrigues Holanda e Laércia Karla Diega Paiva Ferreira."
    const val LAB = "Laboratório de Epidemiologia de Doenças Alérgicas e Respiratórias " +
        "(LEDAR), Universidade Federal de Alagoas (UFAL), Campus Arapiraca."
    const val REFERENCE = "Belém: RFB Editora, 2026. ISBN 978-65-5337-373-0. " +
        "DOI 10.46898/rfb.musghvgdt331."
    const val LICENSE = "Distribuído gratuitamente pelos autores sob a licença Creative " +
        "Commons CC BY-NC-ND 4.0. O Afilaxy apenas indica o guia, com autorização da autoria; " +
        "os autores, o LEDAR e a UFAL não têm vínculo com o Afilaxy."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InhalerGuideScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Como usar sua bombinha") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(InhalerGuide.TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                InhalerGuide.SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(InhalerGuide.CONTENT, style = MaterialTheme.typography.bodyLarge)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        InhalerGuide.NOTICE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Button(
                onClick = { uriHandler.openUri(InhalerGuide.URL) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir o guia na página oficial")
            }
            Text(
                "Abre no navegador, na página da obra na RFB Editora.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text("Autoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(InhalerGuide.AUTHORS, style = MaterialTheme.typography.bodyMedium)
            Text(InhalerGuide.LAB, style = MaterialTheme.typography.bodyMedium)
            Text(
                InhalerGuide.REFERENCE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                InhalerGuide.LICENSE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
