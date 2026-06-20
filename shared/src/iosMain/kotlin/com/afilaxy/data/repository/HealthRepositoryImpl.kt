package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.HealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.HealthKit.HKAuthorizationStatusSharingAuthorized
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleep
import platform.HealthKit.HKCategoryValueSleepAnalysisAwake
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierHeartRate
import platform.HealthKit.HKQuantityTypeIdentifierOxygenSaturation
import platform.HealthKit.HKQuantityTypeIdentifierRespiratoryRate
import platform.HealthKit.HKQuery
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKStatisticsOptionDiscreteAverage
import platform.HealthKit.HKStatisticsOptionDiscreteMin
import platform.HealthKit.HKStatisticsQuery
import platform.HealthKit.HKUnit
import kotlin.coroutines.resume

class IosHealthRepository : HealthRepository {

    private val store = HKHealthStore()

    private val typesToRead by lazy {
        setOfNotNull(
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate),
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierOxygenSaturation),
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierRespiratoryRate),
            HKObjectType.categoryTypeForIdentifier(HKCategoryTypeIdentifierSleepAnalysis)
        )
    }

    override fun isAvailable(): Boolean = HKHealthStore.isHealthDataAvailable()

    override suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        val hrType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)
            ?: return false
        return store.authorizationStatusForType(hrType) == HKAuthorizationStatusSharingAuthorized
    }

    // HealthKit: solicita autorização — deve correr na main thread
    override suspend fun requestPermissions(): Boolean {
        if (!isAvailable()) return false
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                store.requestAuthorizationToShareTypes(
                    typesToShare = null,
                    readTypes = typesToRead
                ) { success, _ -> if (cont.isActive) cont.resume(success) }
            }
        }
    }

    override suspend fun getSnapshot(isEvening: Boolean): HealthSnapshot? {
        if (!isAvailable() || !hasPermissions()) return null

        return try {
            val now = NSDate()
            val last24h = NSDate.dateWithTimeIntervalSinceNow(-86_400.0)
            val sleepWindowStart = if (isEvening) last24h else NSDate.dateWithTimeIntervalSinceNow(-57_600.0)

            // ── Frequência cardíaca (média 24h) ──────────────────────────────
            val avgHR = readQuantityStat(
                typeId = HKQuantityTypeIdentifierHeartRate,
                unit = HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit()),
                start = last24h,
                end = now,
                useMin = false
            )?.toInt()

            // ── SpO₂ mínimo (janela de sono) ─────────────────────────────────
            // HealthKit armazena SpO₂ como fração (0-1) → multiplicar por 100
            val minSpo2Raw = readQuantityStat(
                typeId = HKQuantityTypeIdentifierOxygenSaturation,
                unit = HKUnit.percentUnit(),
                start = sleepWindowStart,
                end = now,
                useMin = true
            )
            // HKUnit.percentUnit() retorna valores 0-1 no HealthKit
            val minSpo2 = minSpo2Raw?.let { (it * 100f).toFloat() }

            // ── Frequência respiratória ───────────────────────────────────────
            val avgResp = readQuantityStat(
                typeId = HKQuantityTypeIdentifierRespiratoryRate,
                unit = HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit()),
                start = last24h,
                end = now,
                useMin = false
            )?.toFloat()

            // ── Sono ─────────────────────────────────────────────────────────
            val (sleepDuration, awakenings) = readSleep(sleepWindowStart, now)

            HealthSnapshot(
                avgHeartRateBpm = avgHR,
                sleepDurationHours = sleepDuration,
                sleepInterruptions = awakenings,
                minSpo2Percent = minSpo2,
                avgRespiratoryRate = avgResp
            )
        } catch (e: Exception) { null }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun readQuantityStat(
        typeId: String,
        unit: HKUnit,
        start: NSDate,
        end: NSDate,
        useMin: Boolean
    ): Double? = suspendCancellableCoroutine { cont ->
        val quantityType = HKObjectType.quantityTypeForIdentifier(typeId) as? HKQuantityType
            ?: run { cont.resume(null); return@suspendCancellableCoroutine }
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = start, endDate = end, options = 0u
        )
        val options = if (useMin) HKStatisticsOptionDiscreteMin else HKStatisticsOptionDiscreteAverage
        val query = HKStatisticsQuery(
            quantityType = quantityType,
            quantitySamplePredicate = predicate,
            options = options
        ) { _, stats, _ ->
            val value = if (useMin)
                stats?.minimumQuantity()?.doubleValueForUnit(unit)
            else
                stats?.averageQuantity()?.doubleValueForUnit(unit)
            if (cont.isActive) cont.resume(value)
        }
        store.executeQuery(query)
    }

    private suspend fun readSleep(
        start: NSDate,
        end: NSDate
    ): Pair<Float?, Int> = suspendCancellableCoroutine { cont ->
        val sleepType = HKObjectType.categoryTypeForIdentifier(HKCategoryTypeIdentifierSleepAnalysis)
            ?: run { cont.resume(Pair(null, 0)); return@suspendCancellableCoroutine }
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = start, endDate = end, options = 0u
        )
        val query = HKSampleQuery(
            sampleType = sleepType,
            predicate = predicate,
            limit = HKObjectQueryNoLimit,
            sortDescriptors = null
        ) { _, samples, _ ->
            @Suppress("UNCHECKED_CAST")
            val categorySamples = samples as? List<platform.HealthKit.HKCategorySample>
                ?: emptyList()

            // Duração total de sono (estágios != awake)
            val asleepSecs = categorySamples
                .filter { it.value.toInt() != HKCategoryValueSleepAnalysisAwake.toInt() }
                .sumOf { it.endDate.timeIntervalSinceDate(it.startDate) }

            // Despertares
            val awakeCount = categorySamples.count {
                it.value.toInt() == HKCategoryValueSleepAnalysisAwake.toInt()
            }

            val duration = if (asleepSecs > 0) (asleepSecs / 3600.0).toFloat() else null
            if (cont.isActive) cont.resume(Pair(duration, awakeCount))
        }
        store.executeQuery(query)
    }
}
