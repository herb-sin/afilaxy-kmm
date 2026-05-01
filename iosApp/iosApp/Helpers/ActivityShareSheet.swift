import SwiftUI

/// UIActivityViewController wrapper para compartilhar arquivos via share sheet do iOS.
struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    var onDismiss: (() -> Void)? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        controller.completionWithItemsHandler = { _, _, _, _ in onDismiss?() }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
