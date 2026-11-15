package com.afilaxy.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Consentimento específico para pesquisa (LGPD art. 11, I): opt-in, começa desmarcado e
 * pode ser revogado a qualquer momento. Fica gravado em users/{uid}, e não só no aparelho,
 * porque é o que scripts/export_ml_dataset.py usa para decidir quem entra na exportação.
 * Espelho no iOS: iosApp/Helpers/ResearchConsent.swift (mesmos campos, versão e texto).
 */
object ResearchConsent {
    const val FIELD = "researchConsent"
    const val FIELD_AT = "researchConsentAt"
    const val FIELD_VERSION = "researchConsentVersion"

    /**
     * Muda sempre que o texto abaixo mudar, para saber qual texto cada pessoa aceitou; se
     * mudar de sentido, o consentimento antigo não cobre o novo. Acrescente a nova versão em
     * ACCEPTED_CONSENT_VERSIONS (scripts/export_ml_dataset.py).
     *  - 2026-11: texto inicial;
     *  - 2026-11b: mesmos dados e finalidade, com o porquê de participar e a garantia de que
     *    recusar não muda o acesso ao app.
     */
    const val VERSION = "2026-11b"

    const val TITLE = "Pesquisa científica (opcional)"
    const val DESCRIPTION = "Ao ativar esta opção, você ajuda o Afilaxy a tornar a previsão de " +
        "risco de crise mais precisa, desenvolvendo e aprimorando modelos de aprendizado de " +
        "máquina que buscam antecipar crises. As melhorias chegam a todos, inclusive você. Os " +
        "dados saem identificados só por um código, sem nome, e-mail, mensagens ou localização " +
        "exata (no máximo a região, num raio de cerca de 1 km).\n\n" +
        "Autorizo o uso, em pesquisas sobre asma e no desenvolvimento de modelos de previsão de " +
        "crises, dos meus check-ins, testes de controle da asma (ACT), índices de risco, pedidos " +
        "de ajuda, ano de nascimento e perfil de saúde.\n\n" +
        "Participar ou não não muda nada no seu acesso ao app, e você pode revogar quando quiser."

    private fun userDoc() = FirebaseAuth.getInstance().currentUser?.uid?.let {
        FirebaseFirestore.getInstance().collection("users").document(it)
    }

    /** Valor gravado; null quando nunca foi respondido ou quando a leitura falhou. */
    suspend fun load(): Boolean? = try {
        userDoc()?.get()?.await()?.getBoolean(FIELD)
    } catch (e: Exception) {
        null
    }

    private fun write(granted: Boolean) = userDoc()?.update(
        mapOf(
            FIELD to granted,
            FIELD_AT to FieldValue.serverTimestamp(),
            FIELD_VERSION to VERSION
        )
    )

    /** Grava e aguarda a confirmação do servidor; false se falhar. */
    suspend fun save(granted: Boolean): Boolean {
        val task = write(granted) ?: return false
        return try {
            task.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Para a tela de consentimento, que navega logo em seguida: dispara a gravação sem esperar
     * (o SDK do Firestore conclui mesmo depois que a tela sai). Nunca grava "não" por cima de
     * um valor que não se conseguiu ler — num aparelho novo, a leitura pode ter falhado e o
     * usuário ter dito "sim" em outro aparelho.
     */
    fun saveFromOnboarding(granted: Boolean, stored: Boolean?) {
        val changed = if (stored == null) granted else granted != stored
        if (changed) write(granted)
    }
}

/** Linha com chave liga/desliga para Perfil > Privacidade e conta (revogação). */
@Composable
fun ResearchConsentSwitchRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        granted = ResearchConsent.load() ?: false
        loaded = true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(ResearchConsent.TITLE, style = MaterialTheme.typography.bodyLarge)
            Text(
                ResearchConsent.DESCRIPTION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = granted,
            enabled = loaded && !saving,
            onCheckedChange = { new ->
                saving = true
                granted = new
                scope.launch {
                    if (!ResearchConsent.save(new)) {
                        granted = !new
                        Toast.makeText(
                            context,
                            "Não foi possível salvar agora. Verifique a conexão e tente de novo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    saving = false
                }
            }
        )
    }
}
