package com.afilaxy.app.ui.screens

import androidx.compose.animation.*
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
import androidx.navigation.NavController

// ---------------------------------------------------------------------------
// AutocuidadoScreen — conteúdo mesclado de Autocuidado + Educação
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocuidadoScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf("basics") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autocuidado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            item { AutocuidadoHeroCard() }
            item {
                AutocuidadoCategoryFilters(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            item {
                when (selectedCategory) {
                    "basics"      -> AutocuidadoBasicsContent()
                    "breathing"   -> AutocuidadoBreathingContent()
                    "environment" -> AutocuidadoEnvironmentContent()
                    "support"     -> AutocuidadoSupportContent()
                    "lifestyle"   -> AutocuidadoLifestyleContent()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hero Card
// ---------------------------------------------------------------------------

@Composable
private fun AutocuidadoHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Cuide-se com conhecimento",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Pequenos hábitos fazem grande diferença. Informação e autocuidado andam juntos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Category Filters
// ---------------------------------------------------------------------------

@Composable
private fun AutocuidadoCategoryFilters(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "basics"      to "Bem-Estar",
        "breathing"   to "Respiração",
        "environment" to "Ambiente",
        "support"     to "Suporte",
        "lifestyle"   to "Hábitos"
    )
    Column {
        Text(
            "Categorias",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

// ---------------------------------------------------------------------------
// Category Content Sections
// ---------------------------------------------------------------------------

@Composable
private fun AutocuidadoBasicsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AutocuidadoInfoCard(
            title = "O que é Bem-Estar?",
            icon = Icons.Default.Info,
            content = "Bem-estar é o equilíbrio entre corpo e mente no dia a dia. " +
                      "Envolve qualidade do sono, energia, humor, alimentação e como " +
                      "você se sente ao longo da semana."
        )
        AutocuidadoInfoCard(
            title = "Sinais de Atenção",
            icon = Icons.Default.Warning,
            content = "• Cansaço excessivo por vários dias seguidos\n" +
                      "• Dificuldade para dormir ou acordar bem\n" +
                      "• Falta de energia para atividades do dia a dia\n" +
                      "• Mudanças bruscas de humor\n" +
                      "• Sensação frequente de mal-estar"
        )
        AutocuidadoInfoCard(
            title = "Quando Buscar Ajuda",
            icon = Icons.Default.Shield,
            content = "Se você se sentir mal por mais de alguns dias, consulte um profissional de saúde. " +
                      "O Afilaxy conecta você com pessoas próximas em momentos de urgência, " +
                      "mas não substitui orientação profissional."
        )
        AutocuidadoInfoCard(
            title = "Acompanhe seu Progresso",
            icon = Icons.Default.Category,
            content = "Use o check-in diário para registrar como você se sente. " +
                      "Com o tempo, você verá padrões e entenderá melhor o que afeta seu bem-estar."
        )
    }
}

@Composable
private fun AutocuidadoBreathingContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AutocuidadoInfoCard(
            title = "Respiração Diafragmática",
            icon = Icons.Default.SelfImprovement,
            content = "1. Deite ou sente confortavelmente\n" +
                      "2. Coloque uma mão no peito e outra no abdômen\n" +
                      "3. Inspire pelo nariz por 4 segundos, expandindo o abdômen\n" +
                      "4. Segure por 2 segundos\n" +
                      "5. Expire lentamente pela boca por 6 segundos\n" +
                      "6. Repita por 5 a 10 minutos"
        )
        AutocuidadoInfoCard(
            title = "Técnica 4-7-8",
            icon = Icons.Default.Timer,
            content = "Ótima para reduzir estresse e melhorar o sono:\n\n" +
                      "• Inspire pelo nariz contando até 4\n" +
                      "• Segure a respiração contando até 7\n" +
                      "• Expire pela boca contando até 8\n\n" +
                      "Repita 4 vezes. Pratique 2× ao dia."
        )
        AutocuidadoInfoCard(
            title = "Respiração Lenta (Coerência Cardíaca)",
            icon = Icons.Default.Favorite,
            content = "• Inspire por 5 segundos\n" +
                      "• Expire por 5 segundos\n" +
                      "• Ritmo de 6 respirações por minuto\n" +
                      "• Duração: 5 minutos\n\n" +
                      "Reduz ansiedade e melhora a variabilidade cardíaca."
        )
        AutocuidadoInfoCard(
            title = "Quando Praticar",
            icon = Icons.Default.Schedule,
            content = "• Ao acordar: ajuda a iniciar o dia com calma\n" +
                      "• Em momentos de estresse: recupera o equilíbrio\n" +
                      "• Antes de dormir: melhora a qualidade do sono\n" +
                      "• Em dias de má qualidade do ar: ajuda a manter a calma"
        )
    }
}

@Composable
private fun AutocuidadoEnvironmentContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AutocuidadoInfoCard(
            title = "Qualidade do Ar",
            icon = Icons.Default.Air,
            content = "A qualidade do ar afeta como você se sente. O Afilaxy mostra o índice " +
                      "AQI da sua região em tempo real:\n\n" +
                      "• AQI 0–50: Boa — atividades normais\n" +
                      "• AQI 51–100: Moderada — atenção se sensível\n" +
                      "• AQI 101–150: Prejudicial a grupos sensíveis\n" +
                      "• AQI 151+: Evite exposição prolongada ao ar livre"
        )
        AutocuidadoInfoCard(
            title = "Fatores Ambientais Comuns",
            icon = Icons.Default.Pets,
            content = "• Poeira e ácaros: mantenha ambientes limpos e arejados\n" +
                      "• Pelos de animais: higiene regular dos pets\n" +
                      "• Mofo e umidade: controle entre 40–60%\n" +
                      "• Pólen: dias ventosos podem aumentar a concentração\n" +
                      "• Fumaça: evite ambientes com fumaça de qualquer tipo"
        )
        AutocuidadoInfoCard(
            title = "Irritantes do Dia a Dia",
            icon = Icons.Default.MoreHoriz,
            content = "• Produtos de limpeza: prefira versões sem perfume forte\n" +
                      "• Perfumes e sprays: use com moderação\n" +
                      "• Ar condicionado: limpe os filtros regularmente\n" +
                      "• Ar frio e seco: hidrate-se mais nesses dias\n" +
                      "• Tráfego intenso: evite horários de pico quando possível"
        )
        AutocuidadoInfoCard(
            title = "Dicas para a Casa",
            icon = Icons.Default.Shield,
            content = "• Aspire e limpe com pano úmido regularmente\n" +
                      "• Mantenha janelas abertas nos horários de menor poluição\n" +
                      "• Use purificador de ar se o AQI local for frequentemente alto\n" +
                      "• Troque a roupa de cama semanalmente\n" +
                      "• Evite tapetes e cortinas que acumulam poeira"
        )
    }
}

