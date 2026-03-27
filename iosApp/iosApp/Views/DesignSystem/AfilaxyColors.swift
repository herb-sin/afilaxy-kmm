import SwiftUI

// MARK: - Afilaxy Design System Colors
// Baseado no Material Design 3 do Android, adaptado para iOS HIG

extension Color {
    // MARK: - Primary Colors
    static let afiPrimary = Color(hex: "#00628f")
    static let afiPrimaryContainer = Color(hex: "#007cb4")
    static let afiOnPrimary = Color.white
    static let afiOnPrimaryContainer = Color.white
    
    // MARK: - Secondary Colors
    static let afiSecondary = Color(hex: "#525f74")
    static let afiSecondaryContainer = Color(hex: "#d5e3f7")
    static let afiOnSecondary = Color.white
    static let afiOnSecondaryContainer = Color(hex: "#0f1c2d")
    
    // MARK: - Tertiary Colors
    static let afiTertiary = Color(hex: "#00657a")
    static let afiTertiaryContainer = Color(hex: "#b8eaff")
    static let afiOnTertiary = Color.white
    static let afiOnTertiaryContainer = Color(hex: "#001f26")
    
    // MARK: - Error Colors
    static let afiError = Color(hex: "#ba1a1a")
    static let afiErrorContainer = Color(hex: "#ffdad6")
    static let afiOnError = Color.white
    static let afiOnErrorContainer = Color(hex: "#410002")
    
    // MARK: - Surface Colors
    static let afiSurface = Color(hex: "#f8f9ff")
    static let afiSurfaceContainer = Color(hex: "#e5efff")
    static let afiSurfaceContainerHigh = Color(hex: "#dfe8f9")
    static let afiSurfaceContainerHighest = Color(hex: "#d9e2f3")
    static let afiOnSurface = Color(hex: "#0d1c2d")
    static let afiOnSurfaceVariant = Color(hex: "#42474e")
    
    // MARK: - Background Colors
    static let afiBackground = Color(hex: "#f8f9ff")
    static let afiOnBackground = Color(hex: "#0d1c2d")
    
    // MARK: - Outline Colors
    static let afiOutline = Color(hex: "#72777f")
    static let afiOutlineVariant = Color(hex: "#c2c7cf")
    
    // MARK: - Success Colors (Custom)
    static let afiSuccess = Color(hex: "#4caf50")
    static let afiSuccessContainer = Color(hex: "#e8f5e8")
    static let afiOnSuccess = Color.white
    static let afiOnSuccessContainer = Color(hex: "#1b5e20")
    
    // MARK: - Warning Colors (Custom)
    static let afiWarning = Color(hex: "#ff9800")
    static let afiWarningContainer = Color(hex: "#fff3e0")
    static let afiOnWarning = Color.white
    static let afiOnWarningContainer = Color(hex: "#e65100")
    
    // MARK: - Gradient Colors
    static let afiGradientStart = Color(hex: "#00628f")
    static let afiGradientEnd = Color(hex: "#007cb4")
    
    // MARK: - Status Colors
    static let afiStatusActive = Color(hex: "#4caf50")
    static let afiStatusInactive = Color(hex: "#9e9e9e")
    static let afiStatusCritical = Color(hex: "#f44336")
    static let afiStatusWarning = Color(hex: "#ff9800")
    
    // MARK: - Helper Initializer
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - Semantic Color Aliases
// Para facilitar o uso e manter consistência

extension Color {
    // MARK: - Card Colors
    static let afiCardBackground = Color.afiSurfaceContainer
    static let afiCardBorder = Color.afiOutlineVariant
    
    // MARK: - Text Colors
    static let afiTextPrimary = Color.afiOnSurface
    static let afiTextSecondary = Color.afiOnSurfaceVariant
    static let afiTextOnPrimary = Color.afiOnPrimary
    
    // MARK: - Button Colors
    static let afiButtonPrimary = Color.afiPrimary
    static let afiButtonSecondary = Color.afiSecondary
    static let afiButtonDanger = Color.afiError
    
    // MARK: - Legacy Color Aliases (for backward compatibility)
    static let afiprimary = Color.afiPrimary
    static let afionSurface = Color.afiOnSurface
}