import Foundation

/// Sinal em memória (não persistido) de que o usuário acabou de concluir login/cadastro.
///
/// Propositalmente NÃO usa UserDefaults/@AppStorage: o tour de boas-vindas deve aparecer
/// sempre que o usuário passar pela tela de Login (ou Cadastro, que reutiliza o mesmo
/// listener de auth de LoginView), mesmo que já o tenha visto antes (múltiplos logins).
/// Uma sessão retomada silenciosamente (app reaberto com o Firebase Auth já autenticado,
/// sem passar pela tela de Login) NÃO deve disparar o tour — por isso o flag só é setado
/// no momento em que `isLoggedIn` vira `true` a partir da tela de Login, nunca em função
/// de "usuário está autenticado".
enum PostLoginTourFlag {
    static var pending = false
}
