import Foundation
import FirebaseFirestore

// Modelos de dados do Relatório de Saúde (HealthReportExportView.swift).
// Separados da view para manter cada arquivo dentro do limite do SwiftLint.

// MARK: - Report Data Models

/// Janela do relatório: 4 semanas, o período dos critérios de controle da GINA.
enum ReportWindow {
    static let days = 28
    static let weeks = 4
    static let dayMs: Int64 = 86_400_000
    static let weekMs: Int64 = 7 * dayMs
    static let possibleCheckIns = 56 // 2 check-ins por dia × 28 dias
}

struct WellbeingReportData {
    let patientName: String
    let healthProfile: HealthProfileData
    let checkIns: [WellbeingCheckIn]
    let emergencyCount: Int
    let emergencies: [EmergencyEventData]
    let actResults: [ActResultData]
    let windowStartMs: Int64
    let generatedAt: Date

    var gina: GinaControl { GinaControl(checkIns: checkIns, windowStartMs: windowStartMs) }
}

struct ActResultData {
    static let historyLimit = 6
    /// Campos na ordem das perguntas P1–P5 (ver AsthmaControlTestResult no shared).
    static let answerFields = ["activityImpairment", "shortnessOfBreath", "nighttimeSymptoms",
                               "rescueMedicationUse", "selfRatedControl"]
    static let answerLabels = ["P1 · Limitação de atividades", "P2 · Falta de ar",
                               "P3 · Despertares noturnos por asma", "P4 · Uso de medicação de alívio",
                               "P5 · Autoavaliação do controle"]

    let timestamp: Int64
    let score: Int
    let answers: [Int] // P1–P5, cada uma de 1 (pior) a 5 (melhor)
    // Perguntas complementares (fora do escore) — nil em resultados antigos
    let oralSteroidCourse4w: Bool?
    let emergencyVisit4w: Bool?

    init(doc: QueryDocumentSnapshot) {
        timestamp = (doc["timestamp"] as? NSNumber)?.int64Value ?? 0
        score = (doc["score"] as? NSNumber)?.intValue ?? 0
        answers = Self.answerFields.map { (doc[$0] as? NSNumber)?.intValue ?? 0 }
        oralSteroidCourse4w = doc["oralSteroidCourse4w"] as? Bool
        emergencyVisit4w = doc["emergencyVisit4w"] as? Bool
    }

    var hadSevereExacerbation: Bool { oralSteroidCourse4w == true || emergencyVisit4w == true }

    /// Mesmos cortes de AsthmaControlLevel (shared) — lido direto do Firestore aqui, como o
    /// restante deste relatório, em vez de atravessar a ponte Kotlin só para o rótulo.
    var levelLabel: String {
        switch score {
        case 20...: return "Bem controlada"
        case 15..<20: return "Parcialmente controlada"
        default: return "Não controlada"
        }
    }
}

struct EmergencyEventData {
    let timestamp: Int64
    let status: String
    let samuCalled: Bool
    // Contexto ambiental gravado na criação do pedido — nil em pedidos antigos ou sem rede
    let temperature: Double?
    let humidity: Double?
    let aqi: Int?
    // Resposta do diálogo pós-crise — nil se não respondida
    let emergencyVisitAfter: Bool?

    init(doc: QueryDocumentSnapshot) {
        timestamp = (doc["timestamp"] as? NSNumber)?.int64Value ?? 0
        status = doc["status"] as? String ?? ""
        samuCalled = doc["samuCalled"] as? Bool ?? false
        emergencyVisitAfter = doc["emergencyVisitAfter"] as? Bool
        temperature = (doc["envTemperature"] as? NSNumber)?.doubleValue
        humidity = (doc["envHumidity"] as? NSNumber)?.doubleValue
        aqi = (doc["envAqi"] as? NSNumber)?.intValue
    }

