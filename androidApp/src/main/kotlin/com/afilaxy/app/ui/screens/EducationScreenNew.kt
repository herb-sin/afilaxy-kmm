package com.afilaxy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreenNew(
    onNavigateBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("basics") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Educação sobre Asma",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeCard()
            }
            
            item {
                CategoryFilters(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            
            item {
                when (selectedCategory) {
                    "basics" -> BasicsContent()
                    "medications" -> MedicationsContent()
                    "triggers" -> TriggersContent()
                    "emergency" -> EmergencyContent()
                    "lifestyle" -> LifestyleContent()
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Aprenda sobre Asma",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Conhecimento é poder! Entenda melhor sua condição e aprenda a viver bem com asma.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryFilters(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column {
        Text(
            "Categorias",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "basics" to "Básico",
                "medications" to "Medicamentos",
                "triggers" to "Gatilhos",
                "emergency" to "Emergência",
                "lifestyle" to "Estilo de Vida"
            )
            
            items(categories) { (key, label) ->
                FilterChip(
                    onClick = { onCategorySelected(key) },
                    label = { Text(label) },
                    selected = selectedCategory == key
                )
            }
        }
    }
}

@Composable
private fun BasicsContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EducationCard(
            title = "O que é Asma?",
            icon = Icons.Default.Info,
            content = "A asma é uma doença crônica que afeta as vias respiratórias, causando inflamação e estreitamento dos brônquios. Isso dificulta a passagem do ar, causando sintomas como falta de ar, chiado no peito e tosse."
        )
        
        EducationCard(
            title = "Sintomas Principais",
            icon = Icons.Default.Warning,
            content = "• Falta de ar ou dificuldade para respirar\n• Chiado no peito (sibilância)\n• Tosse, especialmente à noite\n• Sensação de aperto no peito\n• Cansaço durante atividades físicas"
        )
        
        EducationCard(
            title = "Tipos de Asma",
            icon = Icons.Default.Category,
            content = "• Asma alérgica: Causada por alérgenos\n• Asma não-alérgica: Causada por irritantes\n• Asma ocupacional: Relacionada ao trabalho\n• Asma induzida por exercício\n• Asma noturna: Sintomas piores à noite"
        )
    }
}

@Composable
private fun MedicationsContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EducationCard(
            title = "Medicamentos de Resgate",
            icon = Icons.Default.LocalHospital,
            iconColor = Color(0xFFE53E3E),
            content = "São broncodilatadores de ação rápida (beta-agonistas) usados durante crises:\n\n• Salbutamol (Aerolin®)\n• Fenoterol (Berotec®)\n\nUSO: Apenas durante crises ou antes de exercícios. NÃO use diariamente!"
        )
        
        EducationCard(
            title = "Medicamentos de Manutenção",
            icon = Icons.Default.Schedule,
            iconColor = Color(0xFF38A169),
            content = "São anti-inflamatórios usados diariamente para prevenir crises:\n\n• Corticoides inalatórios\n• Broncodilatadores de longa duração\n• Antileucotrienos\n\nUSO: Todos os dias, mesmo sem sintomas!"
        )
        
        EducationCard(
            title = "Como Usar o Inalador",
            icon = Icons.Default.PlayArrow,
            content = "1. Retire a tampa e agite o inalador\n2. Expire completamente\n3. Coloque os lábios ao redor do bocal\n4. Inspire profundamente e pressione\n5. Segure a respiração por 10 segundos\n6. Expire lentamente"
        )
        
        ImportantNote(
            text = "⚠️ IMPORTANTE: Medicamentos de resgate são para emergências. Se você usa mais de 2x por semana, procure seu médico!"
        )
    }
}

