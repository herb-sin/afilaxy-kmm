package com.afilaxy.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.afilaxy.domain.model.AsthmaControlLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ── Step machine ──────────────────────────────────────────────────────────────

private sealed class ReportStep {
    object Idle : ReportStep()
    object Generating : ReportStep()
    object Done : ReportStep()
    data class Failed(val message: String) : ReportStep()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthReportExportScreen(onNavigateBack: () -> Unit) {
    var step by remember { mutableStateOf<ReportStep>(ReportStep.Idle) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // FLAG_SECURE: enquanto esta tela (relatório de saúde) estiver visível, a
    // janela não pode ser capturada por screenshot nem pela tela recentes.
    // Limpo no dispose ao sair da tela, para não afetar o resto do app.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório de Saúde") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = step) {
                is ReportStep.Idle, is ReportStep.Done -> IdleContent(
                    onGenerate = {
                        scope.launch {
                            step = ReportStep.Generating
                            val ok = generateAndShare(context)
                            step = if (ok) ReportStep.Done
                                   else ReportStep.Failed("Não foi possível gerar o PDF. Tente novamente.")
                        }
                    }
                )
                is ReportStep.Generating -> GeneratingContent()
                is ReportStep.Failed -> FailedContent(s.message) { step = ReportStep.Idle }
            }
        }
    }
}

// ── Idle: pronto para gerar ────────────────────────────────────────────────────

