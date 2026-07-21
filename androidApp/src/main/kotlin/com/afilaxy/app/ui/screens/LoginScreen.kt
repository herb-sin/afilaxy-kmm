package com.afilaxy.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.app.R
import com.afilaxy.app.security.InputSanitizer
import com.afilaxy.app.util.FcmHelper
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.presentation.login.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Tela de Login
 * Conectada ao LoginViewModel compartilhado (KMM)
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val authRepository: AuthRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }
    var resetEmailSent by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.onGoogleSignInResult(idToken, null)
            } else {
                viewModel.onGoogleSignInError("ID Token nulo — web_client_id não configurado no Firebase")
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) { // 12501 = usuário cancelou
                viewModel.onGoogleSignInError("Google Sign-In falhou (código ${e.statusCode})")
            }
        }
    }
    
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            FcmHelper.requestFcmToken(authRepository)
            onLoginSuccess()
        }
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.afilaxy_logo),
                contentDescription = "Afilaxy Logo",
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Campo de Email
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de Senha
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "👁️" else "👁️‍🗨️",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus()
                        viewModel.onLoginClick()
                    }
                ),
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Mensagem de erro
            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = friendlyLoginError(state.error ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botão de Login
            Button(
                onClick = {
                    val sanitizedEmail = InputSanitizer.sanitizeEmail(state.email)
                    if (!InputSanitizer.isValidEmail(sanitizedEmail)) {
                        // Validação será feita pelo ViewModel
                    }
                    viewModel.onLoginClick()
                },
                enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  ou  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    // Força exibir o seletor de contas mesmo que já haja uma sessão Google
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entrar com Google",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val activity = context as? Activity ?: return@OutlinedButton
                    val provider = OAuthProvider.newBuilder("apple.com").apply {
                        scopes = listOf("email", "name")
                        addCustomParameter("locale", "pt_BR")
                    }.build()
                    // Android web-flow: Firebase handles OAuth+PKCE internally.
                    // After success the user is already signed into Firebase —
                    // only createSession() is needed; no second signInWithCredential call.
                    FirebaseAuth.getInstance()
                        .startActivityForSignInWithProvider(activity, provider)
                        .addOnSuccessListener {
                            coroutineScope.launch {
                                authRepository.createSession()
                                FcmHelper.requestFcmToken(authRepository)
                                onLoginSuccess()
                            }
                        }
                        .addOnFailureListener { /* User cancelled or error — no action needed */ }
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Entrar com Apple",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão de Registro
            TextButton(
                onClick = onRegisterClick,
                enabled = !state.isLoading
            ) {
                Text("Não tem uma conta? Criar conta")
            }

            // Esqueci minha senha
            TextButton(
                onClick = {
                    val email = state.email.trim()
                    if (email.isBlank()) {
                        resetError = "Digite seu e-mail acima para recuperar a senha."
                        return@TextButton
                    }
                    coroutineScope.launch {
                        authRepository.sendPasswordResetEmail(email)
                            .onSuccess { resetEmailSent = true; resetError = null }
                            .onFailure { resetError = "Não foi possível enviar o email. Verifique o endereço."; resetEmailSent = false }
                    }
                },
                enabled = !state.isLoading
            ) {
                Text(
                    text = "Esqueci minha senha",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Feedback de redefinição de senha
            if (resetEmailSent) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "✅ Email de redefinição enviado! Verifique sua caixa de entrada.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (resetError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resetError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun friendlyLoginError(raw: String): String {
    val msg = raw.lowercase()
    return when {
        msg.contains("password is invalid") ||
        msg.contains("wrong password") ||
        msg.contains("invalid credential") ||
        msg.contains("auth credential is incorrect") ->
            "Senha incorreta. Verifique e tente novamente."

        msg.contains("no user record") ||
        msg.contains("user not found") ||
        msg.contains("there is no user") ->
            "Nenhuma conta encontrada com esse e-mail."

        msg.contains("badly formatted") ||
        msg.contains("invalid email") ||
        msg.contains("email address is badly") ->
            "Endereço de e-mail inválido."

        msg.contains("network error") ||
        msg.contains("network request failed") ||
        msg.contains("unable to resolve host") ->
            "Sem conexão com a internet. Verifique sua rede."

        msg.contains("too many requests") ||
        msg.contains("too many attempts") ||
        msg.contains("temporarily disabled") ->
            "Muitas tentativas. Aguarde alguns minutos e tente novamente."

        msg.contains("weak password") ->
            "Senha muito fraca. Use ao menos 6 caracteres."

        msg.contains("email already in use") ->
            "Este e-mail já está cadastrado. Faça login ou recupere sua senha."

        msg.contains("user disabled") ->
            "Esta conta foi desativada. Entre em contato com o suporte."

        else -> "Erro ao entrar. Verifique suas credenciais e tente novamente."
    }
}
