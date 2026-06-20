package com.afilaxy.data.repository

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.HealthRepository
import java.time.Instant

class AndroidHealthRepository(private val context: Context) : HealthRepository {

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class)
        )
    }

    private val client: HealthConnectClient? by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@lazy null
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) { null }
    }

    override fun isAvailable(): Boolean = client != null

    override suspend fun hasPermissions(): Boolean {
        val c = client ?: return false
        return try {
            c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        } catch (e: Exception) { false }
    }

    // Android: permissão deve ser solicitada pelo ActivityResultLauncher no CheckInScreen.
    override suspend fun requestPermissions(): Boolean = false

    override suspend fun getSnapshot(isEvening: Boolean): HealthSnapshot? {
        val c = client ?: return null
        if (!hasPermissions()) return null

        return try {
            val now = Instant.now()
            val last24h = now.minusSeconds(86_400)
            // Para check-in matinal lemos as últimas 16h (noite anterior)
            val sleepWindowStart = if (isEvening) last24h else now.minusSeconds(57_600)

            // ── Frequência cardíaca ──────────────────────────────────────────
            val hrRecords = c.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(last24h, now))
            ).records
            val allSamples = hrRecords.flatMap { it.samples }
            val avgHR = if (allSamples.isNotEmpty())
                allSamples.map { it.beatsPerMinute }.average().toInt() else null

            // ── Sono ─────────────────────────────────────────────────────────
            val sleepRecords = c.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(sleepWindowStart, now))
            ).records
            val lastSession = sleepRecords.maxByOrNull { it.startTime }
            val sleepDuration = lastSession?.let {
                val secs = it.endTime.epochSecond - it.startTime.epochSecond
                secs / 3600f
            }
            val awakenings = lastSession?.stages
                ?.count { it.stage == SleepSessionRecord.STAGE_TYPE_AWAKE } ?: 0

            // ── SpO₂ ─────────────────────────────────────────────────────────
            val spo2Records = c.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(sleepWindowStart, now))
            ).records
            // Health Connect armazena SpO₂ como fração 0-1 → converter para %
            val minSpo2 = spo2Records.minOfOrNull { it.percentage.value.toFloat() * 100f }

            // ── Frequência respiratória ───────────────────────────────────────
            val respRecords = c.readRecords(
                ReadRecordsRequest(RespiratoryRateRecord::class, TimeRangeFilter.between(last24h, now))
            ).records
            val avgResp = if (respRecords.isNotEmpty())
                respRecords.map { it.rate }.average().toFloat() else null

            HealthSnapshot(
                avgHeartRateBpm = avgHR,
                sleepDurationHours = sleepDuration,
                sleepInterruptions = awakenings,
                minSpo2Percent = minSpo2,
                avgRespiratoryRate = avgResp
            )
        } catch (e: Exception) { null }
    }
}
