package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.domain.model.AsthmaControlLevel
import com.afilaxy.domain.model.AsthmaControlTestResult
import com.afilaxy.domain.model.daysSince
import com.afilaxy.domain.model.isRecent
import org.koin.androidx.compose.koinViewModel
import com.afilaxy.presentation.act.ActViewModel

// ---------------------------------------------------------------------------
// ACT (Asthma Control Test) — instrumento validado de 5 perguntas, janela
// retrospectiva de 4 semanas. Perguntas e opções reproduzidas literalmente
// do teste público de um pneumologista parceiro (Dr. Carlos Nogueira).
// Cortes (20-25/15-19/5-14) conferidos empiricamente contra a mesma fonte.
//
// Depois das 5 perguntas vêm 2 perguntas complementares de sim/não sobre crise
// grave nas mesmas 4 semanas (definição da GINA). Elas NÃO fazem parte do ACT e
// não entram no escore — alimentam o motor de risco e o relatório para o médico.
// ---------------------------------------------------------------------------

private data class ActQuestion(val text: String, val options: List<String>)

private val ACT_QUESTIONS = listOf(
    ActQuestion(
        "Nas últimas 4 semanas, quanto do tempo a asma prejudicou suas atividades no trabalho, escola ou em casa?",
        listOf("O tempo todo", "A maior parte do tempo", "Alguma parte do tempo", "Um pouco do tempo", "Nenhum tempo")
    ),
    ActQuestion(
        "Durante as últimas 4 semanas, quantas vezes você teve falta de ar?",
        listOf("Mais do que uma vez ao dia", "1 vez por dia", "3 a 6 vezes por semana", "1 ou 2 vezes por semana", "Nenhuma vez")
    ),
    ActQuestion(
        "Durante as últimas 4 semanas, com que frequência os sintomas de asma acordaram você à noite ou mais cedo do que o normal pela manhã?",
        listOf("4 ou mais noites por semana", "2 a 3 noites por semana", "1 vez por semana", "1 ou 2 vezes", "Nenhuma vez")
    ),
    ActQuestion(
        "Durante as últimas 4 semanas, com que frequência você usou medicação de alívio (bombinha ou nebulizador)?",
        listOf("3 ou mais vezes por dia", "1 ou 2 vezes por dia", "2 ou 3 vezes por semana", "1 vez por semana ou menos", "Nenhuma vez")
    ),
    ActQuestion(
        "Como você avaliaria o controle de sua asma nas últimas 4 semanas?",
        listOf("Nada controlada", "Pouco controlada", "Moderadamente controlada", "Bem controlada", "Totalmente controlada")
    )
)

