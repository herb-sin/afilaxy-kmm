package com.afilaxy.app.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ── Step machine ──────────────────────────────────────────────────────────────

private sealed class ExportStep {
    object Idle : ExportStep()
    object Generating : ExportStep()
    object Done : ExportStep()
    data class Failed(val message: String) : ExportStep()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(onNavigateBack: () -> Unit) {
    var step by remember { mutableStateOf<ExportStep>(ExportStep.Idle) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar Meus Dados") },
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
                is ExportStep.Idle, is ExportStep.Done -> IdleContent(
                    onGenerate = {
                        scope.launch {
                            step = ExportStep.Generating
                            val ok = generateAndShareExport(context)
                            step = if (ok) ExportStep.Done
                                   else ExportStep.Failed("Não foi possível gerar o arquivo. Tente novamente.")
                        }
                    }
                )
                is ExportStep.Generating -> GeneratingContent()
                is ExportStep.Failed -> FailedContent(s.message) { step = ExportStep.Idle }
            }
        }
    }
}

// ── Idle ──────────────────────────────────────────────────────────────────────

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
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Seus dados, no seu controle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Gere um arquivo com os dados pessoais que você forneceu ao Afilaxy. Direito garantido pela LGPD.",
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
                    Text("Conteúdo do Arquivo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                listOf(
                    "Perfil e Perfil Médico (alergias, comorbidades, contato de emergência)",
                    "Histórico completo de check-ins",
                    "Histórico de score de risco",
                    "Emergências em que você participou (como solicitante ou helper)",
                    "Avaliações enviadas e recebidas"
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
            Text("Gerar Arquivo de Dados", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Generating / Failed ───────────────────────────────────────────────────────

@Composable
private fun GeneratingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Reunindo seus dados...", style = MaterialTheme.typography.titleMedium)
            Text("Isso pode levar alguns segundos.",
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
            Text("Erro ao gerar arquivo", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

// ── Logic ─────────────────────────────────────────────────────────────────────

private suspend fun generateAndShareExport(context: Context): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val db = FirebaseFirestore.getInstance()

    val root = JSONObject()
    root.put("exportadoEm", isoNow())
    root.put("usuarioId", uid)

    val userDoc = try { db.collection("users").document(uid).get().await() } catch (e: Exception) { null }
    root.put("perfil", userDoc?.data?.toJsonSafe() ?: JSONObject.NULL)

    val checkIns = try {
        db.collection("checkins").document(uid).collection("responses").get().await()
    } catch (e: Exception) { null }
    root.put("checkins", checkIns?.documents?.toJsonArray() ?: JSONArray())

    val riskSnapshots = try {
        db.collection("risk_scores").document(uid).collection("snapshots").get().await()
    } catch (e: Exception) { null }
    root.put("historicoDeRisco", riskSnapshots?.documents?.toJsonArray() ?: JSONArray())

    val statsDoc = try { db.collection("user_stats").document(uid).get().await() } catch (e: Exception) { null }
    root.put("estatisticas", statsDoc?.data?.toJsonSafe() ?: JSONObject.NULL)

    val helperDoc = try { db.collection("helpers").document(uid).get().await() } catch (e: Exception) { null }
    root.put("perfilHelper", helperDoc?.data?.toJsonSafe() ?: JSONObject.NULL)

    val asRequester = try {
        db.collection("emergency_requests").whereEqualTo("requesterId", uid).get().await().documents
    } catch (e: Exception) { emptyList() }
    root.put("emergenciasComoSolicitante", asRequester.toJsonArray())

    val asHelper = try {
        db.collection("emergency_requests").whereEqualTo("helperId", uid).get().await().documents
    } catch (e: Exception) { emptyList() }
    root.put("emergenciasComoHelper", asHelper.toJsonArray())

    val reviewsAsReviewer = try {
        db.collection("reviews").whereEqualTo("reviewerId", uid).get().await().documents
    } catch (e: Exception) { emptyList() }
    val reviewsAsReviewed = try {
        db.collection("reviews").whereEqualTo("reviewedId", uid).get().await().documents
    } catch (e: Exception) { emptyList() }
    root.put("avaliacoes", (reviewsAsReviewer + reviewsAsReviewed).distinctBy { it.id }.toJsonArray())

    val file = writeExportFile(context, root) ?: return false

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar meus dados"))
    return true
}

private fun writeExportFile(context: Context, json: JSONObject): File? = try {
    val dir = File(context.cacheDir, "reports").also { it.mkdirs() }
    val file = File(dir, "meus_dados_afilaxy_${System.currentTimeMillis()}.json")
    FileOutputStream(file).use { it.write(json.toString(2).toByteArray()) }
    file
} catch (e: Exception) { null }

// ── Sanitização (tipos nativos do Firestore → tipos serializáveis em JSON) ────

private fun List<DocumentSnapshot>.toJsonArray(): JSONArray {
    val arr = JSONArray()
    forEach { doc ->
        val obj = (doc.data?.toJsonSafe() as? JSONObject) ?: JSONObject()
        obj.put("id", doc.id)
        arr.put(obj)
    }
    return arr
}

private fun Any?.toJsonSafe(): Any = when (this) {
    null -> JSONObject.NULL
    is GeoPoint -> JSONObject().apply { put("latitude", latitude); put("longitude", longitude) }
    is Timestamp -> isoFormat(toDate())
    is Map<*, *> -> JSONObject().apply { forEach { (k, v) -> put(k.toString(), v.toJsonSafe()) } }
    is List<*> -> JSONArray().apply { forEach { put(it.toJsonSafe()) } }
    else -> this
}

private fun isoFormat(date: Date): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(date)

private fun isoNow(): String = isoFormat(Date())