    var detail: String {
        var parts: [String] = []
        if let t = temperature { parts.append("\(Int(t))°C") }
        if let h = humidity { parts.append("umidade \(Int(h))%") }
        if let a = aqi { parts.append("AQI \(a)") }
        if parts.isEmpty { parts.append("sem dados ambientais") }
        if samuCalled { parts.append("SAMU acionado") }
        if emergencyVisitAfter == true { parts.append("foi ao pronto-socorro/UPA ou internado") }
        if status == "cancelled" { parts.append("cancelado") }
        return parts.joined(separator: " · ")
    }
}

struct HealthProfileData {
    let allergiesText: [String]
    let medications: [String]
    let conditions: [String]
    let notes: String
    let hasGerd: Bool
    let hasSleepApnea: Bool
    let hasRhinitis: Bool
    let hasObesity: Bool
    let hasFoodAllergy: Bool
    let hasNsaidAllergy: Bool
    let hasInhalantAllergy: Bool
    let hasWheelchair: Bool
    let hasLowVision: Bool
    let hasSpecialCondition: Bool
    let severeExacerbations12m: Int
    let severeExacerbationsUpdatedAt: Int64
    let everIcuOrIntubated: Bool
    let emergencyContactName: String
    let emergencyContactPhone: String
    let birthYear: Int // 0 = não informado

    init(doc: DocumentSnapshot?) {
        allergiesText = (doc?["healthData.allergies"] as? [String]) ?? []
        medications = (doc?["healthData.medications"] as? [String]) ?? []
        conditions = (doc?["healthData.conditions"] as? [String]) ?? []
        notes = doc?["healthData.notes"] as? String ?? ""
        hasGerd = doc?["healthData.hasGerd"] as? Bool ?? false
        hasSleepApnea = doc?["healthData.hasSleepApnea"] as? Bool ?? false
        hasRhinitis = doc?["healthData.hasRhinitis"] as? Bool ?? false
        hasObesity = doc?["healthData.hasObesity"] as? Bool ?? false
        hasFoodAllergy = doc?["healthData.hasFoodAllergy"] as? Bool ?? false
        hasNsaidAllergy = doc?["healthData.hasNsaidAllergy"] as? Bool ?? false
        hasInhalantAllergy = doc?["healthData.hasInhalantAllergy"] as? Bool ?? false
        hasWheelchair = doc?["healthData.hasWheelchair"] as? Bool ?? false
        hasLowVision = doc?["healthData.hasLowVision"] as? Bool ?? false
        hasSpecialCondition = doc?["healthData.hasSpecialCondition"] as? Bool ?? false
        severeExacerbations12m = (doc?["healthData.severeExacerbations12m"] as? NSNumber)?.intValue ?? 0
        severeExacerbationsUpdatedAt = (doc?["healthData.severeExacerbationsUpdatedAt"] as? NSNumber)?.int64Value ?? 0
        everIcuOrIntubated = doc?["healthData.everIcuOrIntubated"] as? Bool ?? false
        emergencyContactName = doc?["emergencyContact.name"] as? String ?? ""
        emergencyContactPhone = doc?["emergencyContact.phone"] as? String ?? ""
        birthYear = (doc?["healthData.birthYear"] as? NSNumber)?.intValue ?? 0
    }
}

/// Respostas de um check-in. nil = pergunta não respondida (ou inexistente na época) —
/// não é "Não": check-ins antigos não tinham algumas perguntas e não podem virar sintoma.
struct WellbeingCheckIn {
    let type: String
    let timestamp: Int64
    let rescueInhalerUses: Int?
    let nighttimeAwakening: Bool? // true = "sono tranquilo, sem interrupções"
    let morningMoodGood: Bool?
    let morningEnergyGood: Bool?
    let selfCareGood: Bool?
    let daytimeBreathingEase: Bool?
    let activityAsPlanned: Bool?

    init(doc: QueryDocumentSnapshot) {
        type = doc["type"] as? String ?? ""
        timestamp = (doc["timestamp"] as? NSNumber)?.int64Value ?? 0
        rescueInhalerUses = (doc["rescueInhalerUses"] as? NSNumber)?.intValue
        nighttimeAwakening = doc["nighttimeAwakening"] as? Bool
        morningMoodGood = doc["morningMoodGood"] as? Bool
        morningEnergyGood = doc["morningEnergyGood"] as? Bool
        selfCareGood = doc["selfCareGood"] as? Bool
        daytimeBreathingEase = doc["daytimeBreathingEase"] as? Bool
        activityAsPlanned = doc["activityAsPlanned"] as? Bool
    }

