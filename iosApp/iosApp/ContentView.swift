import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "lungs.fill")
                .resizable()
                .frame(width: 100, height: 100)
                .foregroundColor(.blue)
            
            Text("Afilaxy")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            Text("Health Equity para Asma")
                .font(.subheadline)
                .foregroundColor(.gray)
            
            Spacer().frame(height: 50)
            
            Text("Bem-vindo!")
                .font(.title2)
            
            Text("App em desenvolvimento")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}
