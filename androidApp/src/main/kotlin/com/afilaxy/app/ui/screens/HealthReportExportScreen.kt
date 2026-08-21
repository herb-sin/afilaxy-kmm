package com.afilaxy.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.afilaxy.presentation.professional.CrmResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ── Step machine ──────────────────────────────────────────────────────────────

private sealed class ReportStep {
    object CrmEntry : ReportStep()
    object Validating : ReportStep()
    data class Validated(val doctor: CrmResult) : ReportStep()
    object Generating : ReportStep()
    data class Failed(val message: String) : ReportStep()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthReportExportScreen(onNavigateBack: () -> Unit) {
    var step by remember { mutableStateOf<ReportStep>(ReportStep.CrmEntry) }
    var crm by remember { mutableStateOf("") }
    var selectedUf by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                is ReportStep.CrmEntry, is ReportStep.Validating -> CrmEntryContent(
                    crm = crm,
                    selectedUf = selectedUf,
                    isValidating = step is ReportStep.Validating,
                    onCrmChange = { crm = it.filter { c -> c.isDigit() }.take(10) },
                    onUfChange = { selectedUf = it },
                    onValidate = {
                        scope.launch {
                            step = ReportStep.Validating
                            step = validateCrm(crm, selectedUf)
                        }
                    }
                )
                is ReportStep.Validated -> ValidatedContent(
                    doctor = s.doctor,
                    onGenerate = {
                        scope.launch {
                            step = ReportStep.Generating
                            val ok = generateAndShare(context, s.doctor)
                            step = if (ok) ReportStep.Validated(s.doctor)
                                   else ReportStep.Failed("Não foi possível gerar o PDF. Tente novamente.")
                        }
                    },
                    onChangeDoctor = { step = ReportStep.CrmEntry }
                )
                is ReportStep.Generating -> GeneratingContent()
                is ReportStep.Failed -> FailedContent(s.message) { step = ReportStep.CrmEntry }
            }
        }
    }
}

// ── Step 1: CRM Entry ─────────────────────────────────────────────────────────

