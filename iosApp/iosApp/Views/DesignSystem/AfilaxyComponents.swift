import SwiftUI

// MARK: - Afilaxy Design System Components
// Componentes reutilizáveis baseados no Material Design 3

// MARK: - Cards

struct AfilaxyCard<Content: View>: View {
    let content: Content
    let backgroundColor: Color
    let cornerRadius: CGFloat
    let shadowRadius: CGFloat
    
    init(
        backgroundColor: Color = .afiCardBackground,
        cornerRadius: CGFloat = 16,
        shadowRadius: CGFloat = 2,
        @ViewBuilder content: () -> Content
    ) {
        self.content = content()
        self.backgroundColor = backgroundColor
        self.cornerRadius = cornerRadius
        self.shadowRadius = shadowRadius
    }
    
    var body: some View {
        content
            .padding()
            .background(backgroundColor)
            .cornerRadius(cornerRadius)
            .shadow(color: .black.opacity(0.1), radius: shadowRadius, x: 0, y: 1)
    }
}

struct HeroGradientCard<Content: View>: View {
    let content: Content
    let cornerRadius: CGFloat
    
    init(
        cornerRadius: CGFloat = 16,
        @ViewBuilder content: () -> Content
    ) {
        self.content = content()
        self.cornerRadius = cornerRadius
    }
    
    var body: some View {
        content
            .padding(20)
            .background(
                LinearGradient(
                    colors: [.afiGradientStart, .afiGradientEnd],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .cornerRadius(cornerRadius)
            .shadow(color: .afiPrimary.opacity(0.3), radius: 8, x: 0, y: 4)
    }
}

// MARK: - Badges

struct StatusBadge: View {
    let text: String
    let status: BadgeStatus
    let style: BadgeStyle
    
    enum BadgeStatus {
        case success, warning, error, info, active, inactive
        
        var color: Color {
            switch self {
            case .success: return .afiSuccess
            case .warning: return .afiWarning
            case .error: return .afiError
            case .info: return .afiPrimary
            case .active: return .afiStatusActive
            case .inactive: return .afiStatusInactive
            }
        }
        
        var backgroundColor: Color {
            switch self {
            case .success: return .afiSuccessContainer
            case .warning: return .afiWarningContainer
            case .error: return .afiErrorContainer
            case .info: return .afiPrimaryContainer
            case .active: return .afiSuccessContainer
            case .inactive: return .afiSurfaceContainer
            }
        }
    }
    
    enum BadgeStyle {
        case filled, outlined
    }
    
    init(text: String, status: BadgeStatus, style: BadgeStyle = .filled) {
        self.text = text
        self.status = status
        self.style = style
    }
    
    // Convenience initializer for backward compatibility
    init(text: String, style: BadgeStatus) {
        self.text = text
        self.status = style
        self.style = .filled
    }
    
    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .foregroundColor(style == .filled ? .white : status.color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                style == .filled ? status.color : status.backgroundColor
            )
            .overlay(
                style == .outlined ?
                RoundedRectangle(cornerRadius: 20)
                    .stroke(status.color, lineWidth: 1) : nil
            )
            .cornerRadius(20)
    }
}

// MARK: - Metric Cards

struct MetricCard: View {
    let title: String
    let value: String
    let subtitle: String?
    let icon: String
    let accentColor: Color
    let borderPosition: BorderPosition
    
    enum BorderPosition {
        case leading, top, none
    }
    
    init(
        title: String,
        value: String,
        subtitle: String? = nil,
        icon: String,
        accentColor: Color = .afiPrimary,
        borderPosition: BorderPosition = .none
    ) {
        self.title = title
        self.value = value
        self.subtitle = subtitle
        self.icon = icon
        self.accentColor = accentColor
        self.borderPosition = borderPosition
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Leading border if needed
            if borderPosition == .leading {
                Rectangle()
                    .fill(accentColor)
                    .frame(width: 4)
            }
            
            // Icon
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(accentColor)
                .frame(width: 32, height: 32)
            
            // Content
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.afiTextSecondary)
                
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.afiTextPrimary)
                
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundColor(.afiTextSecondary)
                }
            }
            
            Spacer()
        }
        .padding()
        .background(Color.afiCardBackground)
        .overlay(
            // Top border if needed
            borderPosition == .top ?
            Rectangle()
                .fill(accentColor)
                .frame(height: 4)
                .offset(y: -2) : nil,
            alignment: .top
        )
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

