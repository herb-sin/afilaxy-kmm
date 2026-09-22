package com.afilaxy.app.ui.onboarding

/**
 * Sinal em memória (não persistido) de que o usuário acabou de concluir login/cadastro.
 *
 * Propositalmente NÃO usa SharedPreferences: o tour de boas-vindas deve aparecer sempre
 * que o usuário passar pela tela de Login ou Cadastro, mesmo que já o tenha visto antes
 * (múltiplos logins). Uma sessão retomada silenciosamente (app reaberto com o Firebase Auth
 * já autenticado, sem passar pela tela de Login) NÃO deve disparar o tour — por isso o flag
 * só é setado nos callbacks de sucesso de LoginScreen/RegisterScreen, nunca em função de
 * "usuário está logado".
 */
object PostLoginTour {
    @Volatile
    var pending: Boolean = false
}
