import Foundation
import FirebaseAuth
import FirebaseFirestore

/// Consentimento específico para pesquisa (LGPD art. 11, I): opt-in, começa desmarcado e
/// pode ser revogado a qualquer momento. Fica gravado em users/{uid}, e não só no aparelho,
/// porque é o que scripts/export_ml_dataset.py usa para decidir quem entra na exportação.
/// Espelho do Android: androidApp/.../ui/components/ResearchConsent.kt (mesmos campos, versão e texto).
enum ResearchConsent {
    static let field = "researchConsent"
    static let fieldAt = "researchConsentAt"
    static let fieldVersion = "researchConsentVersion"

    /// Muda quando o texto abaixo mudar de sentido: o consentimento antigo não cobre o novo texto.
    static let version = "2026-11"

    static let title = "Pesquisa científica (opcional)"
    static let summary = "Autorizo o uso, em pesquisas sobre asma e no desenvolvimento de "
        + "modelos de previsão de crises, dos meus check-ins, testes de controle da asma (ACT), "
        + "índices de risco, pedidos de ajuda, ano de nascimento e perfil de saúde. Os dados saem "
        + "identificados só por um código, sem nome, e-mail, mensagens ou localização exata (no "
        + "máximo a região, num raio de cerca de 1 km). É opcional, e você pode revogar quando quiser."

    private static var userDoc: DocumentReference? {
        guard let uid = Auth.auth().currentUser?.uid else { return nil }
        return Firestore.firestore().collection("users").document(uid)
    }

    /// Valor gravado; nil quando nunca foi respondido ou quando a leitura falhou.
    static func load() async -> Bool? {
        guard let doc = userDoc, let snapshot = try? await doc.getDocument() else { return nil }
        return snapshot.data()?[field] as? Bool
    }

    private static func write(_ granted: Bool, completion: ((Error?) -> Void)? = nil) {
        guard let doc = userDoc else {
            completion?(NSError(domain: "ResearchConsent", code: 1))
            return
        }
        doc.updateData([
            field: granted,
            fieldAt: FieldValue.serverTimestamp(),
            fieldVersion: version
        ]) { error in completion?(error) }
    }

    /// Grava e aguarda a confirmação do servidor; false se falhar.
    static func save(_ granted: Bool) async -> Bool {
        await withCheckedContinuation { continuation in
            write(granted) { error in continuation.resume(returning: error == nil) }
        }
    }

    /// Para a tela de consentimento, que segue adiante logo em seguida: dispara a gravação sem
    /// esperar. Nunca grava "não" por cima de um valor que não se conseguiu ler — num aparelho
    /// novo, a leitura pode ter falhado e o usuário ter dito "sim" em outro aparelho.
    static func saveFromOnboarding(_ granted: Bool, stored: Bool?) {
        let changed = stored == nil ? granted : granted != stored
        if changed { write(granted) }
    }
}