// MARK: - Action Cards

struct ActionCard: View {
    let title: String
    let subtitle: String?
    let icon: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.afiPrimary)
                
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.afiTextPrimary)
                
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.afiTextSecondary)
                        .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.afiCardBackground)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Emergency Button

struct EmergencyButton: View {
    let title: String
    let isActive: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: isActive ? "exclamationmark.triangle.fill" : "cross.circle.fill")
                    .font(.title2)
                    .foregroundColor(.white)
                
                Text(title)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(isActive ? Color.afiStatusCritical : Color.afiError)
            .cornerRadius(16)
            .shadow(color: .afiError.opacity(0.3), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isActive ? 1.05 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isActive)
    }
}

// MARK: - Toggle Card

struct ToggleCard: View {
    let title: String
    let subtitle: String
    let icon: String
    @Binding var isOn: Bool
    let onToggle: (Bool) -> Void
    
    var body: some View {
        AfilaxyCard {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(isOn ? .afiSuccess : .afiTextSecondary)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.afiTextPrimary)
                    
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundColor(.afiTextSecondary)
                }
                
                Spacer()
                
                Toggle("", isOn: $isOn)
                    .toggleStyle(SwitchToggleStyle(tint: .afiSuccess))
                    .onChange(of: isOn) { newValue in
                        onToggle(newValue)
                    }
            }
        }
    }
}

// MARK: - Info Grid Item

struct InfoGridItem: View {
    let title: String
    let value: String
    let icon: String
    let accentColor: Color
    
    init(
        title: String,
        value: String,
        icon: String,
        accentColor: Color = .afiPrimary
    ) {
        self.title = title
        self.value = value
        self.icon = icon
        self.accentColor = accentColor
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.afiTextSecondary)
                
                Spacer()
                
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundColor(accentColor)
            }
            
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.afiTextPrimary)
                .lineLimit(2)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.afiCardBackground)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

// MARK: - Filter Chip

struct FilterChip: View {
    let title: String
    let icon: String?
    let isSelected: Bool
    let action: () -> Void
    
    init(
        title: String,
        icon: String? = nil,
        isSelected: Bool,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.isSelected = isSelected
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.caption)
                        .foregroundColor(isSelected ? .white : .afiPrimary)
                }
                
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(isSelected ? .white : .afiPrimary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                isSelected ? Color.afiPrimary : Color.afiPrimary.opacity(0.1)
            )
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.afiPrimary, lineWidth: isSelected ? 0 : 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Loading States

struct LoadingCard: View {
    let message: String
    
    init(message: String = "Carregando...") {
        self.message = message
    }
    
    var body: some View {
        AfilaxyCard {
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.2)
                    .tint(.afiPrimary)
                
                Text(message)
                    .font(.subheadline)
                    .foregroundColor(.afiTextSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
        }
    }
}

struct ErrorCard: View {
    let message: String
    let title: String
    let retryAction: (() -> Void)?
    
    init(message: String, title: String = "Erro", retryAction: (() -> Void)? = nil) {
        self.message = message
        self.title = title
        self.retryAction = retryAction
    }
    
    var body: some View {
        AfilaxyCard(backgroundColor: .afiErrorContainer) {
            VStack(spacing: 12) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.title2)
                    .foregroundColor(.afiError)
                
                Text(title)
                    .font(.headline)
                    .foregroundColor(.afiOnErrorContainer)
                
                Text(message)
                    .font(.subheadline)
                    .foregroundColor(.afiOnErrorContainer)
                    .multilineTextAlignment(.center)
                
                if let retryAction = retryAction {
                    Button("Tentar Novamente", action: retryAction)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.afiError)
                        .padding(.top, 8)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }
}