import SwiftUI
import shared

struct ContentView: View {
    @State private var isLoggedIn = false

    var body: some View {
        Group {
            if isLoggedIn {
                HomeView(onLogout: {
                    DispatchQueue.main.async { isLoggedIn = false }
                })
            } else {
                LoginView(onLoginSuccess: {
                    DispatchQueue.main.async { isLoggedIn = true }
                })
            }
        }
    }
}
