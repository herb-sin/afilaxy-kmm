import SwiftUI

private struct Produto: Identifiable {
    let id: String
    let nome: String
    let descricao: String
    let preco: String
    let cupom: String?
    let farmacia: String?
}

private struct Evento: Identifiable {
    let id: String
    let titulo: String
    let descricao: String
    let data: String
    let local: String?
    let organizador: String?
}

private let produtos: [Produto] = [
    Produto(id: "1", nome: "Bombinha de Asma",
            descricao: "Bombinha para crises de asma. Disponível sem receita em algumas farmácias.",
            preco: "R$ 45,00", cupom: "AFILAXY10", farmacia: "Farmácias Nissei"),
    Produto(id: "2", nome: "Espaçador",
            descricao: "Espaçador para bombinha — melhora absorção do medicamento.",
            preco: "R$ 25,00", cupom: "ESPA20", farmacia: "Ultrafarma")
]

private let eventos: [Evento] = [
    Evento(id: "1", titulo: "Palestra sobre Asma",
           descricao: "Aprenda a controlar a asma com especialistas.",
           data: "15/12/2024", local: "Centro Comunitário", organizador: "ABRA"),
    Evento(id: "2", titulo: "Grupo de Apoio",
           descricao: "Encontro mensal de apoio para pacientes.",
           data: "20/12/2024", local: "Online", organizador: "Crônicos do Dia-a-Dia")
]

struct CommunityView: View {
    @State private var selectedTab = 0

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Produtos").tag(0)
                Text("Eventos").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()

            if selectedTab == 0 {
                List(produtos) { p in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(p.nome).font(.headline)
                        Text(p.descricao).font(.subheadline).foregroundColor(.secondary)
                        Text(p.preco).font(.subheadline).foregroundColor(.blue)
                        if let cupom = p.cupom {
                            Text("🏷️ Cupom: \(cupom)").font(.caption).foregroundColor(.green)
                        }
                        if let farmacia = p.farmacia {
                            Text("🏪 \(farmacia)").font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            } else {
                List(eventos) { e in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(e.titulo).font(.headline)
                        Text(e.descricao).font(.subheadline).foregroundColor(.secondary)
                        Text("📅 \(e.data)").font(.caption)
                        if let local = e.local {
                            Text("📍 \(local)").font(.caption).foregroundColor(.secondary)
                        }
                        if let org = e.organizador {
                            Text("👥 \(org)").font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("Comunidade")
        .navigationBarTitleDisplayMode(.inline)
    }
}
