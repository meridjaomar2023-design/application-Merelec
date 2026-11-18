package com.example.util

import com.example.data.model.DashboardSummary
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PeriodBucket
import com.example.data.model.PeriodType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AnalyticsCalculator {

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
    private val monthShortFormat = SimpleDateFormat("MMM", Locale.FRENCH)
    private val dayMonthFormat = SimpleDateFormat("d MMM", Locale.FRENCH)

    /**
     * Compute periodic dashboard summary based on selected period type and number of intervals.
     */
    fun computeSummary(
        operations: List<OperationWithLignes>,
        periodType: PeriodType,
        periodCount: Int = 8
    ): DashboardSummary {
        val buckets = when (periodType) {
            PeriodType.MENSUEL -> generateMonthlyBuckets(operations, periodCount)
            PeriodType.HEBDOMADAIRE -> generateWeeklyBuckets(operations, periodCount)
        }

        val totalCa = buckets.sumOf { it.caTotal }
        val totalReglements = buckets.sumOf { it.reglementsTotal }
        val totalAchats = buckets.sumOf { it.achatsTotal }

        val globalRecoveryRate = if (totalCa > 0.0) {
            ((totalReglements / totalCa) * 100).toFloat().coerceIn(0f, 100f)
        } else if (totalReglements > 0.0) {
            100f
        } else {
            0f
        }

        // Calculate growth rate comparing the last period to the one before it
        val (caGrowth, reglementsGrowth) = if (buckets.size >= 2) {
            val current = buckets.last()
            val previous = buckets[buckets.size - 2]

            val caG = if (previous.caTotal > 0.0) {
                ((current.caTotal - previous.caTotal) / previous.caTotal) * 100.0
            } else if (current.caTotal > 0.0) {
                100.0
            } else null

            val regG = if (previous.reglementsTotal > 0.0) {
                ((current.reglementsTotal - previous.reglementsTotal) / previous.reglementsTotal) * 100.0
            } else if (current.reglementsTotal > 0.0) {
                100.0
            } else null

            Pair(caG, regG)
        } else {
            Pair(null, null)
        }

        val nonZeroBuckets = buckets.size.coerceAtLeast(1)
        val avgCa = totalCa / nonZeroBuckets
        val avgReg = totalReglements / nonZeroBuckets

        // Mode de règlement distribution across operations in scope
        val paymentModes = mutableMapOf<String, Double>()
        for (item in operations) {
            val mode = item.operation.modeReglement.ifBlank { "Non spécifié" }
            val amount = if (item.operation.typeOperation == OperationType.REGLEMENT) {
                item.operation.montantVerse
            } else {
                item.operation.montantVerse
            }
            if (amount > 0.0) {
                paymentModes[mode] = (paymentModes[mode] ?: 0.0) + amount
            }
        }

        return DashboardSummary(
            periodType = periodType,
            buckets = buckets,
            totalCa = totalCa,
            totalReglements = totalReglements,
            totalAchats = totalAchats,
            globalRecoveryRate = globalRecoveryRate,
            caGrowthPercentage = caGrowth,
            reglementsGrowthPercentage = reglementsGrowth,
            averageCaPerPeriod = avgCa,
            averageReglementsPerPeriod = avgReg,
            paymentModeDistribution = paymentModes
        )
    }

    private fun generateMonthlyBuckets(
        operations: List<OperationWithLignes>,
        count: Int
    ): List<PeriodBucket> {
        val buckets = mutableListOf<PeriodBucket>()
        val cal = Calendar.getInstance(Locale.FRENCH)

        // Reset to first millisecond of current month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Go back (count - 1) months
        cal.add(Calendar.MONTH, -(count - 1))

        for (i in 0 until count) {
            val start = cal.timeInMillis

            val endCal = cal.clone() as Calendar
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)
            val end = endCal.timeInMillis

            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val key = String.format(Locale.US, "%04d-%02d", year, month)

            val fullLabel = monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }
            val shortLabel = monthShortFormat.format(cal.time).replaceFirstChar { it.uppercase() }

            val inPeriodOps = operations.filter { it.operation.dateTime in start..end }

            var caSum = 0.0
            var regSum = 0.0
            var achSum = 0.0
            var blcCount = 0
            var regCount = 0
            var blfCount = 0

            for (opWithLignes in inPeriodOps) {
                when (opWithLignes.operation.typeOperation) {
                    OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                        caSum += opWithLignes.totalTtc
                        blcCount++
                        // Si le client a versé un acompte sur place, l'ajouter aux règlements
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum += opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.REGLEMENT -> {
                        regSum += opWithLignes.operation.montantVerse
                        regCount++
                    }
                    OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                        achSum += opWithLignes.totalTtc
                        blfCount++
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum += opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_CLIENT -> {
                        caSum -= opWithLignes.totalTtc
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum -= opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_FOURNISSEUR -> {
                        achSum -= opWithLignes.totalTtc
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum -= opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> {
                        // Devis / Propos commerciales (hors CA réalisé)
                    }
                }
            }

            buckets.add(
                PeriodBucket(
                    key = key,
                    label = fullLabel,
                    shortLabel = shortLabel,
                    startDate = start,
                    endDate = end,
                    caTotal = caSum,
                    reglementsTotal = regSum,
                    achatsTotal = achSum,
                    countBlClient = blcCount,
                    countReglements = regCount,
                    countBlFournisseur = blfCount,
                    operations = inPeriodOps
                )
            )

            cal.add(Calendar.MONTH, 1)
        }

        return buckets
    }

    private fun generateWeeklyBuckets(
        operations: List<OperationWithLignes>,
        count: Int
    ): List<PeriodBucket> {
        val buckets = mutableListOf<PeriodBucket>()
        val cal = Calendar.getInstance(Locale.FRENCH)
        cal.firstDayOfWeek = Calendar.MONDAY

        // Set to Monday of current week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Go back (count - 1) weeks
        cal.add(Calendar.WEEK_OF_YEAR, -(count - 1))

        for (i in 0 until count) {
            val start = cal.timeInMillis

            val endCal = cal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_WEEK, 6)
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)
            val end = endCal.timeInMillis

            val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            val key = String.format(Locale.US, "%04d-W%02d", year, weekNum)

            val startStr = dayMonthFormat.format(Date(start))
            val endStr = dayMonthFormat.format(Date(end))
            val fullLabel = "Semaine $weekNum ($startStr - $endStr)"
            val shortLabel = "S$weekNum"

            val inPeriodOps = operations.filter { it.operation.dateTime in start..end }

            var caSum = 0.0
            var regSum = 0.0
            var achSum = 0.0
            var blcCount = 0
            var regCount = 0
            var blfCount = 0

            for (opWithLignes in inPeriodOps) {
                when (opWithLignes.operation.typeOperation) {
                    OperationType.BL_CLIENT, OperationType.FACTURE, OperationType.BON_COMMANDE -> {
                        caSum += opWithLignes.totalTtc
                        blcCount++
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum += opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.REGLEMENT -> {
                        regSum += opWithLignes.operation.montantVerse
                        regCount++
                    }
                    OperationType.BL_FOURNISSEUR, OperationType.FACTURE_FOURNISSEUR -> {
                        achSum += opWithLignes.totalTtc
                        blfCount++
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum += opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_CLIENT -> {
                        caSum -= opWithLignes.totalTtc
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum -= opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.AVOIR_FOURNISSEUR -> {
                        achSum -= opWithLignes.totalTtc
                        if (opWithLignes.operation.montantVerse > 0.0) {
                            regSum -= opWithLignes.operation.montantVerse
                        }
                    }
                    OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> {
                        // Devis / Propos commerciales (hors CA réalisé)
                    }
                }
            }

            buckets.add(
                PeriodBucket(
                    key = key,
                    label = fullLabel,
                    shortLabel = shortLabel,
                    startDate = start,
                    endDate = end,
                    caTotal = caSum,
                    reglementsTotal = regSum,
                    achatsTotal = achSum,
                    countBlClient = blcCount,
                    countReglements = regCount,
                    countBlFournisseur = blfCount,
                    operations = inPeriodOps
                )
            )

            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return buckets
    }
}
