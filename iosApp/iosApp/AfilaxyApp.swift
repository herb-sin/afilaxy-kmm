import SwiftUI
import shared
import FirebaseCore

@main
struct AfilaxyApp: App {
    
    init() {
        FirebaseApp.configure()
        KoinHelperKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
