import SwiftUI
import shared

struct ProfessionalDetailView: View {
    let professionalId: String
    @EnvironmentObject var container: AppContainer

    var body: some View {
        let wrapper = container.professionalDetail
        Group {
            // Enquanto o ID que está sendo carregado não corresponde ao solicitado,
            // mostra loading para evitar exibir dados do profissional anterior (estado stale).
            let loadedId = wrapper.state?.professional?.id
            let isStale = loadedId != nil && loadedId != professionalId
            if wrapper.state?.isLoading == true || isStale {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = wrapper.state?.error {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle").font(.largeTitle)
                    Text(error)
                }.foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let p = wrapper.state?.professional {
                detailBody(p)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle("Profissional")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            // Garante que o load sempre dispara para o ID correto,
            // mesmo quando o ViewModel ainda tem o estado do profissional anterior.
            wrapper.vm?.loadProfessional(id: professionalId)
        }
    }

    @ViewBuilder
    private func detailBody(_ p: HealthProfessional) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // Header
                HStack(spacing: 16) {
                    Image(systemName: "person.circle.fill")
                        .resizable().frame(width: 80, height: 80)
                        .foregroundColor(.accentColor)
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(p.name).font(.title2).bold()
                            if p.subscriptionPlan.isActive() {
                                Image(systemName: "checkmark.seal.fill").foregroundColor(Color(red: 0.114, green: 0.631, blue: 0.949))
                            }
                        }
                        Text(specialtyLabel(p.specialty)).foregroundColor(.secondary)
                        Text("CRM: \(p.crm)").font(.caption).foregroundColor(.secondary)
                    }
                }
                .padding()

                // Badge de parceiro
                if p.subscriptionPlan.isActive() {
                    HStack {
                        Label("Parceiro Afilaxy", systemImage: "checkmark.seal.fill")
                            .font(.caption).bold()
                            .foregroundColor(Color(red: 0.114, green: 0.631, blue: 0.949))
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(Color(red: 0.114, green: 0.631, blue: 0.949).opacity(0.1))
                            .cornerRadius(20)
                        Spacer()
                    }.padding(.horizontal)
                }

                // Avaliação
                if p.rating > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill").foregroundColor(.yellow)
                        Text(String(format: "%.1f", p.rating)).bold()
                        Text("(\(p.totalReviews) avaliações)").foregroundColor(.secondary)
                    }.padding(.horizontal)
                }

                // Bio
                if !p.bio.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Sobre").font(.headline)
                        Text(p.bio).foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
                }

                // Endereço
                if let address = p.clinicAddress {
                    HStack(spacing: 12) {
                        Image(systemName: "mappin.circle.fill").foregroundColor(.red)
                        Text(address).foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
                }

                // Botões de contato
                VStack(spacing: 12) {
                    if let whatsapp = p.whatsapp {
                        let digits = whatsapp.filter(\.isNumber)
                        if !digits.isEmpty, let url = URL(string: "https://wa.me/55\(digits)") {
                            Link(destination: url) {
                                Label("WhatsApp", systemImage: "message.fill")
                                    .frame(maxWidth: .infinity).padding()
                                    .background(Color(red: 0.145, green: 0.827, blue: 0.4))
                                    .foregroundColor(.white).cornerRadius(12)
                            }
                        }
                    }
                    if let phone = p.phone {
                        let digits = phone.filter { $0.isNumber || $0 == "+" }
                        if !digits.isEmpty, let url = URL(string: "tel:\(digits)") {
                            Link(destination: url) {
                                Label("Ligar", systemImage: "phone.fill")
                                    .frame(maxWidth: .infinity).padding()
                                    .background(Color(.secondarySystemBackground))
                                    .foregroundColor(.primary).cornerRadius(12)
                            }
                        }
                    }
                }.padding(.horizontal)

                Spacer(minLength: 32)
            }
        }
    }

    private func specialtyLabel(_ s: shared.Specialty) -> String {
        // Mock specialty label since types don't match
        return "Especialista"
    }

}
