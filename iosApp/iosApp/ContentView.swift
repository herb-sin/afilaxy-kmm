import SwiftUI
import shared

struct ContentView: View {
    @State private var isLoggedIn = false
    
    var body: some View {
        if isLoggedIn {
            HomeView(onLogout: {
                isLoggedIn = false
            })
        } else {
            LoginView(onLoginSuccess: {
                isLoggedIn = true
            })
        }
    }
}
