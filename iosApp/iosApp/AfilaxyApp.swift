import SwiftUI
import shared

@main
struct AfilaxyApp: App {
    
    init() {
        KoinHelperKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
