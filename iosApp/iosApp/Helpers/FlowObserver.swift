import Foundation
import shared
import Combine

class FlowObserver<T: AnyObject>: ObservableObject {
    @Published var value: T?
    
    private var cancellable: Cancellable?
    
    init(flow: Flow) {
        cancellable = flow.watch { [weak self] value in
            self?.value = value as? T
        }
    }
    
    deinit {
        cancellable?.cancel()
    }
}

extension Flow {
    func watch(onValue: @escaping (Any?) -> Void) -> Cancellable {
        let collector = FlowCollector(onValue: onValue)
        let job = self.collect(collector: collector) { error in
            print("Flow error: \(error)")
        }
        return FlowCancellable(job: job)
    }
}

class FlowCollector: Kotlinx_coroutines_coreFlowCollector {
    let onValue: (Any?) -> Void
    
    init(onValue: @escaping (Any?) -> Void) {
        self.onValue = onValue
    }
    
    func emit(value: Any?) async throws {
        DispatchQueue.main.async {
            self.onValue(value)
        }
    }
}

class FlowCancellable: Cancellable {
    let job: Kotlinx_coroutines_coreJob
    
    init(job: Kotlinx_coroutines_coreJob) {
        self.job = job
    }
    
    func cancel() {
        job.cancel(cause: nil)
    }
}
