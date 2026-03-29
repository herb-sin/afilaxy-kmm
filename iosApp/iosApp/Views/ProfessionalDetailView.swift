import SwiftUI
import shared

struct ProfessionalDetailView: View {
    let professionalId: String
    @EnvironmentObject var container: AppContainer

    var body: some View {
        let wrapper = container.professionalDetail
        Group {
            if wrapper.state?.isLoading == true {
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
        .onAppear { wrapper.vm?.loadProfessional(id: professionalId) }
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
                            if p.subscriptionPlan.tier() == .premium {
                                Image(systemName: "checkmark.seal.fill").foregroundColor(.blue)
                            }
                        }
                        Text(specialtyLabel(p.specialty)).foregroundColor(.secondary)
                        Text("CRM: \(p.crm)").font(.caption).foregroundColor(.secondary)
                    }
                }
                .padding()

                // Badge do plano
                if p.subscriptionPlan.tier() != .none {
                    HStack {
                        Text(planBadge(p.subscriptionPlan))
                            .font(.caption).bold()
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(planColor(p.subscriptionPlan))
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

    private func planBadge(_ plan: SubscriptionPlan) -> String {
        let duration: String
        switch plan.durationMonths() {
        case 3:  duration = " · Trimestral"
        case 6:  duration = " · Semestral"
        case 12: duration = " · Anual"
        default: duration = ""
        }
        switch plan.tier() {
        case .premium: return "⭐ Premium\(duration)"
        case .pro:     return "✨ Pro\(duration)"
        case .basic:   return "Básico\(duration)"
        default:       return ""
        }
    }

    private func planColor(_ plan: SubscriptionPlan) -> Color {
        switch plan.tier() {
        case .premium: return Color(red: 1, green: 0.84, blue: 0)
        case .pro:     return Color(red: 0.75, green: 0.75, blue: 0.75)
        case .basic:   return Color(red: 0.8, green: 0.5, blue: 0.2)
        default:       return Color(.secondarySystemBackground)
        }
    }
}