@Composable
private fun TriggersContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EducationCard(
            title = "Alérgenos Comuns",
            icon = Icons.Default.Pets,
            content = "• Ácaros da poeira\n• Pelos de animais\n• Pólen de plantas\n• Fungos e mofo\n• Baratas e seus dejetos"
        )
        
        EducationCard(
            title = "Irritantes Ambientais",
            icon = Icons.Default.Air,
            content = "• Fumaça de cigarro\n• Poluição do ar\n• Produtos de limpeza\n• Perfumes e sprays\n• Tinta fresca\n• Ar frio ou seco"
        )
        
        EducationCard(
            title = "Outros Gatilhos",
            icon = Icons.Default.MoreHoriz,
            content = "• Exercícios intensos\n• Estresse emocional\n• Infecções respiratórias\n• Refluxo gastroesofágico\n• Alguns medicamentos\n• Mudanças climáticas"
        )
        
        EducationCard(
            title = "Como Evitar Gatilhos",
            icon = Icons.Default.Shield,
            content = "• Mantenha a casa limpa e arejada\n• Use capas antialérgicas\n• Evite tapetes e cortinas pesadas\n• Não fume e evite fumantes\n• Use máscara em locais poluídos\n• Controle a umidade (40-60%)"
        )
    }
}

@Composable
private fun EmergencyContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EducationCard(
            title = "Sinais de Crise Grave",
            icon = Icons.Default.Emergency,
            iconColor = Color(0xFFE53E3E),
            content = "🚨 PROCURE AJUDA IMEDIATAMENTE:\n\n• Dificuldade extrema para respirar\n• Não consegue falar frases completas\n• Lábios ou unhas azulados\n• Medicamento de resgate não faz efeito\n• Confusão mental ou sonolência"
        )
        
        EducationCard(
            title = "O que Fazer na Crise",
            icon = Icons.Default.MedicalServices,
            content = "1. Mantenha a calma\n2. Use o medicamento de resgate\n3. Sente-se ereto, não se deite\n4. Respire lentamente\n5. Se não melhorar em 15 min, procure ajuda\n6. Ligue 192 (SAMU) se necessário"
        )
        
        EducationCard(
            title = "Kit de Emergência",
            icon = Icons.Default.LocalPharmacy,
            content = "Sempre tenha com você:\n\n• Medicamento de resgate\n• Lista de medicamentos\n• Contatos de emergência\n• Plano de ação da asma\n• Documento de identidade"
        )
        
        ImportantNote(
            text = "📱 Use o Afilaxy para encontrar ajuda rápida durante uma crise!"
        )
    }
}

@Composable
private fun LifestyleContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EducationCard(
            title = "Exercícios e Asma",
            icon = Icons.Default.FitnessCenter,
            content = "• Exercite-se regularmente, mas com cuidado\n• Faça aquecimento antes\n• Use medicamento preventivo se prescrito\n• Prefira atividades como natação\n• Evite exercícios em dias frios e secos\n• Pare se sentir sintomas"
        )
        
        EducationCard(
            title = "Alimentação Saudável",
            icon = Icons.Default.Restaurant,
            content = "• Mantenha peso adequado\n• Coma frutas e vegetais ricos em antioxidantes\n• Evite alimentos que causam refluxo\n• Beba bastante água\n• Evite conservantes se for alérgico\n• Considere suplementos (com orientação médica)"
        )
        
        EducationCard(
            title = "Sono e Descanso",
            icon = Icons.Default.Bedtime,
            content = "• Durma 7-8 horas por noite\n• Mantenha o quarto limpo e arejado\n• Use travesseiros antialérgicos\n• Evite animais no quarto\n• Eleve a cabeceira se tem refluxo\n• Mantenha horários regulares"
        )
        
        EducationCard(
            title = "Controle do Estresse",
            icon = Icons.Default.SelfImprovement,
            content = "• Pratique técnicas de relaxamento\n• Faça meditação ou yoga\n• Mantenha hobbies prazerosos\n• Converse com amigos e família\n• Procure ajuda psicológica se necessário\n• Aprenda técnicas de respiração"
        )
    }
}

@Composable
private fun EducationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
        }
    }
}

@Composable
private fun ImportantNote(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}