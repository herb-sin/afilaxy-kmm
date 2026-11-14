import SwiftUI

/// Indicação do "Guia Prático de Dispositivos Inalatórios" (LEDAR/UFAL Arapiraca), autorizada
/// pela coordenação do LEDAR por e-mail em 14/11/2026 nestas condições:
///  - só link para a página oficial (DOI), sem hospedar cópia (licença CC BY-NC-ND 4.0);
///  - crédito de AUTORIA, nunca apresentar autores, LEDAR ou UFAL como parceiros/apoiadores;
///  - material educativo complementar, reforçando a orientação do médico;
///  - acesso sempre gratuito, sem vínculo com conteúdo ou plano pago.
/// Espelho do Android: InhalerGuideScreen.kt. Mesmo texto no site (página /guia-inalatorio).
enum InhalerGuide {
    static let url = URL(string: "https://doi.org/10.46898/rfb.musghvgdt331")!
    static let title = "Guia Prático de Dispositivos Inalatórios"
    static let subtitle = "Principais dispositivos utilizados no Brasil no manejo das doenças pulmonares"
    static let content = "Passo a passo ilustrado da técnica correta de uso dos principais "
        + "dispositivos inalatórios disponíveis no Brasil, com cuidados de higienização e "
        + "conservação e técnicas de lavagem nasal."
    static let notice = "Material educativo complementar. Ele não substitui a orientação do seu "
        + "médico: siga sempre a prescrição, as doses e as instruções que você recebeu."
    static let authors = "Ana Dora Alécio Virtuoso Costa, Emylle Victória Cavalcante Costa, "
        + "Lívia Soares Bezerra, Jorge Samuel de Oliveira Júnior, Katiane de Lima Pinheiro, "
        + "Ryan Lucas Barbosa Oliveira, Thaysa Karlla Albuquerque Jatobá, Thalyta de Souza "
        + "Rodrigues Holanda e Laércia Karla Diega Paiva Ferreira."
    static let lab = "Laboratório de Epidemiologia de Doenças Alérgicas e Respiratórias "
        + "(LEDAR), Universidade Federal de Alagoas (UFAL), Campus Arapiraca."
    static let reference = "Belém: RFB Editora, 2026. ISBN 978-65-5337-373-0. "
        + "DOI 10.46898/rfb.musghvgdt331."
    static let license = "Distribuído gratuitamente pelos autores sob a licença Creative "
        + "Commons CC BY-NC-ND 4.0. O Afilaxy apenas indica o guia, com autorização da autoria; "
        + "os autores, o LEDAR e a UFAL não têm vínculo com o Afilaxy."
}

struct InhalerGuideView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(InhalerGuide.title)
                    .font(.title2).bold()
                Text(InhalerGuide.subtitle)
                    .font(.subheadline).italic()
                    .foregroundColor(.secondary)
                Text(InhalerGuide.content)
                    .font(.body)

                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "info.circle.fill")
                        .foregroundColor(.afiWarning)
                    Text(InhalerGuide.notice)
                        .font(.subheadline)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 12).fill(Color.afiWarning.opacity(0.12)))

                Link(destination: InhalerGuide.url) {
                    Label("Abrir o guia na página oficial", systemImage: "arrow.up.right.square")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.afiPrimary)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                Text("Abre no navegador, na página da obra na RFB Editora.")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Divider()

                Text("Autoria").font(.headline)
                Text(InhalerGuide.authors).font(.subheadline)
                Text(InhalerGuide.lab).font(.subheadline)
                Text(InhalerGuide.reference)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(InhalerGuide.license)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .navigationTitle("Como usar sua bombinha")
        .navigationBarTitleDisplayMode(.inline)
    }
}
