package com.example.data.model

import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodType(val label: String, val shortLabel: String) {
    HEBDOMADAIRE("Hebdomadaire", "Semaines"),
    MENSUEL("Mensuel", "Mois")
}

data class PeriodBucket(
    val key: String,
    val label: String,
    val shortLabel: String,
    val startDate: Long,
    val endDate: Long,
    val caTotal: Double,
    val reglementsTotal: Double,
    val achatsTotal: Double,
    val countBlClient: Int,
    val countReglements: Int,
    val countBlFournisseur: Int,
    val operations: List<OperationWithLignes>
) {
    val soldeNet: Double
        get() = caTotal - achatsTotal

    val resteARecouvrer: Double
        get() = (caTotal - reglementsTotal).coerceAtLeast(0.0)

    val tauxRecouvrement: Float
        get() = if (caTotal > 0.0) {
            ((reglementsTotal / caTotal) * 100).toFloat().coerceIn(0f, 100f)
        } else if (reglementsTotal > 0.0) {
            100f
        } else {
            0f
        }
}

data class DashboardSummary(
    val periodType: PeriodType,
    val buckets: List<PeriodBucket>,
    val totalCa: Double,
    val totalReglements: Double,
    val totalAchats: Double,
    val globalRecoveryRate: Float,
    val caGrowthPercentage: Double?,
    val reglementsGrowthPercentage: Double?,
    val averageCaPerPeriod: Double,
    val averageReglementsPerPeriod: Double,
    val paymentModeDistribution: Map<String, Double>
)
