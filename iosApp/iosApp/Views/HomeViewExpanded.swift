import SwiftUI
import shared

struct HomeViewExpanded: View {
    @StateObject private var viewModel = ViewModelProvider.shared.homeViewModel
    @State private var selectedTab: FeedTab = .apoio
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Hero Card
                    HeroCardView {
                        viewModel.requestHelp()
                    }
                    
                    // Feed Tabs
                    FeedTabsView(selectedTab: $selectedTab) { tab in
                        viewModel.selectTab(tab: tab)
                    }
                    
                    // Feed Posts
                    ForEach(viewModel.state.feedPosts, id: \\.id) { post in
                        PostCardView(post: post) {
                            viewModel.likePost(postId: post.id, userId: "currentUserId")
                        }
                    }
                    
                    // Air Quality
                    if let airQuality = viewModel.state.airQuality {
                        AirQualityCardView(airQuality: airQuality)
                    }
                    
                    // Quick Actions
                    QuickActionsCardView(actions: viewModel.state.quickActions) { action in
                        viewModel.executeQuickAction(action: action)
                    }
                    
                    // Community Stats
                    if let stats = viewModel.state.communityStats {
                        CommunityStatsCardView(stats: stats)
                    }
                }
                .padding(16)
            }
            .navigationTitle("Afilaxy")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct HeroCardView: View {
    let onRequestHelp: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("BEM-VINDO À COMUNIDADE")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.white.opacity(0.8))
            
            Text("Respire fundo.\\nVocê não está sozinho.")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .lineLimit(nil)
            
            Text("Conecte-se com pessoas que entendem sua jornada com a asma e encontre o suporte que você precisa agora.")
                .font(.body)
                .foregroundColor(.white.opacity(0.9))
                .lineLimit(nil)
            
            Button(action: onRequestHelp) {
                HStack {
                    Image(systemName: "location.fill")
                        .font(.system(size: 16))
                    Text("Solicitar Ajuda")
                        .fontWeight(.medium)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.white)
                .foregroundColor(Color.blue)
                .cornerRadius(8)
            }
        }
        .padding(24)
        .background(Color.blue)
        .cornerRadius(16)
    }
}

struct FeedTabsView: View {
    @Binding var selectedTab: FeedTab
    let onTabSelected: (FeedTab) -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach([FeedTab.apoio, FeedTab.recentes, FeedTab.destaques], id: \\.self) { tab in
                Button(action: {
                    selectedTab = tab
                    onTabSelected(tab)
                }) {
                    Text(tab.displayName)
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(selectedTab == tab ? Color.blue : Color.gray.opacity(0.2))
                        .foregroundColor(selectedTab == tab ? .white : .primary)
                        .cornerRadius(16)
                }
            }
            Spacer()
        }
    }
}

struct PostCardView: View {
    let post: SocialPost
    let onLike: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // User Info
            HStack(spacing: 12) {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 40, height: 40)
                    .overlay(
                        Text(String(post.userName.prefix(2)).uppercased())
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    )
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.userName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Text("HÁ 2 HORAS • DICA DE RESPIRAÇÃO")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            
            // Content
            Text(post.content)
                .font(.body)
                .lineLimit(nil)
            
            // Actions
            HStack(spacing: 24) {
                Button(action: onLike) {
                    HStack(spacing: 4) {
                        Image(systemName: post.isLiked ? "heart.fill" : "heart")
                            .foregroundColor(post.isLiked ? .red : .secondary)
                        Text("\\(post.likes) Apoios")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                HStack(spacing: 4) {
                    Image(systemName: "bubble.left")
                        .foregroundColor(.secondary)
                    Text("\\(post.comments) Respostas")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct AirQualityCardView: View {
    let airQuality: AirQuality
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("AIR QUALITY")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Circle()
                    .fill(Color.green)
                    .frame(width: 8, height: 8)
            }
            
            HStack(alignment: .bottom, spacing: 8) {
                Text("\\(airQuality.index)")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text(airQuality.status)
                    .font(.headline)
                    .fontWeight(.medium)
                    .foregroundColor(.green)
            }
            
            Text(airQuality.recommendation)
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct QuickActionsCardView: View {
    let actions: [QuickAction]
    let onActionClick: (ActionType) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Suporte Rápido")
                .font(.headline)
                .fontWeight(.bold)
            
            ForEach(actions, id: \\.id) { action in
                Button(action: {
                    onActionClick(action.action)
                }) {
                    Text(action.title)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.blue, lineWidth: 1)
                        )
                        .foregroundColor(.blue)
                }
            }
        }
        .padding(16)
        .background(Color.blue.opacity(0.1))
        .cornerRadius(12)
    }
}

struct CommunityStatsCardView: View {
    let stats: CommunityStats
    
    var body: some View {
        HStack {
            Text("COMUNIDADE ATIVA")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text("+\\(stats.onlineCount) online agora")
                .font(.subheadline)
                .fontWeight(.medium)
        }
        .padding(16)
        .background(Color.green.opacity(0.1))
        .cornerRadius(12)
    }
}

enum FeedTab: CaseIterable {
    case apoio, recentes, destaques
    
    var displayName: String {
        switch self {
        case .apoio: return "Feed de Apoio"
        case .recentes: return "Recentes"
        case .destaques: return "Destaques"
        }
    }
}

#Preview {
    HomeViewExpanded()
}