private val UF_LIST = listOf(
    "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
    "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrmEntryContent(
    crm: String,
    selectedUf: String,
    isValidating: Boolean,
    onCrmChange: (String) -> Unit,
    onUfChange: (String) -> Unit,
    onValidate: () -> Unit
) {
    var ufExpanded by remember { mutableStateOf(false) }
    val canValidate = crm.isNotBlank() && selectedUf.length == 2 && !isValidating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Acesso por CRM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Informe o CRM do profissional que irá receber o relatório. O nome e número serão registrados no documento como destinatário.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        OutlinedTextField(
            value = crm,
            onValueChange = onCrmChange,
            label = { Text("Número do CRM") },
            placeholder = { Text("Ex: 123456") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(expanded = ufExpanded, onExpandedChange = { ufExpanded = it }) {
            OutlinedTextField(
                value = selectedUf.ifEmpty { "Selecione a UF" },
                onValueChange = {},
                readOnly = true,
                label = { Text("UF de registro") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ufExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = ufExpanded, onDismissRequest = { ufExpanded = false }) {
                UF_LIST.forEach { uf ->
                    DropdownMenuItem(
                        text = { Text(uf) },
                        onClick = { onUfChange(uf); ufExpanded = false }
                    )
                }
            }
        }

        Button(
            onClick = onValidate,
            enabled = canValidate,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isValidating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.VerifiedUser, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Validar e Continuar", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Step 2: Validated ─────────────────────────────────────────────────────────

@Composable
private fun ValidatedContent(
    doctor: CrmResult,
    onGenerate: () -> Unit,
    onChangeDoctor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Text("Médico confirmado no CFM", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                InfoRow("Nome", doctor.name)
                InfoRow("CRM", "${doctor.crm}/${doctor.uf}")
                InfoRow("Especialidade", doctor.specialty.ifEmpty { "Não informada" })
                val active = doctor.situation.lowercase().let { it.contains("ativo") || it.contains("regular") }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Situação: ", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(doctor.situation.ifEmpty { "Não informada" },
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = if (active) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                }
                Text("Fonte: Conselho Federal de Medicina (CFM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Conteúdo do Relatório (30 dias)",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                listOf(
                    "Resumo executivo e adesão ao monitoramento",
                    "Qualidade do sono (check-in matinal)",
                    "Bem-estar e energia matinal",
                    "Avaliação de bem-estar noturno",
                    "Atividade física e autocuidado",
                    "Eventos de emergência acionados"
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

        TextButton(onClick = onChangeDoctor, modifier = Modifier.fillMaxWidth()) {
            Text("Trocar profissional")
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
            Text("Coletando seus dados dos últimos 30 dias.",
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── Logic ─────────────────────────────────────────────────────────────────────

private suspend fun validateCrm(crm: String, uf: String): ReportStep = try {
    val result = FirebaseFunctions.getInstance("us-central1")
        .getHttpsCallable("validateCrm")
        .call(mapOf("crm" to crm.trim(), "uf" to uf))
        .await()
    @Suppress("UNCHECKED_CAST")
    val data = result.getData() as? Map<String, Any> ?: emptyMap()
    if (data["found"] == true) {
        ReportStep.Validated(CrmResult(
            name      = data["name"]      as? String ?: "",
            specialty = data["specialty"] as? String ?: "",
            situation = data["situation"] as? String ?: "",
            uf        = data["uf"]        as? String ?: uf,
            crm       = data["crm"]       as? String ?: crm
        ))
    } else {
        ReportStep.Failed("CRM $crm/$uf não encontrado no CFM.\nVerifique o número e a UF informados.")
    }
} catch (e: FirebaseFunctionsException) {
    ReportStep.Failed(
        if (e.code == FirebaseFunctionsException.Code.UNAVAILABLE)
            "Serviço do CFM indisponível. Tente novamente mais tarde."
        else "Erro ao consultar CRM. Verifique os dados e tente novamente."
    )
} catch (e: Exception) {
    ReportStep.Failed("Erro de conexão. Verifique sua internet e tente novamente.")
}

private suspend fun generateAndShare(context: Context, doctor: CrmResult): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val patientName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuário"
    val cutoffMs = System.currentTimeMillis() - 30L * 24 * 3_600_000

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

    val checkIns = checkInDocs.documents.map { doc ->
        WellbeingCheckInAndroid(
            type       = doc.getString("type") ?: "",
            wellbeingA = doc.getBoolean("wellbeingA") ?: false,
            wellbeingB = doc.getBoolean("wellbeingB") ?: false,
            wellbeingC = doc.getBoolean("wellbeingC") ?: false
        )
    }

    val report = WellbeingReportAndroid(
        patientName       = patientName,
        doctor            = doctor,
        checkIns          = checkIns,
        emergencyCount30d = emergency30d(statsDoc),
        generatedAt       = Date()
    )

    val file = WellbeingPDFAndroid.generate(context, report) ?: return false

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
    return true
}

private fun emergency30d(statsDoc: DocumentSnapshot?): Int {
    val daily = statsDoc?.get("dailyCount") as? Map<*, *>
        ?: return statsDoc?.getLong("totalEmergencies")?.toInt() ?: 0
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    var sum = 0
    repeat(30) {
        sum += (daily[fmt.format(cal.time)] as? Long)?.toInt() ?: 0
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return sum
}

// ── Data Models ───────────────────────────────────────────────────────────────

private data class WellbeingCheckInAndroid(
    val type: String,
    val wellbeingA: Boolean,
    val wellbeingB: Boolean,
    val wellbeingC: Boolean
)

private data class WellbeingReportAndroid(
    val patientName: String,
    val doctor: CrmResult,
    val checkIns: List<WellbeingCheckInAndroid>,
    val emergencyCount30d: Int,
    val generatedAt: Date
)

// ── PDF Generator ─────────────────────────────────────────────────────────────

private object WellbeingPDFAndroid {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 50f
    private val CONTENT_W get() = PAGE_W - MARGIN * 2

    private val PRIMARY  = android.graphics.Color.rgb(0, 98, 143)
    private val MORNING  = android.graphics.Color.rgb(230, 81, 0)
    private val EVENING  = android.graphics.Color.rgb(26, 35, 126)
    private val CRITICAL = android.graphics.Color.rgb(186, 26, 26)
    private val ROW_ALT  = android.graphics.Color.rgb(247, 247, 247)
    private val TRACK_BG = android.graphics.Color.rgb(220, 220, 220)

    fun generate(context: Context, report: WellbeingReportAndroid): File? {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas

        var y = header(c, report)
        y = infoBox(c, report, y)
        y = summary(c, report, y)
        y = morningSection(c, report, y)
        y = eveningSection(c, report, y)
        criticalSection(c, report, y)
        footer(c, report)

        doc.finishPage(page)
        return try {
            val dir = File(context.cacheDir, "reports").also { it.mkdirs() }
            val file = File(dir, "relatorio_afilaxy_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) { doc.close(); null }
    }

    // MARK: Header

    private fun header(c: android.graphics.Canvas, r: WellbeingReportAndroid): Float {
        val h = 88f
        c.drawRect(RectF(0f, 0f, PAGE_W.toFloat(), h), fill(PRIMARY))
        c.drawText("AFILAXY", MARGIN, 18f + ascent(tp(22f, android.graphics.Color.WHITE, bold = true)), tp(22f, android.graphics.Color.WHITE, bold = true))
        c.drawText("Relatório de Monitoramento de Saúde", MARGIN, 46f + ascent(tp(13f, android.graphics.Color.WHITE)),
            tp(13f, android.graphics.Color.WHITE))
        val datePaint = tp(10f, android.graphics.Color.WHITE).apply { alpha = 200 }
        val dateStr = "Gerado em ${dateFmt(r.generatedAt)}"
        c.drawText(dateStr, PAGE_W - MARGIN - datePaint.measureText(dateStr), 40f + ascent(datePaint), datePaint)
        return h + 22f
    }

    // MARK: Info Box

    private fun infoBox(c: android.graphics.Canvas, r: WellbeingReportAndroid, y: Float): Float {
        val h = 78f
        c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + h), fill(android.graphics.Color.rgb(245, 245, 245)))

        val lx = MARGIN + 10f
        c.drawText("PACIENTE", lx, y + 10f + ascent(lp()), lp())
        c.drawText(r.patientName, lx, y + 22f + ascent(tp(12f, android.graphics.Color.BLACK, bold = true)),
            tp(12f, android.graphics.Color.BLACK, bold = true))
        c.drawText("PERÍODO", lx, y + 46f + ascent(lp()), lp())
        c.drawText(periodStr(), lx, y + 58f + ascent(tp(10f, android.graphics.Color.DKGRAY)),
            tp(10f, android.graphics.Color.DKGRAY))

        val rx = PAGE_W - MARGIN - 205f
        c.drawText("DESTINATÁRIO", rx, y + 10f + ascent(lp()), lp())
        c.drawText(r.doctor.name.ifEmpty { "—" }, rx, y + 22f + ascent(tp(11f, android.graphics.Color.BLACK, bold = true)),
            tp(11f, android.graphics.Color.BLACK, bold = true))
        c.drawText("CRM ${r.doctor.crm}/${r.doctor.uf}", rx, y + 40f + ascent(tp(10f, android.graphics.Color.DKGRAY)),
            tp(10f, android.graphics.Color.DKGRAY))
        if (r.doctor.specialty.isNotEmpty())
            c.drawText(r.doctor.specialty, rx, y + 56f + ascent(tp(9f, android.graphics.Color.GRAY)),
                tp(9f, android.graphics.Color.GRAY))

        return y + h + 20f
    }

    // MARK: Sections

    private fun summary(c: android.graphics.Canvas, r: WellbeingReportAndroid, y: Float): Float {
        val total = r.checkIns.size
        val adherence = total * 100 / 60
        val critical = r.checkIns.count { !it.wellbeingA && !it.wellbeingB && !it.wellbeingC }
        return plainRows(c, listOf(
            "Check-ins realizados (30 dias)"        to "$total de 60 possíveis",
            "Taxa de adesão ao monitoramento"       to "$adherence%",
            "Pedidos de ajuda emergencial"          to "${r.emergencyCount30d} ocorrência(s)",
            "Dias com bem-estar crítico registrado" to "$critical ocorrência(s)"
        ), sectionHeader(c, "1. RESUMO EXECUTIVO", y, PRIMARY))
    }

    private fun morningSection(c: android.graphics.Canvas, r: WellbeingReportAndroid, y: Float): Float {
        val m = r.checkIns.filter { it.type == "MORNING" }
        return barRows(c, listOf(
            "Registros de manhã: ${m.size}"  to null,
            "\"Dormi bem esta noite\""        to pct(m) { it.wellbeingA },
            "\"Me sinto bem esta manhã\""    to pct(m) { it.wellbeingB },
            "\"Estou com boa energia\""      to pct(m) { it.wellbeingC }
        ), sectionHeader(c, "2. CHECK-IN MATINAL", y, MORNING), MORNING)
    }

    private fun eveningSection(c: android.graphics.Canvas, r: WellbeingReportAndroid, y: Float): Float {
        val e = r.checkIns.filter { it.type == "EVENING" }
        return barRows(c, listOf(
            "Registros noturnos: ${e.size}"    to null,
            "\"Tive um bom dia\""              to pct(e) { it.wellbeingA },
            "\"Pratiquei atividade física\""   to pct(e) { it.wellbeingB },
            "\"Me cuidei bem hoje\""          to pct(e) { it.wellbeingC }
        ), sectionHeader(c, "3. CHECK-IN NOTURNO", y, EVENING), EVENING)
    }

    private fun criticalSection(c: android.graphics.Canvas, r: WellbeingReportAndroid, y: Float): Float {
        val critical = r.checkIns.count { !it.wellbeingA && !it.wellbeingB && !it.wellbeingC }
        return plainRows(c, listOf(
            "Pedidos de ajuda emergencial (período)" to "${r.emergencyCount30d}",
            "Dias com bem-estar mínimo registrado"   to "$critical"
        ), sectionHeader(c, "4. EVENTOS CRÍTICOS", y, CRITICAL))
    }

    private fun footer(c: android.graphics.Canvas, r: WellbeingReportAndroid) {
        val fy = PAGE_H - 52f
        c.drawRect(RectF(MARGIN, fy, MARGIN + CONTENT_W, fy + 0.5f), fill(android.graphics.Color.LTGRAY))
        val text = "Este relatório foi gerado automaticamente pelo aplicativo Afilaxy com base nas respostas fornecidas pelo próprio usuário. Não substitui avaliação clínica presencial. Gerado em ${dateFmt(r.generatedAt)}."
        val tp = TextPaint().apply { color = android.graphics.Color.GRAY; textSize = 8f; isAntiAlias = true }
        val sl = StaticLayout.Builder.obtain(text, 0, text.length, tp, CONTENT_W.toInt()).build()
        c.save(); c.translate(MARGIN, fy + 6f); sl.draw(c); c.restore()
    }

    // MARK: Row Drawers

    private fun sectionHeader(c: android.graphics.Canvas, title: String, y: Float, color: Int): Float {
        c.drawText(title, MARGIN, y + ascent(tp(11f, color, bold = true)), tp(11f, color, bold = true))
        val a = android.graphics.Color.argb(64,
            android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
        c.drawRect(RectF(MARGIN, y + 16f, MARGIN + CONTENT_W, y + 17f), fill(a))
        return y + 22f
    }

    private fun plainRows(c: android.graphics.Canvas, rows: List<Pair<String, String>>, y: Float): Float {
        var y = y
        val rh = 21f
        rows.forEachIndexed { i, (label, value) ->
            if (i % 2 == 0) c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rh), fill(ROW_ALT))
            val lp = tp(10f, android.graphics.Color.DKGRAY)
            val vp = tp(10f, android.graphics.Color.BLACK, bold = true)
            c.drawText(label, MARGIN + 8f, y + 5f + ascent(lp), lp)
            c.drawText(value, MARGIN + CONTENT_W - vp.measureText(value) - 8f, y + 5f + ascent(vp), vp)
            y += rh
        }
        return y + 16f
    }

    private fun barRows(
        c: android.graphics.Canvas,
        rows: List<Pair<String, Double?>>,
        y: Float,
        barColor: Int
    ): Float {
        var y = y
        val rh = 24f
        val barMaxW = 110f
        val barRight = MARGIN + CONTENT_W - 8f
        rows.forEachIndexed { i, (label, pctVal) ->
            if (i % 2 == 0) c.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rh), fill(ROW_ALT))
            val lp = tp(10f, android.graphics.Color.DKGRAY)
            c.drawText(label, MARGIN + 8f, y + 7f + ascent(lp), lp)
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
            y += rh
        }
        return y + 16f
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

    private fun pct(items: List<WellbeingCheckInAndroid>, sel: (WellbeingCheckInAndroid) -> Boolean): Double? {
        if (items.isEmpty()) return null
        return items.count { sel(it) }.toDouble() / items.size * 100
    }

    private fun dateFmt(d: Date) = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(d)

    private fun periodStr(): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -29) }
        return "${fmt.format(cal.time)} – ${fmt.format(Date())}"
    }
}
