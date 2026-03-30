import SwiftUI
import shared
import Combine

// MARK: - StateFlowObserver
//
// Implementação com Timer polling a cada 100ms.
// O ideal seria usar Swift concurrency (async for-await) para coletar o Flow
// Kotlin diretamente, mas isso requer coroutines 1.8+ e bindings que não existem
// nesta versão (1.7.3). O polling é confiável, simples e thread-safe.
//
// Otimização: só dispara @Published quando a referência do objeto Kotlin muda
// (StateFlow Kotlin re-usa a mesma instância enquanto não há nova emissão),
// evitando re-renders desnecessários do SwiftUI.

class StateFlowObserver<T: AnyObject>: ObservableObject {
    @Published var value: T?
    private let flow: any Kotlinx_coroutines_coreStateFlow
    private var timer: AnyCancellable?

    init(_ flow: any Kotlinx_coroutines_coreStateFlow) {
        self.flow = flow
        self.value = flow.value as? T

        timer = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                let newValue = self.flow.value as? T
                // Só atualiza se a referência mudou — evita re-render em cada tick
                if newValue !== self.value {
                    self.value = newValue
                }
            }
    }

    deinit { timer?.cancel() }
}