private val SEVERE_EXACERBATION_QUESTIONS = listOf(
    "Nas últimas 4 semanas, você precisou tomar corticoide em comprimido ou xarope (ex.: prednisolona) por 3 dias ou mais por causa da asma?",
    "Nas últimas 4 semanas, você foi ao pronto-socorro/UPA ou ficou internado por causa da asma?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActScreen(
    navController: NavController,
    onNavigateToHistory: () -> Unit,
    viewModel: ActViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showQuestionnaire by rememberSaveable { mutableStateOf(false) }
    var showRecentWarning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadLatest() }

    // Refazer antes de 28 dias repete boa parte da mesma janela de 4 semanas do ACT.
    // Não bloqueia (ex.: depois de uma crise ou troca de remédio), só avisa.
    val startTest: () -> Unit = {
        val latest = state.latestResult
        if (latest != null && latest.isRecent(System.currentTimeMillis())) {
            showRecentWarning = true
        } else {
            viewModel.startNewTest()
            showQuestionnaire = true
        }
    }

    if (showRecentWarning) {
        val days = state.latestResult?.daysSince(System.currentTimeMillis()) ?: 0
        AlertDialog(
            onDismissRequest = { showRecentWarning = false },
            title = { Text("Refazer o teste agora?") },
            text = {
                Text(
                    "O ACT avalia as últimas 4 semanas, e seu último teste foi ${daysAgoText(days)}. " +
                        "Refazer agora repete boa parte do mesmo período. O ideal é refazer a cada 4 semanas, " +
                        "mas você pode refazer antes se algo mudou (por exemplo, depois de uma crise ou de uma troca de remédio)."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRecentWarning = false
                    viewModel.startNewTest()
                    showQuestionnaire = true
                }) { Text("Refazer mesmo assim") }
            },
            dismissButton = {
                TextButton(onClick = { showRecentWarning = false }) { Text("Agora não") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teste de Controle da Asma", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isSubmitted && state.latestResult != null -> {
                    ActResultContent(
                        result = state.latestResult!!,
                        onRedo = startTest,
                        onHistory = onNavigateToHistory,
                        onDone = { navController.popBackStack() }
                    )
                }
                showQuestionnaire -> {
                    ActQuestionnaireContent(
                        isLoading = state.isLoading,
                        onSubmit = { a, b, c, d, e, steroid, er -> viewModel.submitTest(a, b, c, d, e, steroid, er) }
                    )
                }
                state.isLoading && state.latestResult == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    ActEntryContent(
                        latestResult = state.latestResult,
                        onStart = startTest,
                        onHistory = onNavigateToHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun ActEntryContent(
    latestResult: AsthmaControlTestResult?,
    onStart: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "O ACT é um questionário validado de 5 perguntas sobre as últimas 4 semanas, usado por pneumologistas pra avaliar o controle da sua asma.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (latestResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = levelContainerColor(latestResult.level))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Último resultado", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${latestResult.score} pontos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(latestResult.level.label, fontWeight = FontWeight.SemiBold)
                    val days = latestResult.daysSince(System.currentTimeMillis())
                    Text(
                        "Feito ${daysAgoText(days)}" +
                            if (latestResult.isRecent(System.currentTimeMillis())) " · próximo em ${ActIntervalDaysLeft(days)} dia(s)" else " · já pode refazer",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Você ainda não fez o ACT.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(if (latestResult == null) "Fazer teste" else "Refazer teste")
        }

        if (latestResult != null) {
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Ver histórico")
            }
        }

        Text(
            "Este teste é informativo e não substitui avaliação médica. Leva menos de 2 minutos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActQuestionnaireContent(
    isLoading: Boolean,
    onSubmit: (Int, Int, Int, Int, Int, Boolean, Boolean) -> Unit
) {
    val answers = remember { mutableStateListOf<Int?>(null, null, null, null, null) }
    val severeAnswers = remember { mutableStateListOf<Boolean?>(null, null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(ACT_QUESTIONS) { index, question ->
            ActQuestionCard(
                index = index,
                question = question,
                selected = answers[index],
                onSelect = { answers[index] = it }
            )
        }
        item {
            Text(
                "Para o seu médico (não entram na pontuação do ACT)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        itemsIndexed(SEVERE_EXACERBATION_QUESTIONS) { index, question ->
            YesNoQuestionCard(
                number = ACT_QUESTIONS.size + index + 1,
                text = question,
                selected = severeAnswers[index],
                onSelect = { severeAnswers[index] = it }
            )
        }
        item {
            Button(
                onClick = {
                    val values = answers.filterNotNull()
                    val steroid = severeAnswers[0]
                    val er = severeAnswers[1]
                    if (values.size == 5 && steroid != null && er != null) {
                        onSubmit(values[0], values[1], values[2], values[3], values[4], steroid, er)
                    }
                },
                enabled = !isLoading && answers.all { it != null } && severeAnswers.all { it != null },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Calcular pontuação")
                }
            }
        }
    }
}

@Composable
private fun ActQuestionCard(
    index: Int,
    question: ActQuestion,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${index + 1}. ${question.text}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            // Opção N vale (5 - N) pontos — índice 0 = "pior" opção = 1 ponto,
            // índice 4 = "melhor" opção = 5 pontos (mesma ordem da fonte original).
            question.options.forEachIndexed { optionIndex, label ->
                val value = optionIndex + 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = selected == value, onClick = { onSelect(value) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun YesNoQuestionCard(
    number: Int,
    text: String,
    selected: Boolean?,
    onSelect: (Boolean) -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "$number. $text",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            listOf(true to "Sim", false to "Não").forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = selected == value, onClick = { onSelect(value) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ActResultContent(
    result: AsthmaControlTestResult,
    onRedo: () -> Unit,
    onHistory: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = levelContainerColor(result.level))
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Seu resultado", style = MaterialTheme.typography.labelLarge)
                Text("${result.score} pontos", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(result.level.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(levelMessage(result.level), style = MaterialTheme.typography.bodyMedium)

        if (result.hadSevereExacerbation) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    "Você relatou uma crise grave nas últimas 4 semanas (corticoide em comprimido, pronto-socorro ou internação). " +
                        "Leve essa informação ao seu médico: uma crise grave aumenta o risco de novas crises, mesmo quando os sintomas melhoram.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            "Este resultado é informativo e não substitui consulta médica. Leve-o ao seu pneumologista.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Concluir") }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Ver histórico") }
        TextButton(onClick = onRedo, modifier = Modifier.fillMaxWidth()) { Text("Refazer teste") }
    }
}

private fun levelMessage(level: AsthmaControlLevel): String = when (level) {
    AsthmaControlLevel.WELL_CONTROLLED -> "Sua asma parece estar bem controlada nas últimas 4 semanas. Continue com o tratamento e acompanhamento médico regular."
    AsthmaControlLevel.PARTIALLY_CONTROLLED -> "Sua asma não está totalmente controlada. Considere conversar com seu pneumologista para ajustar o tratamento."
    AsthmaControlLevel.NOT_CONTROLLED -> "Sua asma parece estar fora de controle. Procure um pneumologista para reavaliar seu plano de tratamento."
}

@Composable
private fun levelContainerColor(level: AsthmaControlLevel) = when (level) {
    AsthmaControlLevel.WELL_CONTROLLED -> androidx.compose.ui.graphics.Color(0xFF1B5E20).copy(alpha = 0.15f)
    AsthmaControlLevel.PARTIALLY_CONTROLLED -> androidx.compose.ui.graphics.Color(0xFFF57F17).copy(alpha = 0.15f)
    AsthmaControlLevel.NOT_CONTROLLED -> androidx.compose.ui.graphics.Color(0xFFB71C1C).copy(alpha = 0.15f)
}

private fun daysAgoText(days: Int): String = when (days) {
    0 -> "hoje"
    1 -> "ontem"
    else -> "há $days dias"
}

@Suppress("FunctionName")
private fun ActIntervalDaysLeft(daysSince: Int): Int =
    (com.afilaxy.domain.model.ACT_INTERVAL_DAYS - daysSince).coerceAtLeast(1)
