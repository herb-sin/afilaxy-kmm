import SwiftUI
import shared
import Combine

// MARK: - StateFlowObserver Simplificado
// Observa StateFlow do KMM e converte para ObservableObject do SwiftUI
class StateFlowObserver<T>: ObservableObject {
    @Published var value: T
    
    init(_ stateFlow: any Kotlinx_coroutines_coreStateFlow) {
        // Valor inicial
        self.value = stateFlow.value as! T
        
        // Para simplificar, vamos apenas usar o valor inicial
        // Em uma implementação completa, seria necessário observar mudanças
        // usando Kotlin Coroutines Flow collection
    }
}