import Foundation
import os.log

/// Logger que salva logs em arquivo local no Cache directory
class FileLogger {
    static let shared = FileLogger()
    
    private let fileManager = FileManager.default
    private let queue = DispatchQueue(label: "com.afilaxy.filelogger", qos: .utility)
    private var logFileURL: URL?
    private let maxFileSize: Int64 = 5 * 1024 * 1024 // 5MB
    private let maxLogAge: TimeInterval = 7 * 24 * 60 * 60 // 7 dias
    
    private let dateFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()
    
    private init() {
        setupLogFile()
        cleanOldLogs()
        interceptNSLog()
    }
    
    private func setupLogFile() {
        guard let cacheDir = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            os_log("Failed to get cache directory", type: .error)
            return
        }
        
        let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
        
        // Criar diretório de logs se não existir
        if !fileManager.fileExists(atPath: logsDir.path) {
            try? fileManager.createDirectory(at: logsDir, withIntermediateDirectories: true)
        }
        
        // Arquivo de log com data atual
        let dateString = DateFormatter.logFileDate.string(from: Date())
        logFileURL = logsDir.appendingPathComponent("afilaxy_\(dateString).log")
        
        // Criar arquivo se não existir
        if let url = logFileURL, !fileManager.fileExists(atPath: url.path) {
            fileManager.createFile(atPath: url.path, contents: nil)
            let header = "=== Afilaxy iOS Logs - \(dateString) ===\n\n"
            try? header.write(to: url, atomically: true, encoding: .utf8)
        }
    }
    
    /// Intercepta NSLog para salvar em arquivo
    private func interceptNSLog() {
        // Redirecionar stderr para capturar NSLog
        // Nota: Isso é simplificado, em produção use OSLog subsystem
    }
    
    /// Escreve log no arquivo (chamado do Swift)
    func write(level: String, tag: String, message: String) {
        queue.async { [weak self] in
            guard let self = self, let url = self.logFileURL else { return }
            
            let timestamp = self.dateFormatter.string(from: Date())
            let logLine = "[\(timestamp)] [\(level)] [\(tag)] \(message)\n"
            
            // Verificar tamanho do arquivo antes de escrever
            if let attributes = try? self.fileManager.attributesOfItem(atPath: url.path),
               let fileSize = attributes[.size] as? Int64,
               fileSize > self.maxFileSize {
                self.rotateLogFile()
            }
            
            // Append ao arquivo
            if let data = logLine.data(using: .utf8),
               let fileHandle = try? FileHandle(forWritingTo: url) {
                fileHandle.seekToEndOfFile()
                fileHandle.write(data)
                try? fileHandle.close()
            }
            
            // Também logar no OSLog para debug no Xcode
            os_log("%{public}@", type: self.osLogType(for: level), logLine)
        }
    }
    
    /// Rotaciona arquivo de log quando atinge tamanho máximo
    private func rotateLogFile() {
        guard let currentURL = logFileURL else { return }
        
        let timestamp = Int(Date().timeIntervalSince1970)
        let rotatedURL = currentURL.deletingPathExtension()
            .appendingPathExtension("\(timestamp).log")
        
        try? fileManager.moveItem(at: currentURL, to: rotatedURL)
        
        // Criar novo arquivo
        fileManager.createFile(atPath: currentURL.path, contents: nil)
        let header = "=== Afilaxy iOS Logs - Rotated ===\n\n"
        try? header.write(to: currentURL, atomically: true, encoding: .utf8)
    }
    
    /// Remove logs mais antigos que maxLogAge
    private func cleanOldLogs() {
        queue.async { [weak self] in
            guard let self = self,
                  let cacheDir = self.fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else {
                return
            }
            
            let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
            
            guard let files = try? self.fileManager.contentsOfDirectory(
                at: logsDir,
                includingPropertiesForKeys: [.creationDateKey],
                options: .skipsHiddenFiles
            ) else {
                return
            }
            
            let cutoffDate = Date().addingTimeInterval(-self.maxLogAge)
            
            for file in files {
                if let attributes = try? self.fileManager.attributesOfItem(atPath: file.path),
                   let creationDate = attributes[.creationDate] as? Date,
                   creationDate < cutoffDate {
                    try? self.fileManager.removeItem(at: file)
                }
            }
        }
    }
    
    /// Retorna URL do arquivo de log atual
    func getLogFileURL() -> URL? {
        return logFileURL
    }
    
    /// Retorna todas as URLs de logs
    func getAllLogFileURLs() -> [URL] {
        guard let cacheDir = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            return []
        }
        
        let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
        
        guard let files = try? fileManager.contentsOfDirectory(
            at: logsDir,
            includingPropertiesForKeys: [.creationDateKey],
            options: .skipsHiddenFiles
        ) else {
            return []
        }
        
        return files.sorted { url1, url2 in
            let date1 = (try? url1.resourceValues(forKeys: [.creationDateKey]))?.creationDate ?? Date.distantPast
            let date2 = (try? url2.resourceValues(forKeys: [.creationDateKey]))?.creationDate ?? Date.distantPast
            return date1 > date2
        }
    }
    
    /// Retorna tamanho total dos logs em bytes
    func getTotalLogSize() -> Int64 {
        let urls = getAllLogFileURLs()
        var totalSize: Int64 = 0
        
        for url in urls {
            if let attributes = try? fileManager.attributesOfItem(atPath: url.path),
               let fileSize = attributes[.size] as? Int64 {
                totalSize += fileSize
            }
        }
        
        return totalSize
    }
    
    /// Limpa todos os logs
    func clearAllLogs() {
        queue.async { [weak self] in
            guard let self = self,
                  let cacheDir = self.fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else {
                return
            }
            
            let logsDir = cacheDir.appendingPathComponent("logs", isDirectory: true)
            try? self.fileManager.removeItem(at: logsDir)
            
            // Recriar estrutura
            self.setupLogFile()
        }
    }
    
    private func osLogType(for level: String) -> OSLogType {
        switch level {
        case "DEBUG": return .debug
        case "INFO": return .info
        case "WARN": return .default
        case "ERROR": return .error
        default: return .default
        }
    }
}

// MARK: - DateFormatter Extension
extension DateFormatter {
    static let logFileDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}
