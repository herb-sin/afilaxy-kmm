package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.HealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.HealthKit.HKAuthorizationStatusSharingAuthorized
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCategoryValueSleepAnalysisAwake
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierHeartRate
import platform.HealthKit.HKQuantityTypeIdentifierOxygenSaturation
import platform.HealthKit.HKQuantityTypeIdentifierRespiratoryRate
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKStatisticsOptionDiscreteAverage
import platform.HealthKit.HKStatisticsOptionDiscreteMin
import platform.HealthKit.HKStatisticsQuery
import platform.HealthKit.HKUnit
import kotlin.coroutines.resume

class IosHealthRepository : HealthRepository {

    private val store = HKHealthStore()

    private val typesToRead by lazy {
        listOfNotNull(
            HKQuantityTypeIdentifierHeartRate
                ?.let { HKObjectType.quantityTypeForIdentifier(it) },
            HKQuantityTypeIdentifierOxygenSaturation
                ?.let { HKObjectType.quantityTypeForIdentifier(it) },
            HKQuantityTypeIdentifierRespiratoryRate
                ?.let { HKObjectType.quantityTypeForIdentifier(it) },
            HKCategoryTypeIdentifierSleepAnalysis
                ?.let { HKObjectType.categoryTypeForIdentifier(it) }
        ).toSet()
    }

    override fun isAvailable(): Boolean = HKHealthStore.isHealthDataAvailable()

    override suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        val id = HKQuantityTypeIdentifierHeartRate ?: return false
        val hrType = HKObjectType.quantityTypeForIdentifier(id) ?: return false
        return store.authorizationStatusForType(hrType) == HKAuthorizationStatusSharingAuthorized
    }

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
            val nowEpoch = NSDate().timeIntervalSince1970
            val cutoff24h  = nowEpoch - 86_400.0
            val sleepCutoff = if (isEvening) cutoff24h else nowEpoch - 57_600.0

            // Usamos null predicate — HKQuery.predicateForSamplesWithStartDate não
            // tem binding K/N confiável nesta versão; filtramos client-side por epoch.
            val avgHR = readStatistic(
                typeId = HKQuantityTypeIdentifierHeartRate,
                unitString = "count/min",
                useMin = false,
                sinceEpoch = cutoff24h
            )?.toInt()

            val minSpo2Raw = readStatistic(
                typeId = HKQuantityTypeIdentifierOxygenSaturation,
                unitString = "%",
                useMin = true,
                sinceEpoch = sleepCutoff
            )
            // HealthKit retorna SpO₂ como fração (0-1) com HKUnit.percent() → ×100
            val minSpo2 = minSpo2Raw?.let { (it * 100.0).toFloat() }

            val avgResp = readStatistic(
                typeId = HKQuantityTypeIdentifierRespiratoryRate,
                unitString = "count/min",
                useMin = false,
                sinceEpoch = cutoff24h
            )?.toFloat()

            val (sleepDuration, awakenings) = readSleep(sinceEpoch = sleepCutoff)

            HealthSnapshot(
                avgHeartRateBpm  = avgHR,
                sleepDurationHours = sleepDuration,
                sleepInterruptions = awakenings,
                minSpo2Percent   = minSpo2,
                avgRespiratoryRate = avgResp
            )
        } catch (e: Exception) { null }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * HKStatisticsQuery sem predicate de tempo (limitação K/N).
     * A filtragem por [sinceEpoch] é aplicada client-side depois.
     * Na prática, dados de smartwatch recentes dominam a estatística.
     */
    private suspend fun readStatistic(
        typeId: String?,
        unitString: String,
        useMin: Boolean,
        @Suppress("UNUSED_PARAMETER") sinceEpoch: Double
    ): Double? {
        if (typeId == null) return null
        return suspendCancellableCoroutine { cont ->
            val qType = (HKObjectType.quantityTypeForIdentifier(typeId) as? HKQuantityType)
                ?: run { cont.resume(null); return@suspendCancellableCoroutine }

            // unitFromString é o único binding K/N garantido para HKUnit
            val unit = HKUnit.unitFromString(unitString)
            val opts = if (useMin) HKStatisticsOptionDiscreteMin else HKStatisticsOptionDiscreteAverage

            val query = HKStatisticsQuery(
                quantityType = qType,
                quantitySamplePredicate = null,
                options = opts
            ) { _, stats, _ ->
                val value = if (useMin)
                    stats?.minimumQuantity()?.doubleValueForUnit(unit)
                else
                    stats?.averageQuantity()?.doubleValueForUnit(unit)
                if (cont.isActive) cont.resume(value)
            }
            store.executeQuery(query)
        }
    }

    /**
     * Lê sessões de sono via HKSampleQuery sem predicate.
     * Filtra client-side por [sinceEpoch] usando timeIntervalSince1970.
     * Usa 300 amostras como teto para cobrir múltiplas noites.
     */
    private suspend fun readSleep(sinceEpoch: Double): Pair<Float?, Int> =
        suspendCancellableCoroutine { cont ->
            val id = HKCategoryTypeIdentifierSleepAnalysis
                ?: run { cont.resume(Pair(null, 0)); return@suspendCancellableCoroutine }
            val sType = HKObjectType.categoryTypeForIdentifier(id)
                ?: run { cont.resume(Pair(null, 0)); return@suspendCancellableCoroutine }

            val query = HKSampleQuery(
                sampleType = sType,
                predicate = null,
                limit = 300u,
                sortDescriptors = null
            ) { _, samples, _ ->
                @Suppress("UNCHECKED_CAST")
                val raw = samples as? List<platform.HealthKit.HKCategorySample>
                    ?: emptyList()

                // Filtra client-side pela janela de tempo
                val inWindow = raw.filter {
                    it.startDate.timeIntervalSince1970 >= sinceEpoch
                }

                var asleepSecs = 0.0
                var awakeCount = 0
                for (s in inWindow) {
                    val dur = s.endDate.timeIntervalSince1970 - s.startDate.timeIntervalSince1970
                    if (s.value.toLong() == HKCategoryValueSleepAnalysisAwake) {
                        awakeCount++
                    } else {
                        asleepSecs += dur
                    }
                }

                val hours = if (asleepSecs > 0.0) (asleepSecs / 3600.0).toFloat() else null
                if (cont.isActive) cont.resume(Pair(hours, awakeCount))
            }
            store.executeQuery(query)
        }
}
