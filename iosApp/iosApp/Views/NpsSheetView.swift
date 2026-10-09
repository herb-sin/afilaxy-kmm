import SwiftUI

struct NpsSheetView: View {
    var onSubmit: (Int) -> Void
    var onSkip: () -> Void
    @State private var selected = -1

    var body: some View {
        NavigationStack {
            VStack(spacing: 28) {
                VStack(spacing: 8) {
                    Text("Você recomendaria o Afilaxy?")
                        .font(.title3).bold()
                    Text("De 0 (pouco provável) a 10 (com certeza).")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 24)

                VStack(spacing: 8) {
                    HStack(spacing: 6) {
                        ForEach(0...5, id: \.self) { i in npsButton(i) }
                    }
                    HStack(spacing: 6) {
                        ForEach(6...10, id: \.self) { i in npsButton(i) }
                    }
                }

                Spacer()

                HStack(spacing: 20) {
                    Button("Pular") { onSkip() }
                        .foregroundColor(.secondary)
                    Button("Enviar") { onSubmit(max(0, selected)) }
                        .buttonStyle(.borderedProminent)
                        .disabled(selected < 0)
                }
                .padding(.bottom, 32)
            }
            .padding(.horizontal)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    @ViewBuilder
    private func npsButton(_ score: Int) -> some View {
        let isSelected = score == selected
        Button { selected = score } label: {
            Text("\(score)")
                .font(.subheadline).bold()
                .frame(width: 40, height: 40)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isSelected ? Color.accentColor :
                              score <= 6  ? Color.red.opacity(0.15) :
                              score <= 8  ? Color.orange.opacity(0.15) :
                                            Color.green.opacity(0.15))
                )
                .foregroundColor(isSelected ? .white : .primary)
        }
    }
}
