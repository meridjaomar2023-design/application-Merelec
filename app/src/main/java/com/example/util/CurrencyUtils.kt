package com.example.util

import com.example.data.model.OperationType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyUtils {

    private val symbols = DecimalFormatSymbols(Locale.FRENCH).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }

    private val daFormatter = DecimalFormat("#,##0.00", symbols)
    private val daCompactFormatter = DecimalFormat("#,##0", symbols)

    /**
     * Format a double amount into Algerian Dinar string:
     * e.g. 15000.0 -> "15 000,00 DA"
     */
    fun formatDa(amount: Double): String {
        return "${daFormatter.format(amount)} DA"
    }

    /**
     * Format without mandatory decimals if amount is whole:
     * e.g. 15000.0 -> "15 000 DA", 15000.5 -> "15 000,50 DA"
     */
    fun formatDaSmart(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "${daCompactFormatter.format(amount)} DA"
        } else {
            "${daFormatter.format(amount)} DA"
        }
    }

    /**
     * Format quantity cleanly (e.g. 5 instead of 5.0, or 3.25)
     */
    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toLong().toString()
        } else {
            qty.toString().replace('.', ',')
        }
    }

    /**
     * Compact format for chart axes and badges:
     * e.g. 1 500 000 DA -> "1,5M DA", 85 000 DA -> "85k DA", 500 DA -> "500 DA"
     */
    fun formatDaCompact(amount: Double): String {
        val absVal = abs(amount)
        return when {
            absVal >= 1_000_000.0 -> {
                val millions = amount / 1_000_000.0
                if (millions % 1.0 == 0.0) {
                    "${millions.toLong()}M DA"
                } else {
                    String.format(Locale.FRENCH, "%.1fM DA", millions)
                }
            }
            absVal >= 1_000.0 -> {
                val thousands = amount / 1_000.0
                if (thousands % 1.0 == 0.0) {
                    "${thousands.toLong()}k DA"
                } else {
                    String.format(Locale.FRENCH, "%.1fk DA", thousands)
                }
            }
            else -> formatDaSmart(amount)
        }
    }

    /**
     * Parse text input into clean Double amount
     */
    fun parseAmount(input: String): Double {
        val sanitized = input.trim().replace(" ", "").replace(",", ".")
        return sanitized.toDoubleOrNull() ?: 0.0
    }

    /**
     * Convertit un montant en chiffres vers son écriture intégrale en lettres (Français) et en Dinars Algériens.
     * Ex: 15450.50 -> "Quinze mille quatre cent cinquante dinars algériens et cinquante centimes"
     */
    fun amountToFrenchWordsDa(amount: Double): String {
        val cleanAmount = abs(amount)
        val totalCentimes = (cleanAmount * 100.0).roundToLong()
        val dinars = totalCentimes / 100L
        val centimes = (totalCentimes % 100L).toInt()

        val dinarsWords = numberToFrenchWords(dinars)
        val dinarUnit = if (dinars <= 1L) "dinar algérien" else "dinars algériens"

        val result = if (centimes > 0) {
            val centimesWords = numberToFrenchWords(centimes.toLong())
            val centimeUnit = if (centimes == 1) "centime" else "centimes"
            "$dinarsWords $dinarUnit et $centimesWords $centimeUnit"
        } else {
            "$dinarsWords $dinarUnit"
        }

        return result.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
        }
    }

    /**
     * Retourne l'intitulé légal d'arrêté de somme selon le type de document commercial.
     */
    fun formatArretePrefix(type: OperationType): String {
        return when (type) {
            OperationType.FACTURE, OperationType.FACTURE_FOURNISSEUR -> "Arrêtée la présente facture à la somme de :"
            OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> "Arrêté le présent avoir à la somme de :"
            OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> "Arrêté le présent devis à la somme de :"
            OperationType.BON_COMMANDE -> "Arrêté le présent bon de commande à la somme de :"
            OperationType.REGLEMENT -> "Arrêté le présent reçu à la somme de :"
            OperationType.BL_CLIENT, OperationType.BL_FOURNISSEUR -> "Arrêté la présente facture / bon à la somme de :"
        }
    }

    /**
     * Retourne la mention légale complète avec le montant en lettres et en Dinars Algériens.
     */
    fun formatArreteSommeText(type: OperationType, amount: Double): String {
        return "${formatArretePrefix(type)} ${amountToFrenchWordsDa(amount)} (${formatDa(amount)})."
    }

    private val unitsNames = arrayOf(
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
        "dix-sept", "dix-huit", "dix-neuf"
    )

    private val tensNames = arrayOf(
        "", "dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt", "quatre-vingt"
    )

    private fun convertBelowOneHundred(n: Int, isFinalGroup: Boolean = true): String {
        if (n == 0) return ""
        if (n < 20) return unitsNames[n]

        val ten = n / 10
        val unit = n % 10

        return when (ten) {
            7 -> {
                val sub = 10 + unit
                if (unit == 1) "soixante et onze" else "soixante-${unitsNames[sub]}"
            }
            8 -> {
                if (unit == 0) {
                    if (isFinalGroup) "quatre-vingts" else "quatre-vingt"
                } else {
                    "quatre-vingt-${unitsNames[unit]}"
                }
            }
            9 -> {
                val sub = 10 + unit
                "quatre-vingt-${unitsNames[sub]}"
            }
            else -> {
                val tenStr = tensNames[ten]
                when {
                    unit == 0 -> tenStr
                    unit == 1 -> "$tenStr et un"
                    else -> "$tenStr-${unitsNames[unit]}"
                }
            }
        }
    }

    private fun convertBelowOneThousand(n: Int, isFinalGroup: Boolean = true): String {
        if (n == 0) return ""
        val hundreds = n / 100
        val remainder = n % 100

        val hundredPart = when {
            hundreds == 0 -> ""
            hundreds == 1 -> "cent"
            remainder == 0 && isFinalGroup -> "${unitsNames[hundreds]} cents"
            else -> "${unitsNames[hundreds]} cent"
        }

        val remPart = convertBelowOneHundred(remainder, isFinalGroup)

        return when {
            hundredPart.isEmpty() -> remPart
            remPart.isEmpty() -> hundredPart
            else -> "$hundredPart $remPart"
        }
    }

    private fun numberToFrenchWords(number: Long): String {
        if (number == 0L) return "zéro"

        var n = number
        val milliards = (n / 1_000_000_000L).toInt()
        n %= 1_000_000_000L
        val millions = (n / 1_000_000L).toInt()
        n %= 1_000_000L
        val milliers = (n / 1_000L).toInt()
        val reste = (n % 1_000L).toInt()

        val parts = mutableListOf<String>()

        if (milliards > 0) {
            val w = convertBelowOneThousand(milliards, isFinalGroup = false)
            parts.add(if (milliards == 1) "un milliard" else "$w milliards")
        }

        if (millions > 0) {
            val w = convertBelowOneThousand(millions, isFinalGroup = false)
            parts.add(if (millions == 1) "un million" else "$w millions")
        }

        if (milliers > 0) {
            if (milliers == 1) {
                parts.add("mille")
            } else {
                val w = convertBelowOneThousand(milliers, isFinalGroup = false)
                parts.add("$w mille")
            }
        }

        if (reste > 0) {
            parts.add(convertBelowOneThousand(reste, isFinalGroup = true))
        }

        return parts.joinToString(" ").trim()
    }
}

