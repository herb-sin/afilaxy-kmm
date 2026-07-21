import Foundation

class FileLogger {
    static let shared = FileLogger()

    private let fileManager = FileManager.default
    private let queue = DispatchQueue(label: "com.afilaxy.filelogger", qos: .utility)
    private var logFileURL: URL?
    private let maxFileSize: Int64 = 5 * 1024 * 1024
    private let maxLogAge: TimeInterval = 7 * 24 * 60 * 60

    private let dateFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private init() {
        setupLogFile()
        cleanOldLogs()
    }

    private func setupLogFile() {
        guard let cacheDir = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else { return }

        let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
        try? fileManager.createDirectory(at: logsDir, withIntermediateDirectories: true)

        let dateString = DateFormatter.logFileDate.string(from: Date())
        logFileURL = logsDir.appendingPathComponent("afilaxy_\(dateString).log")

        if let url = logFileURL, !fileManager.fileExists(atPath: url.path) {
            fileManager.createFile(atPath: url.path, contents: nil)
            try? "=== Afilaxy iOS Logs - \(dateString) ===\n\n".write(to: url, atomically: true, encoding: .utf8)
        }
    }

    func write(level: String, tag: String, message: String) {
        queue.async { [weak self] in
            guard let self, let url = self.logFileURL else { return }

            if let size = try? self.fileManager.attributesOfItem(atPath: url.path)[.size] as? Int64,
               size > self.maxFileSize {
                self.rotateLogFile()
            }

            let line = "[\(self.dateFormatter.string(from: Date()))] [\(level)] [\(tag)] \(message)\n"
            if let data = line.data(using: .utf8),
               let handle = try? FileHandle(forWritingTo: url) {
                handle.seekToEndOfFile()
                handle.write(data)
                try? handle.close()
            }
        }
    }

    private func rotateLogFile() {
        guard let url = logFileURL else { return }
        let rotated = url.deletingLastPathComponent()
            .appendingPathComponent("afilaxy_\(Int(Date().timeIntervalSince1970)).log")
        try? fileManager.moveItem(at: url, to: rotated)
        fileManager.createFile(atPath: url.path, contents: nil)
    }

    private func cleanOldLogs() {
        queue.async { [weak self] in
            guard let self else { return }
            let cutoff = Date().addingTimeInterval(-self.maxLogAge)
            self.getAllLogFileURLs().forEach { url in
                if let date = (try? self.fileManager.attributesOfItem(atPath: url.path))?[.creationDate] as? Date,
                   date < cutoff {
                    try? self.fileManager.removeItem(at: url)
                }
            }
        }
    }

    func getAllLogFileURLs() -> [URL] {
        guard let cacheDir = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else { return [] }
        let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
        let files = (try? fileManager.contentsOfDirectory(at: logsDir, includingPropertiesForKeys: [.creationDateKey], options: .skipsHiddenFiles)) ?? []
        return files.filter { $0.pathExtension == "log" }.sorted {
            let d1 = (try? $0.resourceValues(forKeys: [.creationDateKey]))?.creationDate ?? .distantPast
            let d2 = (try? $1.resourceValues(forKeys: [.creationDateKey]))?.creationDate ?? .distantPast
            return d1 > d2
        }
    }

    func getTotalLogSize() -> Int64 {
        getAllLogFileURLs().reduce(0) { total, url in
            total + ((try? fileManager.attributesOfItem(atPath: url.path)[.size] as? Int64) ?? 0)
        }
    }

    func clearAllLogs() {
        queue.async { [weak self] in
            guard let self,
                  let cacheDir = self.fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else { return }
            try? self.fileManager.removeItem(at: cacheDir.appendingPathComponent("logs"))
            self.setupLogFile()
        }
    }
}

extension DateFormatter {
    static let logFileDate: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