@Composable
private fun AutocuidadoSupportContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AutocuidadoInfoCard(
            title = "Como o Afilaxy Ajuda",
            icon = Icons.Default.People,
            content = "Em momentos difíceis, você não precisa enfrentar sozinho:\n\n" +
                      "• Toque em Solicitar Ajuda para alertar pessoas próximas\n" +
                      "• Helpers voluntários próximos recebem uma notificação\n" +
                      "• Alguém disponível pode estar do seu lado em minutos\n\n" +
                      "O Afilaxy não substitui serviços de emergência."
        )
        AutocuidadoInfoCard(
            title = "Seja um Helper",
            icon = Icons.Default.Favorite,
            content = "Ative o Modo Ajudante e esteja disponível para ajudar quem precisar perto de você:\n\n" +
                      "• Receba notificações de pedidos de ajuda próximos\n" +
                      "• Aceite apenas se puder realmente comparecer\n" +
                      "• Aja com responsabilidade e empatia\n" +
                      "• Você pode desativar a qualquer momento"
        )
        AutocuidadoInfoCard(
            title = "Números de Emergência",
            icon = Icons.Default.Phone,
            iconColor = Color(0xFFE53E3E),
            content = "Sempre que houver risco à vida, ligue imediatamente:\n\n" +
                      "• SAMU: 192\n" +
                      "• Bombeiros: 193\n" +
                      "• Polícia: 190\n" +
                      "• CVV (apoio emocional): 188\n\n" +
                      "O Afilaxy é um apoio comunitário, não um serviço de emergência."
        )
        AutocuidadoAlertCard(text = "📱 Use o Afilaxy para acionar sua rede de apoio. Para emergências graves, ligue 192 (SAMU).")
    }
}

@Composable
private fun AutocuidadoLifestyleContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AutocuidadoInfoCard(
            title = "Exercício e Bem-Estar",
            icon = Icons.Default.FitnessCenter,
            content = "• Exercite-se regularmente — 30 minutos por dia faz diferença\n" +
                      "• Faça aquecimento antes de qualquer atividade\n" +
                      "• Prefira atividades como caminhada, natação ou yoga\n" +
                      "• Em dias de baixa qualidade do ar, prefira atividades indoor\n" +
                      "• Pare se sentir qualquer desconforto e descanse"
        )
        AutocuidadoInfoCard(
            title = "Alimentação Saudável",
            icon = Icons.Default.Restaurant,
            content = "• Mantenha peso adequado\n• Coma frutas e vegetais ricos em antioxidantes\n" +
                      "• Evite alimentos que causam refluxo\n• Beba bastante água\n" +
                      "• Evite conservantes se for alérgico"
        )
        AutocuidadoInfoCard(
            title = "Sono e Descanso",
            icon = Icons.Default.Bedtime,
            content = "• Durma 7-8 horas por noite\n• Mantenha o quarto limpo e arejado\n" +
                      "• Use travesseiros antialérgicos\n• Evite animais no quarto\n" +
                      "• Eleve a cabeceira se tem refluxo"
        )
        AutocuidadoInfoCard(
            title = "Controle do Estresse",
            icon = Icons.Default.SelfImprovement,
            content = "• Pratique técnicas de relaxamento\n• Faça meditação ou yoga\n" +
                      "• Mantenha hobbies prazerosos\n• Converse com amigos e família\n" +
                      "• Procure ajuda psicológica se necessário\n• Aprenda técnicas de respiração"
        )
    }
}

// ---------------------------------------------------------------------------
// Shared UI Components
// ---------------------------------------------------------------------------

@Composable
private fun AutocuidadoInfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
private fun AutocuidadoAlertCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