    var isMorning: Bool { type == "MORNING" }
    var isEvening: Bool { type == "EVENING" }

    /// Crítico só quando as três respostas do período foram explicitamente "Não".
    var isCritical: Bool {
        isMorning
            ? nighttimeAwakening == false && morningMoodGood == false && morningEnergyGood == false
            : selfCareGood == false && daytimeBreathingEase == false && activityAsPlanned == false
    }
}

// MARK: - Controle de sintomas — critérios da GINA (4 semanas)
// O app não diagnostica nem classifica: mostra cada critério com as contagens semanais e
// quantos estão presentes. A escala da GINA vai como referência e a interpretação fica com
// o profissional. Mesma regra do Android (HealthReportExportScreen.kt).

enum GinaStatus: String {
    case present = "Presente", absent = "Ausente", noData = "Sem dados"
}

struct GinaCriterion {
    let label: String
    /// Ocorrências por semana, S1 (mais antiga) → S4 (mais recente); nil = sem resposta na semana.
    let weekly: [Int?]
    /// Presente quando alguma semana tem mais que `threshold` ocorrências.
    let threshold: Int

    var status: GinaStatus {
        if weekly.allSatisfy({ $0 == nil }) { return .noData }
        return weekly.contains { ($0 ?? 0) > threshold } ? .present : .absent
    }

    var valueText: String {
        let weeks = weekly.map { v in v.map { String($0) } ?? "—" }.joined(separator: " · ")
        return "\(status.rawValue) — por semana: \(weeks)"
    }
}

struct GinaWeek {
    let startMs: Int64
    let morningCount: Int
    let eveningCount: Int
}

struct GinaControl {
    let weeks: [GinaWeek]
    let criteria: [GinaCriterion]

    init(checkIns: [WellbeingCheckIn], windowStartMs: Int64) {
        func weekOf(_ c: WellbeingCheckIn) -> Int {
            let w = Int((c.timestamp - windowStartMs) / ReportWindow.weekMs)
            return min(max(w, 0), ReportWindow.weeks - 1)
        }
        func weekly(_ source: [WellbeingCheckIn], _ value: (WellbeingCheckIn) -> Int?) -> [Int?] {
            var sums = [Int?](repeating: nil, count: ReportWindow.weeks)
            for c in source {
                guard let v = value(c) else { continue }
                let w = weekOf(c)
                sums[w] = (sums[w] ?? 0) + v
            }
            return sums
        }
        func occurred(_ answer: Bool?, when occursWhen: Bool) -> Int? {
            answer.map { $0 == occursWhen ? 1 : 0 }
        }

        let morning = checkIns.filter { $0.isMorning }
        let evening = checkIns.filter { $0.isEvening }

        weeks = (0..<ReportWindow.weeks).map { w in
            GinaWeek(
                startMs: windowStartMs + Int64(w) * ReportWindow.weekMs,
                morningCount: morning.filter { weekOf($0) == w }.count,
                eveningCount: evening.filter { weekOf($0) == w }.count
            )
        }
        criteria = [
            GinaCriterion(label: "Sintomas diurnos >2x/sem.",
                          weekly: weekly(evening) { occurred($0.daytimeBreathingEase, when: false) }, threshold: 2),
            GinaCriterion(label: "Despertar noturno",
                          weekly: weekly(morning) { occurred($0.nighttimeAwakening, when: false) }, threshold: 0),
            GinaCriterion(label: "Resgate >2x/sem.",
                          weekly: weekly(evening) { $0.rescueInhalerUses }, threshold: 2),
            GinaCriterion(label: "Limitação de atividades",
                          weekly: weekly(evening) { occurred($0.activityAsPlanned, when: false) }, threshold: 0)
        ]
    }

    var presentCount: Int { criteria.filter { $0.status == .present }.count }
    var noDataCount: Int { criteria.filter { $0.status == .noData }.count }
}