@Composable
private fun IdleContent(onGenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Relatório completo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Gere um PDF com seus check-ins e pedidos de ajuda das últimas 4 semanas, seus Testes de Controle da Asma e seu Perfil Médico — pronto para compartilhar com quem você quiser, sem precisar informar um profissional específico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Conteúdo do Relatório (4 semanas)",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                listOf(
                    "Controle de sintomas pelos critérios da GINA",
                    "Teste de Controle da Asma (último resultado e histórico)",
                    "Crises, pedidos de ajuda e fatores de risco",
                    "Adesão ao monitoramento e uso de bombinha de resgate",
                    "Check-ins matinal e noturno",
                    "Perfil Médico (alergias, medicamentos, acessibilidade)"
                ).forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Text(item, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Gerar Relatório PDF", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Step 3 & 4: Generating / Failed ──────────────────────────────────────────

@Composable
private fun GeneratingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Gerando relatório...", style = MaterialTheme.typography.titleMedium)
            Text("Coletando seus dados das últimas 4 semanas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp))
            Text("Erro ao gerar relatório", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

// ── Logic ─────────────────────────────────────────────────────────────────────

private suspend fun generateAndShare(context: Context): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val patientName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuário"
    // Janela de 4 semanas (28 dias): é o período dos critérios de controle da GINA.
    val nowMs = System.currentTimeMillis()
    // Em dias de calendário: de 00:00 de 27 dias atrás até agora (hoje incluso), para que
    // o período, as semanas S1–S4 e os dados do PDF usem as mesmas datas.
    val cutoffMs = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -(REPORT_DAYS - 1))
    }.timeInMillis

    val checkInDocs = try {
        FirebaseFirestore.getInstance()
            .collection("checkins").document(uid)
            .collection("responses")
            .whereGreaterThanOrEqualTo("timestamp", cutoffMs)
            .get().await()
    } catch (e: Exception) { return false }

    val statsDoc = try {
        FirebaseFirestore.getInstance().collection("user_stats").document(uid).get().await()
    } catch (e: Exception) { null }

    val userDoc = try {
        FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
    } catch (e: Exception) { null }

    // ACT: histórico completo (não só as 4 semanas) — o teste é mensal, então a janela do
    // relatório mostraria no máximo um resultado e esconderia a tendência.
    val actResults = try {
        FirebaseFirestore.getInstance()
            .collection("act").document(uid)
            .collection("results")
            .get().await()
            .documents.map { doc ->
                ActResultAndroid(
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    score = doc.getLong("score")?.toInt() ?: 0,
                    answers = ACT_ANSWER_FIELDS.map { doc.getLong(it)?.toInt() ?: 0 },
                    oralSteroidCourse4w = doc.getBoolean("oralSteroidCourse4w"),
                    emergencyVisit4w = doc.getBoolean("emergencyVisit4w")
                )
            }
            .sortedByDescending { it.timestamp }
            .take(ACT_HISTORY_LIMIT)
    } catch (e: Exception) { emptyList() }

    // Filtro só por requesterId (exigido pela regra de list) e período aplicado no cliente —
    // evita índice composto, e o volume de pedidos por usuário é pequeno.
    val emergencies = try {
        FirebaseFirestore.getInstance()
            .collection("emergency_requests")
            .whereEqualTo("requesterId", uid)
            .get().await()
            .documents.mapNotNull { doc ->
                val ts = doc.getLong("timestamp") ?: return@mapNotNull null
                if (ts < cutoffMs) return@mapNotNull null
                EmergencyEventAndroid(
                    timestamp = ts,
                    status = doc.getString("status") ?: "",
                    samuCalled = doc.getBoolean("samuCalled") ?: false,
                    temperature = doc.getDouble("envTemperature"),
                    humidity = doc.getDouble("envHumidity"),
                    aqi = doc.getLong("envAqi")?.toInt(),
                    emergencyVisitAfter = doc.getBoolean("emergencyVisitAfter")
                )
            }
            .sortedByDescending { it.timestamp }
    } catch (e: Exception) { emptyList() }

    // Pergunta sem resposta fica null — não é "Não". Check-ins antigos não tinham
    // algumas perguntas e não podem ser contados como sintoma.
    val checkIns = checkInDocs.documents.map { doc ->
        WellbeingCheckInAndroid(
            type                 = doc.getString("type") ?: "",
            timestamp            = doc.getLong("timestamp") ?: 0L,
            rescueInhalerUses    = doc.getLong("rescueInhalerUses")?.toInt(),
            nighttimeAwakening   = doc.getBoolean("nighttimeAwakening"),
            morningMoodGood      = doc.getBoolean("morningMoodGood"),
            morningEnergyGood    = doc.getBoolean("morningEnergyGood"),
            selfCareGood         = doc.getBoolean("selfCareGood"),
            daytimeBreathingEase = doc.getBoolean("daytimeBreathingEase"),
            activityAsPlanned    = doc.getBoolean("activityAsPlanned")
        )
    }

    val healthProfile = HealthProfileAndroid(
        allergiesText = (userDoc?.get("healthData.allergies") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList(),
        medications = (userDoc?.get("healthData.medications") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList(),
        conditions = (userDoc?.get("healthData.conditions") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList(),
        notes = userDoc?.getString("healthData.notes") ?: "",
        hasGerd = userDoc?.getBoolean("healthData.hasGerd") ?: false,
        hasSleepApnea = userDoc?.getBoolean("healthData.hasSleepApnea") ?: false,
        hasRhinitis = userDoc?.getBoolean("healthData.hasRhinitis") ?: false,
        hasObesity = userDoc?.getBoolean("healthData.hasObesity") ?: false,
        hasFoodAllergy = userDoc?.getBoolean("healthData.hasFoodAllergy") ?: false,
        hasNsaidAllergy = userDoc?.getBoolean("healthData.hasNsaidAllergy") ?: false,
        hasInhalantAllergy = userDoc?.getBoolean("healthData.hasInhalantAllergy") ?: false,
        hasWheelchair = userDoc?.getBoolean("healthData.hasWheelchair") ?: false,
        hasLowVision = userDoc?.getBoolean("healthData.hasLowVision") ?: false,
        hasSpecialCondition = userDoc?.getBoolean("healthData.hasSpecialCondition") ?: false,
        severeExacerbations12m = userDoc?.getLong("healthData.severeExacerbations12m")?.toInt() ?: 0,
        severeExacerbationsUpdatedAt = userDoc?.getLong("healthData.severeExacerbationsUpdatedAt") ?: 0L,
        everIcuOrIntubated = userDoc?.getBoolean("healthData.everIcuOrIntubated") ?: false,
        emergencyContactName = userDoc?.getString("emergencyContact.name") ?: "",
        emergencyContactPhone = userDoc?.getString("emergencyContact.phone") ?: "",
        birthYear = userDoc?.getLong("healthData.birthYear")?.toInt() ?: 0
    )

    val report = WellbeingReportAndroid(
        patientName       = patientName,
        healthProfile     = healthProfile,
        checkIns          = checkIns,
        emergencyCount    = emergencyCountInWindow(statsDoc),
        emergencies       = emergencies,
        actResults        = actResults,
        windowStartMs     = cutoffMs,
        generatedAt       = Date(nowMs)
    )

    // PdfDocument + FileOutputStream rodam em IO: a geração do PDF é pesada e
    // nunca deve bloquear a main thread.
    val file = withContext(Dispatchers.IO) {
        WellbeingPDFAndroid.generate(context, report)
    } ?: return false

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
    return true
}

private fun emergencyCountInWindow(statsDoc: DocumentSnapshot?): Int {
    val daily = statsDoc?.get("dailyCount") as? Map<*, *>
        ?: return statsDoc?.getLong("totalEmergencies")?.toInt() ?: 0
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    var sum = 0
    repeat(REPORT_DAYS) {
        sum += (daily[fmt.format(cal.time)] as? Long)?.toInt() ?: 0
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return sum
}

// ── Data Models ───────────────────────────────────────────────────────────────

private const val DAY_MS = 86_400_000L
private const val WEEK_MS = 7 * DAY_MS
private const val REPORT_DAYS = 28
private const val REPORT_WEEKS = 4
private const val POSSIBLE_CHECKINS = 56 // 2 check-ins por dia × 28 dias

/** Campos do ACT na ordem das perguntas P1–P5 (ver AsthmaControlTestResult). */
private val ACT_ANSWER_FIELDS = listOf(
    "activityImpairment", "shortnessOfBreath", "nighttimeSymptoms", "rescueMedicationUse", "selfRatedControl"
)
private val ACT_ANSWER_LABELS = listOf(
    "P1 · Limitação de atividades",
    "P2 · Falta de ar",
    "P3 · Despertares noturnos por asma",
    "P4 · Uso de medicação de alívio",
    "P5 · Autoavaliação do controle"
)
private const val ACT_HISTORY_LIMIT = 6

private data class ActResultAndroid(
    val timestamp: Long,
    val score: Int,
    val answers: List<Int>, // P1–P5, cada uma de 1 (pior) a 5 (melhor)
    // Perguntas complementares (fora do escore) — null em resultados antigos
    val oralSteroidCourse4w: Boolean?,
    val emergencyVisit4w: Boolean?
) {
    val levelLabel: String get() = AsthmaControlLevel.fromScore(score).label
    val hadSevereExacerbation: Boolean get() = oralSteroidCourse4w == true || emergencyVisit4w == true
}

private data class EmergencyEventAndroid(
    val timestamp: Long,
    val status: String,
    val samuCalled: Boolean,
    // Contexto ambiental gravado na criação do pedido — null em pedidos antigos ou sem rede
    val temperature: Double?,
    val humidity: Double?,
    val aqi: Int?,
    // Resposta do diálogo pós-crise — null se não respondida
    val emergencyVisitAfter: Boolean?
)

/** Respostas de um check-in. null = pergunta não respondida (ou inexistente na época). */
private data class WellbeingCheckInAndroid(
    val type: String,
    val timestamp: Long,
    val rescueInhalerUses: Int?,
    val nighttimeAwakening: Boolean?, // true = "sono tranquilo, sem interrupções"
    val morningMoodGood: Boolean?,
    val morningEnergyGood: Boolean?,
    val selfCareGood: Boolean?,
    val daytimeBreathingEase: Boolean?,
    val activityAsPlanned: Boolean?
) {
    val isMorning: Boolean get() = type == "MORNING"
    val isEvening: Boolean get() = type == "EVENING"

    /** Crítico só quando as três respostas do período foram explicitamente "Não". */
    fun isCritical(): Boolean = if (isMorning) {
        nighttimeAwakening == false && morningMoodGood == false && morningEnergyGood == false
    } else {
        selfCareGood == false && daytimeBreathingEase == false && activityAsPlanned == false
    }
}

// ── Controle de sintomas — critérios da GINA (4 semanas) ─────────────────────
// O app não diagnostica nem classifica: mostra cada critério com as contagens
// semanais e quantos estão presentes. A escala da GINA vai como referência e a
// interpretação fica com o profissional.

private enum class GinaStatus(val label: String) {
    PRESENT("Presente"), ABSENT("Ausente"), NO_DATA("Sem dados")
}

private class GinaCriterion(
    val label: String,
    /** Ocorrências por semana, S1 (mais antiga) → S4 (mais recente); null = sem resposta na semana. */
    val weekly: List<Int?>,
    /** Presente quando alguma semana tem mais que [threshold] ocorrências. */
    threshold: Int
) {
    val status: GinaStatus = when {
        weekly.all { it == null } -> GinaStatus.NO_DATA
        weekly.any { it != null && it > threshold } -> GinaStatus.PRESENT
        else -> GinaStatus.ABSENT
    }

    fun valueText(): String {
        val weeks = weekly.joinToString(" · ") { it?.toString() ?: "—" }
        return "${status.label} — por semana: $weeks"
    }
}

private class GinaWeek(val startMs: Long, val morningCount: Int, val eveningCount: Int)

private class GinaControl(checkIns: List<WellbeingCheckInAndroid>, windowStartMs: Long) {
    val weeks: List<GinaWeek>
    val criteria: List<GinaCriterion>

    init {
        fun weekOf(c: WellbeingCheckInAndroid): Int =
            ((c.timestamp - windowStartMs) / WEEK_MS).toInt().coerceIn(0, REPORT_WEEKS - 1)

        fun weekly(source: List<WellbeingCheckInAndroid>, value: (WellbeingCheckInAndroid) -> Int?): List<Int?> {
            val sums = arrayOfNulls<Int>(REPORT_WEEKS)
            source.forEach { c ->
                val v = value(c) ?: return@forEach
                val w = weekOf(c)
                sums[w] = (sums[w] ?: 0) + v
            }
            return sums.toList()
        }

        fun occurred(answer: Boolean?, occursWhen: Boolean): Int? = answer?.let { if (it == occursWhen) 1 else 0 }

        val morning = checkIns.filter { it.isMorning }
        val evening = checkIns.filter { it.isEvening }

        weeks = (0 until REPORT_WEEKS).map { w ->
            GinaWeek(
                startMs = windowStartMs + w * WEEK_MS,
                morningCount = morning.count { weekOf(it) == w },
                eveningCount = evening.count { weekOf(it) == w }
            )
        }

        criteria = listOf(
            GinaCriterion("Sintomas diurnos >2x/sem.",
                weekly(evening) { occurred(it.daytimeBreathingEase, occursWhen = false) }, threshold = 2),
            GinaCriterion("Despertar noturno",
                weekly(morning) { occurred(it.nighttimeAwakening, occursWhen = false) }, threshold = 0),
            GinaCriterion("Resgate >2x/sem.",
                weekly(evening) { it.rescueInhalerUses }, threshold = 2),
            GinaCriterion("Limitação de atividades",
                weekly(evening) { occurred(it.activityAsPlanned, occursWhen = false) }, threshold = 0)
        )
    }

    val presentCount: Int get() = criteria.count { it.status == GinaStatus.PRESENT }
    val noDataCount: Int get() = criteria.count { it.status == GinaStatus.NO_DATA }
}

private data class HealthProfileAndroid(
    val allergiesText: List<String>,
    val medications: List<String>,
    val conditions: List<String>,
    val notes: String,
    val hasGerd: Boolean,
    val hasSleepApnea: Boolean,
    val hasRhinitis: Boolean,
    val hasObesity: Boolean,
    val hasFoodAllergy: Boolean,
    val hasNsaidAllergy: Boolean,
    val hasInhalantAllergy: Boolean,
    val hasWheelchair: Boolean,
    val hasLowVision: Boolean,
    val hasSpecialCondition: Boolean,
    val severeExacerbations12m: Int,
    val severeExacerbationsUpdatedAt: Long,
    val everIcuOrIntubated: Boolean,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val birthYear: Int // 0 = não informado
)

private data class WellbeingReportAndroid(
    val patientName: String,
    val healthProfile: HealthProfileAndroid,
    val checkIns: List<WellbeingCheckInAndroid>,
    val emergencyCount: Int,
    val emergencies: List<EmergencyEventAndroid>,
    val actResults: List<ActResultAndroid>,
    val windowStartMs: Long,
    val generatedAt: Date
) {
    val gina = GinaControl(checkIns, windowStartMs)
}

// ── PDF Generator ─────────────────────────────────────────────────────────────

private object WellbeingPDFAndroid {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 50f
    private const val FOOTER_RESERVE = 60f
    private val CONTENT_W get() = PAGE_W - MARGIN * 2
    private val BOTTOM_LIMIT get() = PAGE_H - FOOTER_RESERVE

    private val PRIMARY  = android.graphics.Color.rgb(0, 98, 143)
    private val MORNING  = android.graphics.Color.rgb(230, 81, 0)
    private val EVENING  = android.graphics.Color.rgb(26, 35, 126)
    private val CRITICAL = android.graphics.Color.rgb(186, 26, 26)
    private val ROW_ALT  = android.graphics.Color.rgb(247, 247, 247)
    private val TRACK_BG = android.graphics.Color.rgb(220, 220, 220)

    // Cursor de paginação: cada seção/linha pede espaço antes de desenhar, e uma
    // nova página do PdfDocument é aberta automaticamente quando não há espaço
    // suficiente antes da área reservada ao rodapé. onPageStart desenha o
    // cabeçalho de continuação nas páginas 2+ e devolve o Y inicial de conteúdo.
    private class Cursor(
        private val doc: PdfDocument,
        private val pageW: Int,
        private val pageH: Int,
        private val bottomLimit: Float,
        private val onPageStart: (android.graphics.Canvas, Int) -> Float
    ) {
        var page: PdfDocument.Page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        var canvas: android.graphics.Canvas = page.canvas
        var y: Float = 0f
        private var pageNumber = 1

        fun breakIfNeeded(neededHeight: Float) {
            if (y + neededHeight > bottomLimit) newPage()
        }

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
            canvas = page.canvas
            y = onPageStart(canvas, pageNumber)
        }

        fun finish() {
            doc.finishPage(page)
        }
    }

    fun generate(context: Context, report: WellbeingReportAndroid): File? {
        val doc = PdfDocument()
        val cursor = Cursor(doc, PAGE_W, PAGE_H, BOTTOM_LIMIT) { canvas, pageNumber ->
            val p = tp(9f, android.graphics.Color.GRAY)
            canvas.drawText(
                "AFILAXY — Relatório de Monitoramento de Saúde (continuação, pág. $pageNumber)",
                MARGIN, MARGIN + ascent(p), p
            )
            MARGIN + 20f
        }

        // Ordem pensada para o médico: o que decide a consulta (GINA, ACT, crises e
        // fatores de risco) vem primeiro; os detalhes do monitoramento depois.
        header(cursor, report)
        infoBox(cursor, report)
        ginaSection(cursor, report)
        actSection(cursor, report)
        riskSection(cursor, report)
        adherenceSection(cursor, report)
        morningSection(cursor, report)
        eveningSection(cursor, report)
        healthProfileSection(cursor, report)
        footer(cursor, report)
        cursor.finish()

        return try {
            val dir = File(context.cacheDir, "reports").also { it.mkdirs() }
            // Rotação: apaga SOMENTE PDFs de gerações anteriores (lastModified
            // anterior ao início desta geração) — duas gerações rápidas não se
            // apagam mutuamente. Trade-off: um relatório já compartilhado deixa
            // de existir em disco, mas o arquivo atual fica disponível para o
            // chooser do share até a próxima geração, e cacheDir é liberável
            // pelo sistema quando o armazenamento aperta.
            val generationStartMs = System.currentTimeMillis()
            dir.listFiles()?.forEach { old ->
                if (old.isFile && old.lastModified() < generationStartMs) old.delete()
            }
            val file = File(dir, "relatorio_afilaxy_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) { doc.close(); null }
    }

    // MARK: Header

    private fun header(cursor: Cursor, r: WellbeingReportAndroid) {
        val c = cursor.canvas
        val h = 88f
        c.drawRect(RectF(0f, 0f, PAGE_W.toFloat(), h), fill(PRIMARY))
        c.drawText("AFILAXY", MARGIN, 18f + ascent(tp(22f, android.graphics.Color.WHITE, bold = true)), tp(22f, android.graphics.Color.WHITE, bold = true))
        c.drawText("Relatório de Monitoramento de Saúde", MARGIN, 46f + ascent(tp(13f, android.graphics.Color.WHITE)),
            tp(13f, android.graphics.Color.WHITE))
        val datePaint = tp(10f, android.graphics.Color.WHITE).apply { alpha = 200 }
        val dateStr = "Gerado em ${dateFmt(r.generatedAt)}"
        c.drawText(dateStr, PAGE_W - MARGIN - datePaint.measureText(dateStr), 40f + ascent(datePaint), datePaint)
        cursor.y = h + 22f
    }

    // MARK: Info Box

    private fun infoBox(cursor: Cursor, r: WellbeingReportAndroid) {
        val h = 96f
        cursor.breakIfNeeded(h + 20f)
        val c = cursor.canvas
        val y = cursor.y
        c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + h), fill(android.graphics.Color.rgb(245, 245, 245)))

        val lx = MARGIN + 10f
        c.drawText("PACIENTE", lx, y + 10f + ascent(lp()), lp())
        c.drawText(r.patientName, lx, y + 22f + ascent(tp(12f, android.graphics.Color.BLACK, bold = true)),
            tp(12f, android.graphics.Color.BLACK, bold = true))
        c.drawText("PERÍODO", lx, y + 46f + ascent(lp()), lp())
        c.drawText(periodStr(r), lx, y + 58f + ascent(tp(10f, android.graphics.Color.DKGRAY)),
            tp(10f, android.graphics.Color.DKGRAY))

        val rx = PAGE_W - MARGIN - 205f
        c.drawText("ORIGEM", rx, y + 10f + ascent(lp()), lp())
        c.drawText("Autorrelato do paciente", rx, y + 22f + ascent(tp(11f, android.graphics.Color.BLACK, bold = true)),
            tp(11f, android.graphics.Color.BLACK, bold = true))
        c.drawText("Sem revisão clínica prévia", rx, y + 40f + ascent(tp(10f, android.graphics.Color.DKGRAY)),
            tp(10f, android.graphics.Color.DKGRAY))
        c.drawText("ANO DE NASCIMENTO", rx, y + 60f + ascent(lp()), lp())
        c.drawText(birthYearText(r), rx, y + 72f + ascent(tp(11f, android.graphics.Color.BLACK, bold = true)),
            tp(11f, android.graphics.Color.BLACK, bold = true))

        cursor.y = y + h + 20f
    }

    // MARK: Sections

    private fun ginaSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val g = r.gina
        sectionHeader(cursor, "1. CONTROLE DE SINTOMAS — CRITÉRIOS GINA (4 SEMANAS)", PRIMARY)
        val present = buildString {
            append("${g.presentCount} de 4")
            if (g.noDataCount > 0) append(" (${g.noDataCount} sem dados)")
        }
        val weekFmt = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
        val weeksText = g.weeks.mapIndexed { i, w ->
            "S${i + 1} ${weekFmt.format(Date(w.startMs))}–${weekFmt.format(Date(w.startMs + WEEK_MS - DAY_MS))}"
        }.joinToString(" · ")
        val coverageText = g.weeks.mapIndexed { i, w ->
            "S${i + 1}: ${w.morningCount} manhãs, ${w.eveningCount} noites"
        }.joinToString(" · ")

        val rows = mutableListOf("Critérios presentes" to present)
        g.criteria.forEach { rows += it.label to it.valueText() }
        rows += "Semanas" to weeksText
        rows += "Check-ins por semana" to coverageText
        rows += "Regra" to "Sintomas diurnos e resgate: presente se mais de 2 ocorrências em alguma semana. Despertar e limitação: presente se ocorreu ao menos uma vez. — = semana sem resposta."
        rows += "Fontes" to "Sintomas diurnos: \"Respirei com facilidade ao longo do dia?\" = Não. Despertar: \"Meu sono foi tranquilo, sem interrupções?\" = Não. Resgate: nº de usos informado no check-in noturno. Limitação: \"Consegui fazer tudo que tinha planejado?\" = Não."
        rows += "Limitações" to "Perguntas de bem-estar, não específicas de asma: despertar e limitação podem ter outras causas. O uso de resgate antes de exercício não é separado. Dias sem check-in não entram nas contagens."
        rows += "Referência GINA" to "0 critérios: bem controlada; 1–2: parcialmente controlada; 3–4: não controlada. A interpretação cabe ao profissional de saúde."
        plainRows(cursor, rows)
    }

    private fun adherenceSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val total = r.checkIns.size
        val adherence = total * 100 / POSSIBLE_CHECKINS
        val critical = r.checkIns.count { it.isCritical() }
        val rescueAnswers = r.checkIns.mapNotNull { it.rescueInhalerUses }
        val rescueValue = if (rescueAnswers.isEmpty()) "Não informado"
            else "${rescueAnswers.sum()} vez(es) em ${rescueAnswers.size} dia(s) informado(s)"
        sectionHeader(cursor, "4. ADESÃO AO MONITORAMENTO", PRIMARY)
        plainRows(cursor, listOf(
            "Check-ins realizados"                  to "$total de $POSSIBLE_CHECKINS possíveis",
            "Taxa de adesão ao monitoramento"       to "$adherence%",
            "Uso de bombinha de resgate"            to rescueValue,
            "Dias com bem-estar mínimo registrado"  to "$critical ocorrência(s)"
        ))
    }

    private fun actSection(cursor: Cursor, r: WellbeingReportAndroid) {
        sectionHeader(cursor, "2. CONTROLE DA ASMA — ACT", PRIMARY)
        val latest = r.actResults.firstOrNull()
        if (latest == null) {
            plainRows(cursor, listOf("Teste de Controle da Asma (ACT)" to "Nunca respondido"))
            return
        }
        val rows = mutableListOf(
            "Último resultado (${dateFmt(Date(latest.timestamp))})" to "${latest.score}/25 — ${latest.levelLabel}"
        )
        ACT_ANSWER_LABELS.forEachIndexed { i, label ->
            rows += label to "${latest.answers.getOrElse(i) { 0 }}/5"
        }
        rows += "Corticoide oral ≥3 dias (4 semanas)" to yesNo(latest.oralSteroidCourse4w)
        rows += "Pronto-socorro/UPA ou internação (4 semanas)" to yesNo(latest.emergencyVisit4w)
        r.actResults.drop(1).forEach { previous ->
            val severe = if (previous.hadSevereExacerbation) " · crise grave relatada" else ""
            rows += "Anterior (${dateFmt(Date(previous.timestamp))})" to "${previous.score}/25 — ${previous.levelLabel}$severe"
        }
        rows += "Crise grave" to "Definição da GINA: corticoide oral por 3 dias ou mais, pronto-socorro ou internação. Perguntas complementares, fora da pontuação do ACT."
        rows += "Interpretação" to "Itens de 1 (pior) a 5 (melhor). Total ≥20: bem controlada; 15–19: parcialmente; ≤14: não controlada. Respondido pelo paciente."
        plainRows(cursor, rows)
    }

    private fun morningSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val m = r.checkIns.filter { it.isMorning }
        sectionHeader(cursor, "5. CHECK-IN MATINAL — % DE RESPOSTAS \"SIM\"", MORNING)
        barRows(cursor, listOf(
            "Registros de manhã: ${m.size}" to null,
            answeredRow("\"Meu sono foi tranquilo, sem interrupções\"", m) { it.nighttimeAwakening },
            answeredRow("\"Me sinto bem esta manhã\"", m) { it.morningMoodGood },
            answeredRow("\"Estou com boa energia\"", m) { it.morningEnergyGood }
        ), MORNING)
    }

    private fun eveningSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val e = r.checkIns.filter { it.isEvening }
        sectionHeader(cursor, "6. CHECK-IN NOTURNO — % DE RESPOSTAS \"SIM\"", EVENING)
        barRows(cursor, listOf(
            "Registros noturnos: ${e.size}" to null,
            answeredRow("\"Me cuidei bem hoje\"", e) { it.selfCareGood },
            answeredRow("\"Respirei com facilidade ao longo do dia\"", e) { it.daytimeBreathingEase },
            answeredRow("\"Consegui fazer tudo que tinha planejado\"", e) { it.activityAsPlanned }
        ), EVENING)
    }

    private fun riskSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val p = r.healthProfile
        sectionHeader(cursor, "3. CRISES E FATORES DE RISCO", CRITICAL)

        val rows = mutableListOf("Pedidos de ajuda (4 semanas)" to "${r.emergencyCount}")
        r.emergencies.forEach { e -> rows += dateTimeFmt(Date(e.timestamp)) to emergencyDetail(e) }

        val recentAct = r.actResults.firstOrNull()?.takeIf { it.timestamp >= r.windowStartMs }
        val recentSevere = buildList {
            recentAct?.takeIf { it.hadSevereExacerbation }?.let { add("relatada no ACT de ${dateFmt(Date(it.timestamp))}") }
            r.emergencies.filter { it.emergencyVisitAfter == true }.forEach {
                add("pronto-socorro/UPA ou internação após pedido de ajuda em ${dateFmt(Date(it.timestamp))}")
            }
        }
        rows += "Crise grave (4 semanas)" to when {
            recentSevere.isNotEmpty() -> "Sim — " + recentSevere.joinToString("; ")
            recentAct == null -> "Nenhuma registrada (sem ACT no período)"
            recentAct.oralSteroidCourse4w == null && recentAct.emergencyVisit4w == null ->
                "Não perguntado no ACT de ${dateFmt(Date(recentAct.timestamp))}"
            else -> "Não relatada"
        }

        val severeHistory = when {
            p.severeExacerbationsUpdatedAt <= 0L -> "Não informado"
            p.severeExacerbations12m == 0 -> "Nenhuma (informado em ${dateFmt(Date(p.severeExacerbationsUpdatedAt))})"
            else -> "${p.severeExacerbations12m} nos 12 meses anteriores a ${dateFmt(Date(p.severeExacerbationsUpdatedAt))}"
        }
        rows += "Crises graves em 12 meses" to severeHistory
        rows += "UTI ou intubação (alguma vez)" to if (p.everIcuOrIntubated) "Sim" else "Não informado"

        val comorbidities = buildList {
            if (p.hasObesity) add("Obesidade")
            if (p.hasRhinitis) add("Rinite alérgica")
            if (p.hasGerd) add("Refluxo / DRGE")
            if (p.hasFoodAllergy) add("Alergia alimentar")
            if (p.hasSleepApnea) add("Apneia do sono")
        }
        rows += "Comorbidades relatadas" to comorbidities.joinToString(", ").ifEmpty { "Nenhuma" }
        rows += "Não coletado pelo app" to "Tabagismo ou exposição à fumaça, adesão ao tratamento de manutenção, técnica inalatória, gestação e função pulmonar."
        plainRows(cursor, rows)
    }

    private fun emergencyDetail(e: EmergencyEventAndroid): String {
        val parts = mutableListOf<String>()
        e.temperature?.let { parts += "${it.toInt()}°C" }
        e.humidity?.let { parts += "umidade ${it.toInt()}%" }
        e.aqi?.let { parts += "AQI $it" }
        if (parts.isEmpty()) parts += "sem dados ambientais"
        if (e.samuCalled) parts += "SAMU acionado"
        if (e.emergencyVisitAfter == true) parts += "foi ao pronto-socorro/UPA ou internado"
        if (e.status == "cancelled") parts += "cancelado"
        return parts.joinToString(" · ")
    }

    private fun healthProfileSection(cursor: Cursor, r: WellbeingReportAndroid) {
        val p = r.healthProfile
        val specificAllergies = buildList {
            if (p.hasFoodAllergy) add("Alimentar")
            if (p.hasNsaidAllergy) add("AINEs/aspirina")
            if (p.hasInhalantAllergy) add("Inalantes (ácaros/pólen/mofo/pelos)")
        }
        val accessibility = buildList {
            if (p.hasWheelchair) add("Cadeirante")
            if (p.hasLowVision) add("Baixa visão ou cegueira")
            if (p.hasSpecialCondition) add("Outra condição especial")
        }
        val emergencyContact = if (p.emergencyContactName.isNotEmpty()) {
            if (p.emergencyContactPhone.isNotEmpty()) "${p.emergencyContactName} — ${p.emergencyContactPhone}"
            else p.emergencyContactName
        } else "Não informado"

        // Comorbidades, crises graves e UTI ficam na seção 3 (fatores de risco).
        sectionHeader(cursor, "7. PERFIL MÉDICO", PRIMARY)
        plainRows(cursor, listOf(
            "Alergias conhecidas"    to p.allergiesText.joinToString(", ").ifEmpty { "Nenhuma relatada" },
            "Medicamentos em uso"    to p.medications.joinToString(", ").ifEmpty { "Nenhum relatado" },
            "Condições relatadas"    to p.conditions.joinToString(", ").ifEmpty { "Nenhuma" },
            "Alergias específicas"   to specificAllergies.joinToString(", ").ifEmpty { "Nenhuma" },
            "Acessibilidade"         to accessibility.joinToString(", ").ifEmpty { "Nenhuma" },
            "Detalhes complementares" to p.notes.ifEmpty { "Nenhum" },
            "Contato de emergência"  to emergencyContact
        ))
    }

    private fun footer(cursor: Cursor, r: WellbeingReportAndroid) {
        val fy = PAGE_H - 52f
        if (cursor.y > fy - 10f) cursor.newPage()
        val c = cursor.canvas
        c.drawRect(RectF(MARGIN, fy, MARGIN + CONTENT_W, fy + 0.5f), fill(android.graphics.Color.LTGRAY))
        val text = "Este relatório foi gerado automaticamente pelo aplicativo Afilaxy com base nas respostas " +
            "fornecidas pelo próprio usuário no período de ${periodStr(r)} (check-ins e pedidos de ajuda; " +
            "o ACT inclui testes anteriores ao período). Não substitui avaliação clínica presencial. " +
            "Gerado em ${dateFmt(r.generatedAt)}."
        val textPaint = TextPaint().apply { color = android.graphics.Color.GRAY; textSize = 8f; isAntiAlias = true }
        val sl = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, CONTENT_W.toInt()).build()
        c.save(); c.translate(MARGIN, fy + 6f); sl.draw(c); c.restore()
    }

    // MARK: Row Drawers

    private fun sectionHeader(cursor: Cursor, title: String, color: Int) {
        cursor.breakIfNeeded(22f + 21f)
        val c = cursor.canvas
        val y = cursor.y
        c.drawText(title, MARGIN, y + ascent(tp(11f, color, bold = true)), tp(11f, color, bold = true))
        val a = android.graphics.Color.argb(64,
            android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
        c.drawRect(RectF(MARGIN, y + 16f, MARGIN + CONTENT_W, y + 17f), fill(a))
        cursor.y = y + 22f
    }

    private fun plainRows(cursor: Cursor, rows: List<Pair<String, String>>) {
        val labelPaint = tp(10f, android.graphics.Color.DKGRAY)
        val valuePaint = tp(10f, android.graphics.Color.BLACK, bold = true)
        val valueMaxW = CONTENT_W - 150f
        val lineH = 12f
        rows.forEachIndexed { i, (label, value) ->
            val lines = wrapText(value, valuePaint, valueMaxW)
            val rh = (lineH * lines.size + 9f).coerceAtLeast(21f)
            cursor.breakIfNeeded(rh)
            val c = cursor.canvas
            val y = cursor.y
            if (i % 2 == 0) c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rh), fill(ROW_ALT))
            c.drawText(label, MARGIN + 8f, y + 5f + ascent(labelPaint), labelPaint)
            lines.forEachIndexed { li, line ->
                val lineY = y + 5f + ascent(valuePaint) + li * lineH
                c.drawText(line, MARGIN + CONTENT_W - valuePaint.measureText(line) - 8f, lineY, valuePaint)
            }
            cursor.y = y + rh
        }
        cursor.y += 16f
    }

    private fun barRows(cursor: Cursor, rows: List<Pair<String, Double?>>, barColor: Int) {
        val rh = 24f
        val barMaxW = 110f
        val barRight = MARGIN + CONTENT_W - 8f
        rows.forEachIndexed { i, (label, pctVal) ->
            cursor.breakIfNeeded(rh)
            val c = cursor.canvas
            val y = cursor.y
            if (i % 2 == 0) c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rh), fill(ROW_ALT))
            val labelPaint = tp(10f, android.graphics.Color.DKGRAY)
            c.drawText(label, MARGIN + 8f, y + 7f + ascent(labelPaint), labelPaint)
            if (pctVal != null) {
                val vp = tp(10f, android.graphics.Color.BLACK, bold = true)
                val valStr = "${pctVal.toInt()}%"
                val vW = vp.measureText(valStr)
                val barX = barRight - vW - 6f - barMaxW - 6f
                c.drawRoundRect(RectF(barX, y + 9f, barX + barMaxW, y + 15f), 3f, 3f, fill(TRACK_BG))
                val fillW = (barMaxW * pctVal / 100).coerceAtLeast(2.0).toFloat()
                c.drawRoundRect(RectF(barX, y + 9f, barX + fillW, y + 15f), 3f, 3f,
                    fill(barColor).apply { alpha = 180 })
                c.drawText(valStr, barRight - vW, y + 7f + ascent(vp), vp)
            }
            cursor.y = y + rh
        }
        cursor.y += 16f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isEmpty() || paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf("") }
    }

    // MARK: Primitives

    private fun fill(color: Int) = Paint().apply { this.color = color; style = Paint.Style.FILL }

    private fun tp(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun lp() = tp(8f, android.graphics.Color.GRAY, bold = true)

    private fun ascent(paint: Paint) = -paint.fontMetrics.ascent

    /**
     * Linha de barra com o % de "Sim" calculado só sobre quem respondeu a pergunta.
     * O nº de respostas vai no rótulo; sem nenhuma resposta, a barra não é desenhada.
     */
    private fun answeredRow(
        label: String,
        items: List<WellbeingCheckInAndroid>,
        sel: (WellbeingCheckInAndroid) -> Boolean?
    ): Pair<String, Double?> {
        val answers = items.mapNotNull(sel)
        if (answers.isEmpty()) return "$label (sem respostas)" to null
        return "$label (${answers.size} resp.)" to answers.count { it }.toDouble() / answers.size * 100
    }

    private fun yesNo(value: Boolean?) = when (value) {
        true -> "Sim"
        false -> "Não"
        null -> "Não perguntado (teste anterior)"
    }

    private fun dateFmt(d: Date) = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(d)

    private fun dateTimeFmt(d: Date) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(d)

    /** Ano de nascimento com a idade aproximada (só o ano é coletado: pode variar 1 ano). */
    private fun birthYearText(r: WellbeingReportAndroid): String {
        val year = r.healthProfile.birthYear
        if (year <= 0) return "Não informado"
        val currentYear = Calendar.getInstance().apply { time = r.generatedAt }.get(Calendar.YEAR)
        return "$year (cerca de ${currentYear - year} anos)"
    }

    /** Período dos dados: do início da janela (00:00) até a data de geração. */
    private fun periodStr(r: WellbeingReportAndroid): String =
        "${dateFmt(Date(r.windowStartMs))} a ${dateFmt(r.generatedAt)}"
}
