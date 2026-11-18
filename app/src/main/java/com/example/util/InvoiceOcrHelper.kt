package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.ProductCatalogEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

enum class OcrConfidenceLevel(val label: String) {
    HIGH("Reconnaissance fiable"),
    MEDIUM("À vérifier"),
    LOW("Incertaine / Ambiguë")
}

enum class OcrColumnField(val shortLabel: String, val fullLabel: String) {
    QUANTITE("Qts", "Quantité (Qts)"),
    DESIGNATION("Désignation", "Désignation"),
    PU_HT("P.U.HT", "Prix Unitaire HT (P.U.HT)"),
    REFERENCE("Réf", "Référence (Réf)")
}

enum class OcrColumnPreset(
    val shortTitle: String,
    val description: String,
    val orderedFields: List<OcrColumnField>,
    val includeReference: Boolean = true,
    val isAuto: Boolean = false
) {
    AUTO(
        shortTitle = "Auto IA",
        description = "Détection automatique de l'ordre des colonnes",
        orderedFields = listOf(OcrColumnField.REFERENCE, OcrColumnField.DESIGNATION, OcrColumnField.QUANTITE, OcrColumnField.PU_HT),
        includeReference = true,
        isAuto = true
    ),
    QTE_DES_PU(
        shortTitle = "1. Qts → 2. Désignation → 3. P.U.HT",
        description = "Qts → Désignation → P.U.HT (sans Réf)",
        orderedFields = listOf(OcrColumnField.QUANTITE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT),
        includeReference = false
    ),
    QTE_REF_DES_PU(
        shortTitle = "1. Qts → 2. Réf → 3. Désignation → 4. P.U.HT",
        description = "Qts → Réf → Désignation → P.U.HT",
        orderedFields = listOf(OcrColumnField.QUANTITE, OcrColumnField.REFERENCE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT),
        includeReference = true
    ),
    DES_QTE_PU(
        shortTitle = "1. Désignation → 2. Qts → 3. P.U.HT",
        description = "Désignation → Qts → P.U.HT (sans Réf)",
        orderedFields = listOf(OcrColumnField.DESIGNATION, OcrColumnField.QUANTITE, OcrColumnField.PU_HT),
        includeReference = false
    ),
    REF_DES_QTE_PU(
        shortTitle = "1. Réf → 2. Désignation → 3. Qts → 4. P.U.HT",
        description = "Réf → Désignation → Qts → P.U.HT",
        orderedFields = listOf(OcrColumnField.REFERENCE, OcrColumnField.DESIGNATION, OcrColumnField.QUANTITE, OcrColumnField.PU_HT),
        includeReference = true
    ),
    REF_QTE_DES_PU(
        shortTitle = "1. Réf → 2. Qts → 3. Désignation → 4. P.U.HT",
        description = "Réf → Qts → Désignation → P.U.HT",
        orderedFields = listOf(OcrColumnField.REFERENCE, OcrColumnField.QUANTITE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT),
        includeReference = true
    ),
    DES_REF_QTE_PU(
        shortTitle = "1. Désignation → 2. Réf → 3. Qts → 4. P.U.HT",
        description = "Désignation → Réf → Qts → P.U.HT",
        orderedFields = listOf(OcrColumnField.DESIGNATION, OcrColumnField.REFERENCE, OcrColumnField.QUANTITE, OcrColumnField.PU_HT),
        includeReference = true
    ),
    REF_DES_PU_QTE(
        shortTitle = "1. Réf → 2. Désignation → 3. P.U.HT → 4. Qts",
        description = "Réf → Désignation → P.U.HT → Qts",
        orderedFields = listOf(OcrColumnField.REFERENCE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT, OcrColumnField.QUANTITE),
        includeReference = true
    );
}

data class OcrColumnConfig(
    val isAuto: Boolean = true,
    val orderedFields: List<OcrColumnField> = listOf(
        OcrColumnField.REFERENCE,
        OcrColumnField.DESIGNATION,
        OcrColumnField.QUANTITE,
        OcrColumnField.PU_HT
    ),
    val includeReference: Boolean = true,
    val userSelectedOrder: List<OcrColumnField> = emptyList()
) {
    val activeFields: List<OcrColumnField>
        get() {
            val base = if (includeReference) {
                orderedFields.distinct()
            } else {
                orderedFields.filter { it != OcrColumnField.REFERENCE }.distinct()
            }
            return base.ifEmpty {
                listOf(OcrColumnField.QUANTITE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT)
            }
        }

    /**
     * Liste des colonnes dans l'ordre exact d'affichage du tableau.
     */
    val displayFields: List<OcrColumnField>
        get() = if (isAuto) {
            listOf(
                OcrColumnField.REFERENCE,
                OcrColumnField.DESIGNATION,
                OcrColumnField.QUANTITE,
                OcrColumnField.PU_HT
            )
        } else {
            activeFields
        }

    /**
     * Position (1 = 1er, 2 = 2e, 3 = 3e, 4 = 4e) d'une colonne choisie par l'utilisateur, ou null si non sélectionnée.
     */
    fun selectedPositionOf(field: OcrColumnField): Int? {
        if (isAuto) return null
        val list = if (userSelectedOrder.isNotEmpty()) userSelectedOrder else activeFields
        val idx = list.indexOf(field)
        return if (idx >= 0) idx + 1 else null
    }

    /**
     * Permet de choisir les colonnes et leur ordre en touchant simplement chaque colonne dans l'ordre souhaité :
     * 1er clic -> 1re colonne (ex: Qts)
     * 2e clic -> 2e colonne (ex: Désignation)
     * 3e clic -> 3e colonne (ex: P.U.HT)
     * 4e clic -> 4e colonne (ex: Réf)
     * Toucher une colonne déjà sélectionnée la retire et renumérote les suivantes.
     */
    fun toggleFieldInOrder(field: OcrColumnField): OcrColumnConfig {
        val currentSelection = if (isAuto) {
            emptyList()
        } else if (userSelectedOrder.isNotEmpty()) {
            userSelectedOrder
        } else {
            activeFields
        }

        val newSelection = if (currentSelection.contains(field)) {
            currentSelection - field
        } else {
            currentSelection + field
        }

        if (newSelection.isEmpty()) {
            return OcrColumnConfig(isAuto = true)
        }

        val coreDefaults = listOf(
            OcrColumnField.QUANTITE,
            OcrColumnField.DESIGNATION,
            OcrColumnField.PU_HT
        )
        val remainingCore = coreDefaults.filter { it !in newSelection }
        val fullOrder = (newSelection + remainingCore).distinct().toMutableList()
        val hasRef = OcrColumnField.REFERENCE in newSelection
        if (!fullOrder.contains(OcrColumnField.REFERENCE)) {
            fullOrder.add(OcrColumnField.REFERENCE)
        }

        return OcrColumnConfig(
            isAuto = false,
            orderedFields = fullOrder,
            includeReference = hasRef,
            userSelectedOrder = newSelection
        )
    }

    val qteBeforePu: Boolean
        get() {
            val fields = activeFields
            val qIdx = fields.indexOf(OcrColumnField.QUANTITE)
            val pIdx = fields.indexOf(OcrColumnField.PU_HT)
            return if (qIdx >= 0 && pIdx >= 0) qIdx < pIdx else true
        }

    val qteBeforeDesignation: Boolean
        get() {
            val fields = activeFields
            val qIdx = fields.indexOf(OcrColumnField.QUANTITE)
            val dIdx = fields.indexOf(OcrColumnField.DESIGNATION)
            return qIdx in 0 until dIdx
        }

    val puBeforeDesignation: Boolean
        get() {
            val fields = activeFields
            val pIdx = fields.indexOf(OcrColumnField.PU_HT)
            val dIdx = fields.indexOf(OcrColumnField.DESIGNATION)
            return pIdx in 0 until dIdx
        }

    val refBeforeDesignation: Boolean
        get() {
            if (!includeReference) return false
            val fields = activeFields
            val rIdx = fields.indexOf(OcrColumnField.REFERENCE)
            val dIdx = fields.indexOf(OcrColumnField.DESIGNATION)
            return rIdx in 0 until dIdx
        }

    val qteBeforeRef: Boolean
        get() {
            if (!includeReference) return true
            val fields = activeFields
            val qIdx = fields.indexOf(OcrColumnField.QUANTITE)
            val rIdx = fields.indexOf(OcrColumnField.REFERENCE)
            return qIdx in 0 until rIdx
        }

    val refAfterNumbers: Boolean
        get() {
            if (!includeReference) return false
            val fields = activeFields
            val rIdx = fields.indexOf(OcrColumnField.REFERENCE)
            val qIdx = fields.indexOf(OcrColumnField.QUANTITE)
            val pIdx = fields.indexOf(OcrColumnField.PU_HT)
            val dIdx = fields.indexOf(OcrColumnField.DESIGNATION)
            return rIdx > dIdx && rIdx > qIdx && rIdx > pIdx
        }

    fun summaryLabel(): String {
        if (isAuto) return "Auto IA (Détection automatique)"
        return activeFields.mapIndexed { i, f -> "${i + 1}. ${f.shortLabel}" }.joinToString(" → ")
    }

    fun moveField(field: OcrColumnField, delta: Int): OcrColumnConfig {
        val list = activeFields.toMutableList()
        val idx = list.indexOf(field)
        if (idx < 0) return this
        val target = (idx + delta).coerceIn(0, list.lastIndex)
        if (target == idx) return this.copy(isAuto = false, userSelectedOrder = list)
        val item = list.removeAt(idx)
        list.add(target, item)
        val fullOrder = list.toMutableList()
        if (!fullOrder.contains(OcrColumnField.REFERENCE)) {
            fullOrder.add(OcrColumnField.REFERENCE)
        }
        return copy(
            isAuto = false,
            orderedFields = fullOrder,
            userSelectedOrder = list
        )
    }

    fun matchesPreset(preset: OcrColumnPreset): Boolean {
        if (preset.isAuto) return isAuto
        if (isAuto) return false
        if (includeReference != preset.includeReference) return false
        val expected = if (preset.includeReference) preset.orderedFields else preset.orderedFields.filter { it != OcrColumnField.REFERENCE }
        return activeFields == expected
    }

    companion object {
        fun fromPreset(preset: OcrColumnPreset): OcrColumnConfig {
            val active = if (preset.includeReference) {
                preset.orderedFields
            } else {
                preset.orderedFields.filter { it != OcrColumnField.REFERENCE }
            }
            val fullList = active.toMutableList()
            if (!fullList.contains(OcrColumnField.REFERENCE)) {
                fullList.add(OcrColumnField.REFERENCE)
            }
            return OcrColumnConfig(
                isAuto = preset.isAuto,
                orderedFields = fullList,
                includeReference = preset.includeReference,
                userSelectedOrder = if (preset.isAuto) emptyList() else active
            )
        }
    }
}

data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pageIndex: Int = 0
)

data class OcrExtractedLine(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int = 0,
    val reference: String = "",
    val designation: String,
    val quantite: Double = 1.0,
    val prixUnitaire: Double = 0.0,
    val remisePct: Double = 0.0,
    val tauxTva: Double = 19.0,
    val majorationPct: Double = 0.0,
    val applyMajoration: Boolean = true,
    val extractedMontantHt: Double? = null,
    val confidence: OcrConfidenceLevel = OcrConfidenceLevel.HIGH,
    val confidenceNote: String = "Reconnaissance fiable",
    val boundingBox: NormalizedBoundingBox? = null,
    val userVerified: Boolean = false,
    val selected: Boolean = true,
    val matchedCatalogProductName: String? = null
) {
    /**
     * Prix de vente HT = Prix d'achat HT × (1 + Pourcentage / 100)
     */
    val puHtAfterMajoration: Double
        get() {
            val pct = if (applyMajoration) majorationPct else 0.0
            return round(prixUnitaire * (1.0 + pct / 100.0) * 100.0) / 100.0
        }

    /**
     * PU HT après remise individuelle (%)
     */
    val puHtNet: Double
        get() {
            val rem = remisePct.coerceIn(0.0, 100.0)
            return round(puHtAfterMajoration * (1.0 - rem / 100.0) * 100.0) / 100.0
        }

    /**
     * Total HT ligne = Quantité × PU HT après remise
     */
    val totalHt: Double
        get() = round(quantite * puHtNet * 100.0) / 100.0

    /**
     * Montant TVA de la ligne = Total HT ligne × (Taux TVA / 100)
     */
    val montantTva: Double
        get() = round(totalHt * (tauxTva.coerceAtLeast(0.0) / 100.0) * 100.0) / 100.0

    val totalTtc: Double
        get() = round((totalHt + montantTva) * 100.0) / 100.0

    val hasCalculationMismatch: Boolean
        get() {
            val expectedRawHt = quantite * prixUnitaire * (1.0 - remisePct.coerceIn(0.0, 100.0) / 100.0)
            val ext = extractedMontantHt ?: return false
            if (ext <= 0.0) return false
            return abs(expectedRawHt - ext) > max(2.0, ext * 0.05)
        }

    val effectiveConfidence: OcrConfidenceLevel
        get() {
            if (userVerified) return OcrConfidenceLevel.HIGH
            if (hasCalculationMismatch || designation.isBlank() || quantite <= 0.0) {
                return OcrConfidenceLevel.LOW
            }
            if (prixUnitaire <= 0.0) return OcrConfidenceLevel.MEDIUM
            return confidence
        }
}

data class InvoiceOcrResult(
    val rawText: String,
    val reconstructedRows: List<String>,
    val detectedInvoiceNumber: String = "",
    val detectedPartnerName: String = "",
    val detectedDateText: String = "",
    val suggestedOperationType: OperationType = OperationType.FACTURE_FOURNISSEUR,
    val appliquerTva: Boolean = false,
    val tauxTva: Double = 19.0,
    val extractedLines: List<OcrExtractedLine> = emptyList(),
    val detectedTotalHt: Double? = null,
    val detectedTotalTtc: Double? = null,
    val detectedTotalTva: Double? = null,
    val detectedBonNumber: String = "",
    val detectedBonDate: String = "",
    val detectedOrderReference: String = "",
    val detectedPaymentConditions: String = "",
    val detectedAddress: String = "",
    val detectedPhone: String = "",
    val detectedRc: String = "",
    val detectedNif: String = "",
    val detectedNis: String = "",
    val detectedArticleImposition: String = "",
    val detectedRib: String = "",
    val detectedColumnOrderDescription: String = "Automatique (Réf | Désignation | Qté | PU HT)",
    val analyzedByAi: Boolean = false,
    val pageCount: Int = 1
) {
    val detectedDate: String get() = detectedDateText
    val detectedPartnerAddress: String get() = detectedAddress
    val detectedPartnerPhone: String get() = detectedPhone
    val detectedPartnerRc: String get() = detectedRc
    val detectedPartnerNif: String get() = detectedNif
    val detectedPartnerNis: String get() = detectedNis
    val detectedPartnerAi: String get() = detectedArticleImposition
    val detectedPartnerRib: String get() = detectedRib
    val detectedOrderRef: String get() = detectedOrderReference
    val detectedDocumentType: String get() = suggestedOperationType.name
    val detectedColumnOrder: String get() = detectedColumnOrderDescription
    val extractedTotalHt: Double get() = detectedTotalHt ?: 0.0
}

object InvoiceOcrHelper {

    fun loadNormalizedBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return null

            val maxDim = 2048
            var sampleSize = 1
            while (width / sampleSize > maxDim || height / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
            if (decoded.config != Bitmap.Config.ARGB_8888) {
                decoded.copy(Bitmap.Config.ARGB_8888, false) ?: decoded
            } else {
                decoded
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun recognizeBitmapWithMlKitSuspend(
        bitmap: Bitmap,
        pageIndex: Int = 0,
        knownPartners: List<PartnerEntity> = emptyList(),
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        ownCompanyName: String = "",
        preferredQteBeforePu: Boolean? = null,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): InvoiceOcrResult {
        val outcome = withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            if (cont.isActive) {
                                val result = try {
                                    parseVisionTextToInvoice(
                                        visionText = visionText,
                                        knownPartners = knownPartners,
                                        catalogProducts = catalogProducts,
                                        ownCompanyName = ownCompanyName,
                                        pageIndex = pageIndex,
                                        imageWidth = bitmap.width,
                                        imageHeight = bitmap.height,
                                        preferredQteBeforePu = preferredQteBeforePu,
                                        columnConfig = columnConfig
                                    )
                                } catch (_: Exception) {
                                    InvoiceOcrResult(
                                        rawText = visionText.text,
                                        reconstructedRows = visionText.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                    )
                                }
                                cont.resume(result)
                            }
                        }
                        .addOnFailureListener {
                            if (cont.isActive) {
                                cont.resume(
                                    InvoiceOcrResult(
                                        rawText = "",
                                        reconstructedRows = emptyList()
                                    )
                                )
                            }
                        }
                        .addOnCanceledListener {
                            if (cont.isActive) {
                                cont.resume(
                                    InvoiceOcrResult(
                                        rawText = "",
                                        reconstructedRows = emptyList()
                                    )
                                )
                            }
                        }
                } catch (_: Exception) {
                    if (cont.isActive) {
                        cont.resume(
                            InvoiceOcrResult(
                                rawText = "",
                                reconstructedRows = emptyList()
                            )
                        )
                    }
                }
            }
        }
        return outcome ?: InvoiceOcrResult(rawText = "", reconstructedRows = emptyList())
    }

    fun recognizeInvoiceFromUri(
        context: Context,
        imageUri: Uri,
        knownPartners: List<PartnerEntity> = emptyList(),
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        ownCompanyName: String = "",
        onSuccess: (InvoiceOcrResult, Bitmap?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadNormalizedBitmapFromUri(context, imageUri)
                }
                if (bitmap == null) {
                    onError("Impossible de décoder l'image sélectionnée. Veuillez choisir un fichier JPG ou PNG valide.")
                    return@launch
                }
                val parsed = withContext(Dispatchers.IO) {
                    recognizeBitmapWithMlKitSuspend(
                        bitmap = bitmap,
                        pageIndex = 0,
                        knownPartners = knownPartners,
                        catalogProducts = catalogProducts,
                        ownCompanyName = ownCompanyName
                    )
                }
                onSuccess(parsed, bitmap)
            } catch (e: Exception) {
                onError("Erreur lors de la reconnaissance OCR : ${e.localizedMessage ?: "échec de lecture"}")
            }
        }
    }

    data class ReconstructedRowWithBox(
        val text: String,
        val boundingBox: NormalizedBoundingBox?
    )

    private data class PositionedTextSegment(
        val text: String,
        val box: Rect,
        val centerY: Float,
        val height: Float
    )

    private fun reconstructHorizontalRowsWithBoxes(
        visionText: Text,
        pageIndex: Int = 0,
        imageWidth: Int = 1000,
        imageHeight: Int = 1400
    ): List<ReconstructedRowWithBox> {
        val safeW = imageWidth.coerceAtLeast(1).toFloat()
        val safeH = imageHeight.coerceAtLeast(1).toFloat()
        val segments = mutableListOf<PositionedTextSegment>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val elems = line.elements
                val clean = if (elems.size > 1) {
                    buildString {
                        for (idx in elems.indices) {
                            val el = elems[idx]
                            val t = el.text.trim()
                            if (t.isEmpty()) continue
                            if (isNotEmpty() && idx > 0) {
                                val prevBox = elems[idx - 1].boundingBox
                                val curBox = el.boundingBox
                                val gap = if (prevBox != null && curBox != null) {
                                    (curBox.left - prevBox.right).toFloat()
                                } else 0f
                                val prevCharW = if (prevBox != null && elems[idx - 1].text.isNotEmpty()) {
                                    prevBox.width().toFloat() / elems[idx - 1].text.length.coerceAtLeast(1)
                                } else 12f
                                if (gap > max(prevCharW * 1.5f, 14f)) {
                                    append("   ")
                                } else {
                                    append(" ")
                                }
                            }
                            append(t)
                        }
                    }.trim()
                } else {
                    line.text.trim()
                }
                if (clean.isEmpty()) continue
                val rect = line.boundingBox
                if (rect != null) {
                    val h = (rect.bottom - rect.top).toFloat().coerceAtLeast(8f)
                    val cy = (rect.top + rect.bottom) / 2f
                    segments.add(PositionedTextSegment(clean, rect, cy, h))
                } else {
                    segments.add(PositionedTextSegment(clean, Rect(0, 0, 0, 0), Float.MAX_VALUE, 16f))
                }
            }
        }

        if (segments.isEmpty()) {
            return visionText.text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { ReconstructedRowWithBox(it, null) }
        }

        val sortedByY = segments.sortedWith(compareBy({ it.centerY }, { it.box.left }))
        val rowClusters = mutableListOf<MutableList<PositionedTextSegment>>()

        for (seg in sortedByY) {
            if (seg.centerY == Float.MAX_VALUE) {
                rowClusters.add(mutableListOf(seg))
                continue
            }
            val matchingCluster = rowClusters.lastOrNull { cluster ->
                val lastCy = cluster.map { it.centerY }.average().toFloat()
                val avgH = cluster.map { it.height }.average().toFloat()
                val tolerance = max(avgH * 0.82f, 18f)
                abs(seg.centerY - lastCy) <= tolerance
            }
            if (matchingCluster != null) {
                matchingCluster.add(seg)
            } else {
                rowClusters.add(mutableListOf(seg))
            }
        }

        return rowClusters.mapNotNull { cluster ->
            val sortedCluster = cluster.sortedBy { it.box.left }
            val joined = sortedCluster
                .joinToString("   ") { it.text }
                .replace(Regex("\\s{4,}"), "   ")
                .trim()
            if (joined.isBlank()) return@mapNotNull null

            val validBoxes = sortedCluster.map { it.box }.filter { it.width() > 0 && it.height() > 0 }
            val normBox = if (validBoxes.isNotEmpty()) {
                val minL = (validBoxes.minOf { it.left } / safeW).coerceIn(0f, 1f)
                val minT = (validBoxes.minOf { it.top } / safeH).coerceIn(0f, 1f)
                val maxR = (validBoxes.maxOf { it.right } / safeW).coerceIn(0f, 1f)
                val maxB = (validBoxes.maxOf { it.bottom } / safeH).coerceIn(0f, 1f)
                NormalizedBoundingBox(minL, minT, maxR, maxB, pageIndex)
            } else {
                null
            }
            ReconstructedRowWithBox(joined, normBox)
        }
    }

    fun parseVisionTextToInvoice(
        visionText: Text,
        knownPartners: List<PartnerEntity> = emptyList(),
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        ownCompanyName: String = "",
        pageIndex: Int = 0,
        imageWidth: Int = 1000,
        imageHeight: Int = 1400,
        preferredQteBeforePu: Boolean? = null,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): InvoiceOcrResult {
        val rowsWithBoxes = reconstructHorizontalRowsWithBoxes(visionText, pageIndex, imageWidth, imageHeight)
        val horizontalRows = rowsWithBoxes.map { it.text }
        val rawFullText = if (horizontalRows.isNotEmpty()) {
            horizontalRows.joinToString("\n")
        } else {
            visionText.text
        }
        return parseRawInvoiceText(
            rawText = rawFullText,
            reconstructedRows = horizontalRows,
            knownPartners = knownPartners,
            catalogProducts = catalogProducts,
            ownCompanyName = ownCompanyName,
            preferredQteBeforePu = preferredQteBeforePu,
            rowsWithBoxesOverride = rowsWithBoxes,
            columnConfig = columnConfig
        )
    }

    fun reparseResultWithColumnConfig(
        existingResult: InvoiceOcrResult,
        columnConfig: OcrColumnConfig,
        catalogProducts: List<ProductCatalogEntity> = emptyList()
    ): InvoiceOcrResult {
        val rows = existingResult.reconstructedRows.ifEmpty {
            existingResult.rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        }
        val boxByDesignation = existingResult.extractedLines.associateBy(
            keySelector = { it.designation.take(12).lowercase(Locale.FRENCH) },
            valueTransform = { it.boundingBox }
        )
        val reExtracted = extractProductLinesFromRowsWithBoxes(
            rowsWithBoxes = rows.map { ReconstructedRowWithBox(it, null) },
            catalogProducts = catalogProducts,
            defaultTvaRate = if (existingResult.appliquerTva) existingResult.tauxTva else 19.0,
            preferredQteBeforePu = if (!columnConfig.isAuto) columnConfig.qteBeforePu else null,
            columnConfig = columnConfig
        ).map { line ->
            val preservedBox = line.boundingBox
                ?: boxByDesignation[line.designation.take(12).lowercase(Locale.FRENCH)]
            line.copy(boundingBox = preservedBox)
        }
        val orderDesc = if (!columnConfig.isAuto) {
            "Manuel : ${columnConfig.summaryLabel()}"
        } else {
            existingResult.detectedColumnOrderDescription
        }
        return existingResult.copy(
            extractedLines = reExtracted.ifEmpty { existingResult.extractedLines },
            detectedColumnOrderDescription = orderDesc
        )
    }

    fun parseRawInvoiceText(
        rawText: String,
        reconstructedRows: List<String> = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() },
        knownPartners: List<PartnerEntity> = emptyList(),
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        ownCompanyName: String = "",
        preferredQteBeforePu: Boolean? = null,
        rowsWithBoxesOverride: List<ReconstructedRowWithBox>? = null,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): InvoiceOcrResult {
        val horizontalRows = reconstructedRows.map { it.trim() }.filter { it.isNotEmpty() }
        val rowsWithBoxes = rowsWithBoxesOverride ?: horizontalRows.map { ReconstructedRowWithBox(it, null) }

        val detectedDocNumber = extractInvoiceNumber(horizontalRows)
        val detectedBonNum = extractBonNumber(horizontalRows)
        val detectedOrderRef = extractOrderReference(horizontalRows)
        val detectedPaymentCond = extractPaymentConditions(horizontalRows)
        val detectedDate = extractDateString(horizontalRows)
        val detectedPartner = extractPartnerName(horizontalRows, knownPartners, ownCompanyName)
        val (hasTva, tvaRate) = extractTvaInfo(horizontalRows)
        val suggestedType = detectSuggestedOperationType(horizontalRows, knownPartners, detectedPartner)
        val (totalHt, totalTtc) = extractTotals(horizontalRows)
        val totalTva = extractTotalTva(horizontalRows)
        val fiscalInfo = extractFiscalAndContactMetadata(horizontalRows)

        val effectivePreferredQteBeforePu = if (!columnConfig.isAuto) {
            columnConfig.qteBeforePu
        } else {
            preferredQteBeforePu
        }

        val extractedLines = extractProductLinesFromRowsWithBoxes(
            rowsWithBoxes = rowsWithBoxes,
            catalogProducts = catalogProducts,
            defaultTvaRate = if (hasTva) tvaRate else 19.0,
            preferredQteBeforePu = effectivePreferredQteBeforePu,
            columnConfig = columnConfig
        )

        val headerIndex = horizontalRows.indexOfFirst { isTableHeaderRow(it) }
        val headerOrder = if (!columnConfig.isAuto) {
            columnConfig.qteBeforePu
        } else if (headerIndex >= 0) {
            detectHeaderColumnOrder(horizontalRows.slice(headerIndex..minOf(headerIndex + 2, horizontalRows.lastIndex)))
        } else {
            preferredQteBeforePu
        }
        val orderDesc = if (!columnConfig.isAuto) {
            "Choix utilisateur : ${columnConfig.summaryLabel()}"
        } else {
            when (headerOrder) {
                true -> "Réf / Désignation → Quantité (Qts) → Prix Unitaire (PU HT)"
                false -> "Réf / Désignation → Prix Unitaire (PU HT) → Quantité (Qts)"
                null -> "Détection contextuelle intelligente (Qts / PU HT / Montant HT)"
            }
        }

        return InvoiceOcrResult(
            rawText = rawText,
            reconstructedRows = horizontalRows,
            detectedInvoiceNumber = detectedDocNumber,
            detectedPartnerName = detectedPartner,
            detectedDateText = detectedDate,
            suggestedOperationType = suggestedType,
            appliquerTva = hasTva,
            tauxTva = tvaRate,
            extractedLines = extractedLines,
            detectedTotalHt = totalHt,
            detectedTotalTtc = totalTtc,
            detectedTotalTva = totalTva,
            detectedBonNumber = detectedBonNum,
            detectedBonDate = detectedDate,
            detectedOrderReference = detectedOrderRef,
            detectedPaymentConditions = detectedPaymentCond,
            detectedAddress = fiscalInfo["address"].orEmpty(),
            detectedPhone = fiscalInfo["phone"].orEmpty(),
            detectedRc = fiscalInfo["rc"].orEmpty(),
            detectedNif = fiscalInfo["nif"].orEmpty(),
            detectedNis = fiscalInfo["nis"].orEmpty(),
            detectedArticleImposition = fiscalInfo["ai"].orEmpty(),
            detectedRib = fiscalInfo["rib"].orEmpty(),
            detectedColumnOrderDescription = orderDesc
        )
    }

    private fun extractInvoiceNumber(rows: List<String>): String {
        val patterns = listOf(
            Regex("(?i)(?:facture|fac|invoice|فاتورة)\\s*(?:proforma\\s*)?(?:n[°oº]?|num[eé]ro|no|#)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_\\-]{1,22})"),
            Regex("(?i)(?:bon\\s+de\\s+livraison|bl|devis)\\s*(?:n[°oº]?|num[eé]ro|#)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_\\-]{1,22})"),
            Regex("(?i)\\b(?:n[°oº]|r[eé]f(?:[eé]rence)?)\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_\\-]{2,22})")
        )
        for (row in rows.take(20)) {
            if (Regex("(?i)\\b(rc|nif|nis|nin|rib|article|t[eé]l)\\b").containsMatchIn(row) &&
                !Regex("(?i)\\b(facture|bon|devis|invoice)\\b").containsMatchIn(row)
            ) {
                continue
            }
            for (regex in patterns) {
                val match = regex.find(row)
                val candidate = match?.groupValues?.getOrNull(1)?.trim() ?: continue
                if (candidate.any { it.isDigit() } && candidate.length >= 2) {
                    return candidate
                }
            }
        }
        return ""
    }

    private fun extractBonNumber(rows: List<String>): String {
        val regex = Regex("(?i)(?:bon\\s+de\\s+livraison|b\\.?l|bon\\s+de\\s+r[eé]ception|delivery\\s+note)\\s*(?:n[°oº]?|num[eé]ro|#)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_\\-]{1,22})")
        for (row in rows.take(22)) {
            val m = regex.find(row)
            val c = m?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (c.any { it.isDigit() }) return c
        }
        return ""
    }

    private fun extractOrderReference(rows: List<String>): String {
        val regex = Regex("(?i)(?:bon\\s+de\\s+commande|b\\.?c|commande|order\\s+ref|r[eé]f\\.?\\s*commande)\\s*(?:n[°oº]?|#)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_\\-]{1,22})")
        for (row in rows.take(25)) {
            val m = regex.find(row)
            val c = m?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (c.any { it.isDigit() }) return c
        }
        return ""
    }

    private fun extractPaymentConditions(rows: List<String>): String {
        val regex = Regex("(?i)(?:mode\\s+de\\s+r[eé]glement|mode\\s+de\\s+paiement|conditions?\\s+de\\s+paiement|payment\\s+terms?)\\s*[:\\-]?\\s*(.+)")
        for (row in rows) {
            val m = regex.find(row)
            if (m != null) {
                val raw = m.groupValues[1].split("   ").firstOrNull()?.trim().orEmpty()
                if (raw.isNotBlank()) return raw.take(60)
            }
            if (Regex("(?i)\\b(esp[eè]ces|ch[eè]que|virement|terme|cr[eé]dit)\\b").containsMatchIn(row) &&
                Regex("(?i)\\b(r[eé]glement|paiement|mode)\\b").containsMatchIn(row)
            ) {
                return row.take(60)
            }
        }
        return ""
    }

    private fun extractFiscalAndContactMetadata(rows: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val rcRegex = Regex("(?i)\\bR\\.?C\\s*(?:N[°oº]?)?\\s*[:\\-]?\\s*([A-Z0-9/\\-.\\s]{5,28})")
        val nifRegex = Regex("(?i)\\b(?:N\\.?I\\.?F|M\\.?F|identifiant\\s+fiscal)\\s*(?:N[°oº]?)?\\s*[:\\-]?\\s*(\\d[\\d\\s]{8,24})")
        val nisRegex = Regex("(?i)\\bN\\.?I\\.?S\\s*(?:N[°oº]?)?\\s*[:\\-]?\\s*(\\d[\\d\\s]{8,24})")
        val aiRegex = Regex("(?i)\\b(?:A\\.?I|art(?:icle)?\\s*d'?imposition)\\s*(?:N[°oº]?)?\\s*[:\\-]?\\s*(\\d[\\d\\s]{6,20})")
        val phoneRegex = Regex("(?i)(?:t[eé]l(?:[eé]phone)?|mob(?:ile)?|gsm|phone)\\s*[:\\-.]?\\s*((?:\\+213|0)\\s*[567234]\\d(?:[\\s.\\-]?\\d{2}){4})")
        val addressRegex = Regex("(?i)(?:adresse|si[eè]ge\\s+social|rue|zone\\s+industrielle|cit[eé]|route|rn\\s*\\d+)\\s*[:\\-]?\\s*(.+)")
        val ribRegex = Regex("(?i)\\b(?:R\\.?I\\.?B|compte\\s+bancaire)\\s*[:\\-]?\\s*([0-9\\s]{12,30})")

        for (row in rows) {
            if (!map.containsKey("rc")) {
                rcRegex.find(row)?.groupValues?.getOrNull(1)?.split("   ")?.firstOrNull()?.trim()?.let {
                    if (it.any { c -> c.isDigit() }) map["rc"] = it.take(30)
                }
            }
            if (!map.containsKey("nif")) {
                nifRegex.find(row)?.groupValues?.getOrNull(1)?.replace(" ", "")?.trim()?.let {
                    if (it.length >= 8) map["nif"] = it.take(25)
                }
            }
            if (!map.containsKey("nis")) {
                nisRegex.find(row)?.groupValues?.getOrNull(1)?.replace(" ", "")?.trim()?.let {
                    if (it.length >= 8) map["nis"] = it.take(25)
                }
            }
            if (!map.containsKey("ai")) {
                aiRegex.find(row)?.groupValues?.getOrNull(1)?.replace(" ", "")?.trim()?.let {
                    if (it.length >= 6) map["ai"] = it.take(20)
                }
            }
            if (!map.containsKey("phone")) {
                phoneRegex.find(row)?.groupValues?.getOrNull(1)?.trim()?.let {
                    map["phone"] = it.take(25)
                }
            }
            if (!map.containsKey("address")) {
                addressRegex.find(row)?.groupValues?.getOrNull(1)?.split("   ")?.firstOrNull()?.trim()?.let {
                    if (it.length >= 5) map["address"] = it.take(90)
                }
            }
            if (!map.containsKey("rib")) {
                ribRegex.find(row)?.groupValues?.getOrNull(1)?.trim()?.let {
                    if (it.count { c -> c.isDigit() } >= 10) map["rib"] = it.take(35)
                }
            }
        }
        return map
    }

    private fun extractDateString(rows: List<String>): String {
        val dateRegex = Regex("\\b(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2})\\b")
        for (row in rows.take(20)) {
            val match = dateRegex.find(row)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return ""
    }

    private fun extractPartnerName(
        rows: List<String>,
        knownPartners: List<PartnerEntity>,
        ownCompanyName: String
    ): String {
        val ownClean = ownCompanyName.trim().lowercase(Locale.FRENCH)

        val sortedPartners = knownPartners
            .filter { it.name.trim().length >= 3 }
            .sortedByDescending { it.name.length }

        for (partner in sortedPartners) {
            val pName = partner.name.trim()
            if (ownClean.isNotEmpty() && pName.lowercase(Locale.FRENCH) == ownClean) continue
            val found = rows.any { row ->
                row.contains(pName, ignoreCase = true)
            }
            if (found) return pName
        }

        val labelRegex = Regex("(?i)(?:fournisseur|client|destinataire|doit|partenaire|supplier|customer)\\s*[:\\-]\\s*(.+)")
        for ((idx, row) in rows.take(20).withIndex()) {
            val m = labelRegex.find(row)
            if (m != null) {
                val after = m.groupValues[1].split("   ").firstOrNull()?.trim().orEmpty()
                if (after.length >= 3 && !after.lowercase(Locale.FRENCH).contains(ownClean.takeIf { it.isNotEmpty() } ?: "___")) {
                    return after.take(45)
                } else if (idx + 1 < rows.size) {
                    val nextLine = rows[idx + 1].split("   ").firstOrNull()?.trim().orEmpty()
                    if (nextLine.length >= 3) return nextLine.take(45)
                }
            }
        }

        val companyEntityRegex = Regex("(?i)\\b(SARL|EURL|SPA|SNC|ETS|ÉTABLISSEMENT|ETABLISSEMENT|STE|SOCI[EÉ]T[EÉ]|GROUPE)\\s+[A-Z0-9À-Ÿ\\s&'\\-.]{3,40}")
        for (row in rows.take(15)) {
            val match = companyEntityRegex.find(row)
            if (match != null) {
                val candidate = match.value.split("   ").firstOrNull()?.trim().orEmpty()
                if (candidate.length >= 4 && (ownClean.isEmpty() || !candidate.lowercase(Locale.FRENCH).contains(ownClean))) {
                    return candidate.take(45)
                }
            }
        }

        return ""
    }

    private fun detectSuggestedOperationType(
        rows: List<String>,
        knownPartners: List<PartnerEntity>,
        detectedPartnerName: String
    ): OperationType {
        val headerText = rows.take(15).joinToString(" ").lowercase(Locale.FRENCH)
        val matchedPartner = knownPartners.find {
            it.name.trim().equals(detectedPartnerName.trim(), ignoreCase = true)
        }
        return when {
            headerText.contains("bon de commande") || headerText.contains("purchase order") -> {
                OperationType.BON_COMMANDE
            }
            headerText.contains("bon de livraison") || headerText.contains("bon de réception") || headerText.contains("delivery note") -> {
                if (matchedPartner?.type?.name == "CLIENT") OperationType.BL_CLIENT else OperationType.BL_FOURNISSEUR
            }
            headerText.contains("devis") || headerText.contains("proforma") || headerText.contains("quotation") -> {
                if (matchedPartner?.type?.name == "CLIENT") OperationType.DEVIS_CLIENT else OperationType.DEVIS_FOURNISSEUR
            }
            else -> OperationType.FACTURE_FOURNISSEUR
        }
    }

    private fun extractTvaInfo(rows: List<String>): Pair<Boolean, Double> {
        for (row in rows) {
            if (Regex("(?i)\\b(tva|t\\.v\\.a|vat)\\b").containsMatchIn(row)) {
                if (row.contains("9%") || row.contains("9 %") || row.contains("9.0%") || row.contains("9,0%")) {
                    return true to 9.0
                }
                if (row.contains("19%") || row.contains("19 %") || row.contains("19.0%") || row.contains("19,0%")) {
                    return true to 19.0
                }
            }
        }
        return false to 19.0
    }

    private fun extractTotals(rows: List<String>): Pair<Double?, Double?> {
        var totalHt: Double? = null
        var totalTtc: Double? = null
        for (row in rows) {
            val lower = row.lowercase(Locale.FRENCH)
            val numbers = extractNumbersFromText(row)
            if (numbers.isEmpty()) continue
            if ((lower.contains("total") || lower.contains("montant")) && (lower.contains("ht") || lower.contains("h.t") || lower.contains("hors taxe"))) {
                totalHt = numbers.last()
            } else if ((lower.contains("total") && (lower.contains("ttc") || lower.contains("t.t.c") || lower.contains("net"))) || lower.contains("net à payer") || lower.contains("net a payer")) {
                totalTtc = numbers.last()
            }
        }
        return totalHt to totalTtc
    }

    private fun extractTotalTva(rows: List<String>): Double? {
        for (row in rows) {
            val lower = row.lowercase(Locale.FRENCH)
            if (Regex("\\b(t\\.?v\\.?a|taxe)\\b").containsMatchIn(lower) &&
                !isTableHeaderRow(row)
            ) {
                val numbers = extractNumbersFromText(row).filter { it != 19.0 && it != 9.0 }
                if (numbers.isNotEmpty()) {
                    return numbers.last()
                }
            }
        }
        return null
    }

    /**
     * Détecte si une ligne correspond à l'en-tête des colonnes du tableau d'articles (FR / EN / AR).
     */
    fun isTableHeaderRow(row: String): Boolean {
        val norm = row.lowercase(Locale.FRENCH)
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('à', 'a')
            .replace('°', ' ')
        val hasRefWord = Regex("\\b(ref|reference|code\\s+art|code|n\\s+art|sku|item\\s+code|المرجع|رمز)\\b").containsMatchIn(norm)
        val hasDesignationWord = Regex("\\b(designation|libelle|description|articles?|produits?|items?|product|التعيين|البيان|الصنف)\\b").containsMatchIn(norm)
        val hasQtyOrPriceWord = Regex("\\b(qte|quantite|qty|quantity|p\\.?u\\b|p\\.?u\\.?h\\.?t|prix\\s+unitaire|unit\\s+price|prix|unitaire|montant|u\\.m|unite|الكمية|السعر)\\b").containsMatchIn(norm)

        return (hasRefWord && (hasDesignationWord || hasQtyOrPriceWord)) ||
            (hasDesignationWord && hasQtyOrPriceWord) ||
            Regex("^\\s*(n\\s+)?(ref|reference|qte|qty|code)\\s+(designation|libelle|article|description)").containsMatchIn(norm)
    }

    /**
     * Détecte si une ligne appartient au bloc des totaux ou de pied de page (Total HT, TVA, Timbre, Total TTC, Net à payer, Arrêté...).
     */
    fun isTableFooterOrTotalRow(row: String): Boolean {
        if (isTableHeaderRow(row)) return false

        val norm = row.lowercase(Locale.FRENCH)
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('à', 'a')
            .replace('û', 'u')
            .trim()

        // Une deuxième ligne d'en-tête de colonne sans montant chiffré (ex: "HT   TVA   TTC" ou "Montant HT") n'est pas un pied de tableau
        val hasDigits = norm.any { it.isDigit() }
        if (!hasDigits && Regex("^(p\\.?u\\.?\\s*)?(montant\\s+ht|total\\s+ht|ht\\s+ttc|tva\\s+ttc|montant|valeur)\\b").containsMatchIn(norm)) {
            return false
        }

        if (Regex("\\b(total\\s+(brut|general|ht|h\\.t|ttc|t\\.t\\.c|net|facture|bon|devis|a\\s+payer)|sous[- ]?total|subtotal|montant\\s+(total|global|net|tva|t\\.v\\.a)|net\\s+a\\s+payer|reste\\s+a\\s+payer|المجموع|الصافي)\\b").containsMatchIn(norm)) {
            return true
        }

        if (hasDigits && Regex("^\\s*(montant\\s+(ht|h\\.t|ttc|t\\.t\\.c))\\b").containsMatchIn(norm)) {
            return true
        }

        if (Regex("^\\s*(total\\b|s/?total\\b|t\\.?v\\.?a\\b|vat\\b|droit\\s+de\\s+timbre|timbre\\b|remise\\s+globale|escompte\\b|acompte\\b|versement\\b|solde\\b|ancien\\s+solde|nouveau\\s+solde|arrete\\b|arretee\\b|mode\\s+de\\s+(reglement|paiement)|conditions?\\s+de|cachet\\b|signature\\b|visa\\b|le\\s+fournisseur|le\\s+client)").containsMatchIn(norm)) {
            return true
        }

        if (norm.contains("arrete la presente") ||
            norm.contains("arretee la presente") ||
            norm.contains("en toutes lettres") ||
            Regex("\\bt\\.?v\\.?a\\s*(19|9)\\s*%").containsMatchIn(norm)
        ) {
            return true
        }

        return false
    }

    /**
     * Détecte toute ligne d'en-tête de document, coordonnées fiscales/bancaires ou mention non-article.
     */
    fun isNonProductMetadataRow(row: String): Boolean {
        if (isTableHeaderRow(row) || isTableFooterOrTotalRow(row)) return true

        val norm = row.lowercase(Locale.FRENCH)
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('à', 'a')

        if (Regex("\\b(r\\.?c\\s*[:n°]|n\\.?i\\.?f\\b|n\\.?i\\.?s\\b|n\\.?i\\.?n\\b|article\\s+d'?imposition|n[°o]?\\s*article|r\\.?i\\.?b\\b|iban\\b|banque\\b|compte\\s+bancaire|tel\\b|telephone|fax\\b|email\\b|e-mail|site\\s+web|capital\\s+social)\\b").containsMatchIn(norm)) {
            return true
        }

        if (Regex("^\\s*(facture|invoice|bon\\s+de\\s+livraison|bon\\s+de\\s+commande|bon\\s+de\\s+reception|devis|proforma|avoir|releve|client\\s*:|fournisseur\\s*:|doit\\s*:|destinataire\\s*:|partenaire\\s*:|adresse\\s*:|date\\s*:|edite\\s+le|bejaia\\s+le|alger\\s+le|page\\s+\\d)").containsMatchIn(norm)) {
            return true
        }

        return false
    }

    /**
     * Analyse la ligne d'en-tête du tableau pour déterminer l'ordre des colonnes :
     * true = Colonne Qté avant Colonne Prix Unitaire (ex: Qté | P.U. | Montant)
     * false = Colonne Prix Unitaire avant Colonne Qté (ex: P.U. | Qté | Montant)
     * null = Non détecté avec certitude
     */
    fun detectHeaderColumnOrder(headerRows: List<String>): Boolean? {
        for (headerRow in headerRows) {
            val norm = headerRow.lowercase(Locale.FRENCH)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
            val qteMatch = Regex("(?i)\\b(qt[eé]s?|quantit[eé]s?|qte\\.?|qty|quantity|nbre|quant|الكمية)\\b").find(norm)
            val puMatch = Regex("(?i)\\b(p\\.?u(?:\\.?h\\.?t)?|prix\\s*(?:unit(?:aire)?)?|unit\\s*price|unit(?:aire)?|tarif|puht|p\\.u|السعر)\\b").find(norm)
            if (qteMatch != null && puMatch != null) {
                return qteMatch.range.first < puMatch.range.first
            }
        }
        return null
    }

    /**
     * Reconstruit les prix décimaux dont les centimes ont été collés au taux de TVA (ex: "361,13" + "19" -> 361 et 1319)
     * ou séparés par erreur par l'OCR.
     */
    fun normalizeSplitDecimalAndTvaTokens(rawNumbers: List<Double>): List<Double> {
        if (rawNumbers.size < 2) return rawNumbers
        val out = mutableListOf<Double>()
        var i = 0
        while (i < rawNumbers.size) {
            val cur = rawNumbers[i]
            val next = rawNumbers.getOrNull(i + 1)
            if (next != null &&
                cur >= 1.0 &&
                cur % 1.0 == 0.0 &&
                next >= 100.0 &&
                next <= 9999.0 &&
                next % 1.0 == 0.0
            ) {
                val nextInt = next.toInt()
                val lastTwoDigits = nextInt % 100
                val cents = nextInt / 100
                if ((lastTwoDigits == 19 || lastTwoDigits == 9) && cents in 0..99) {
                    val reconstructedPrice = round((cur + cents / 100.0) * 100.0) / 100.0
                    out.add(reconstructedPrice)
                    out.add(lastTwoDigits.toDouble())
                    i += 2
                    continue
                }
            }
            out.add(cur)
            i++
        }
        return out
    }

    /**
     * Détermine sans ambiguïté la Quantité et le Prix Unitaire à partir des nombres reconnus sur la ligne.
     * Garantit que le prix va strictement dans le prix unitaire et la quantité dans la quantité (jamais l'inverse).
     */
    fun resolveQtyAndPrice(
        candidateNumbers: List<Double>,
        headerHasQteBeforePu: Boolean? = null,
        catalogPriceHint: Double? = null,
        strictColumnOrder: Boolean = false
    ): Pair<Double, Double> {
        val normalized = normalizeSplitDecimalAndTvaTokens(candidateNumbers)
        if (normalized.isEmpty()) return 1.0 to 0.0
        if (normalized.size == 1) {
            val single = normalized[0]
            return 1.0 to single
        }

        // 1. Recherche directe d'un couple (facteurA, facteurB) et d'un total T = facteurA × facteurB parmi toutes les colonnes numériques
        if (normalized.size >= 3) {
            for (k in normalized.lastIndex downTo 2) {
                val totalCandidate = normalized[k]
                if (totalCandidate <= 0.0) continue
                for (i in 0 until k) {
                    for (j in i + 1 until k) {
                        val a = normalized[i]
                        val b = normalized[j]
                        if (a > 0.0 && b > 0.0 && abs(a * b - totalCandidate) <= max(2.0, totalCandidate * 0.02)) {
                            return chooseQtyAndPriceFromFactors(
                                factorA = a,
                                factorB = b,
                                headerHasQteBeforePu = headerHasQteBeforePu,
                                catalogPriceHint = catalogPriceHint,
                                strictColumnOrder = strictColumnOrder
                            )
                        }
                    }
                }
            }

            // 2. Cas où la partie décimale du PU HT a été altérée mais Total HT et Qté sont présents (ex: Qté=309, PU_int=361, Total=111589.17 -> 111589.17 / 309 = 361.13)
            val lastTotal = normalized.last()
            if (lastTotal > 10.0) {
                for (i in 0 until normalized.lastIndex) {
                    val qCandidate = normalized[i]
                    if (qCandidate >= 1.0 && qCandidate % 1.0 == 0.0 && qCandidate < lastTotal) {
                        val impliedPu = round((lastTotal / qCandidate) * 100.0) / 100.0
                        val hasMatchingIntegerPart = (0 until normalized.lastIndex).any { idx ->
                            idx != i && abs(kotlin.math.floor(impliedPu) - normalized[idx]) < 0.01
                        }
                        if (hasMatchingIntegerPart && impliedPu > 0.0) {
                            return qCandidate to impliedPu
                        }
                    }
                }

                // 3. Cas où la colonne Quantité (ex: 309) a été omise par l'OCR mais PU HT (ex: 361.13) et Montant HT (ex: 111589.17) sont présents
                for (i in 0 until normalized.lastIndex) {
                    val puCandidate = normalized[i]
                    if (puCandidate > 0.0 && puCandidate != 19.0 && puCandidate != 9.0 && lastTotal > puCandidate) {
                        val impliedQty = lastTotal / puCandidate
                        val roundedQty = round(impliedQty)
                        if (roundedQty in 2.0..10000.0 && abs(impliedQty - roundedQty) <= 0.015) {
                            return roundedQty to puCandidate
                        }
                    }
                }
            }

            // 4. Filtrer un éventuel taux de TVA (19% ou 9%) intercalé entre PU HT et Montant HT ou en fin de ligne
            val withoutVat = if (normalized.size >= 3) {
                normalized.filterIndexed { idx, v ->
                    val isVatToken = (v == 19.0 || v == 9.0) && idx >= 1 && normalized.size > 2
                    !isVatToken
                }
            } else {
                normalized
            }

            val working = if (withoutVat.size >= 2) withoutVat else normalized
            if (working.size >= 3) {
                val n1 = working[0]
                val n2 = working[1]
                val n3 = working.last()

                val n3IsTotal = n1 > 0 && n2 > 0 && n3 > 0 && abs(n1 * n2 - n3) <= max(2.0, n3 * 0.05)
                val n2IsTotal = n1 > 0 && n2 > 0 && n3 > 0 && abs(n1 * n3 - n2) <= max(2.0, n2 * 0.05)

                val (factorA, factorB) = when {
                    n2IsTotal -> n1 to n3
                    n3IsTotal -> n1 to n2
                    abs(n2 - n3) < 0.05 && n2 > 0.0 -> {
                        val q = if (n1 in 1.0..500.0 && n1 % 1.0 == 0.0) n1 else 1.0
                        return q to n2
                    }
                    else -> n1 to n2
                }

                return chooseQtyAndPriceFromFactors(
                    factorA = factorA,
                    factorB = factorB,
                    headerHasQteBeforePu = headerHasQteBeforePu,
                    catalogPriceHint = catalogPriceHint,
                    strictColumnOrder = strictColumnOrder
                )
            } else if (working.size == 2) {
                return resolveQtyAndPrice(working, headerHasQteBeforePu, catalogPriceHint, strictColumnOrder)
            }
        }

        val n1 = normalized[0]
        val n2 = normalized[1]

        if (abs(n1 - n2) < 0.05 && n1 > 0.0) {
            return 1.0 to n1
        }

        if (!strictColumnOrder) {
            if (n1 > 0.0 && n2 > n1) {
                val ratio = n2 / n1
                if (abs(ratio - round(ratio)) < 0.025 && round(ratio) in 2.0..5000.0) {
                    return round(ratio) to n1
                }
            } else if (n2 > 0.0 && n1 > n2) {
                val ratio = n1 / n2
                if (abs(ratio - round(ratio)) < 0.025 && round(ratio) in 2.0..5000.0) {
                    return round(ratio) to n2
                }
            }
        }

        return chooseQtyAndPriceFromFactors(
            factorA = n1,
            factorB = n2,
            headerHasQteBeforePu = headerHasQteBeforePu,
            catalogPriceHint = catalogPriceHint,
            strictColumnOrder = strictColumnOrder
        )
    }

    private fun chooseQtyAndPriceFromFactors(
        factorA: Double,
        factorB: Double,
        headerHasQteBeforePu: Boolean?,
        catalogPriceHint: Double?,
        strictColumnOrder: Boolean = false
    ): Pair<Double, Double> {
        val a = factorA.coerceAtLeast(0.0)
        val b = factorB.coerceAtLeast(0.0)

        if (strictColumnOrder && headerHasQteBeforePu != null) {
            return if (headerHasQteBeforePu) {
                (if (a > 0.0) a else 1.0) to b
            } else {
                (if (b > 0.0) b else 1.0) to a
            }
        }

        if (catalogPriceHint != null && catalogPriceHint > 0.0) {
            val diffA = abs(a - catalogPriceHint) / catalogPriceHint
            val diffB = abs(b - catalogPriceHint) / catalogPriceHint
            if (diffA < diffB && diffA < 0.35) {
                return (if (b > 0.0) b else 1.0) to a
            } else if (diffB < diffA && diffB < 0.35) {
                return (if (a > 0.0) a else 1.0) to b
            }
        }

        val aHasDecimals = abs(a - round(a)) > 0.001
        val bHasDecimals = abs(b - round(b)) > 0.001
        // Si l'un est un entier (ex: 309) et l'autre a des centimes (ex: 361,13), l'entier est la quantité et le décimal est le PU HT
        if (!aHasDecimals && bHasDecimals && a >= 1.0) {
            return a to b
        }
        if (aHasDecimals && !bHasDecimals && b >= 1.0) {
            return b to a
        }

        if (headerHasQteBeforePu != null) {
            if (headerHasQteBeforePu) {
                if (a > b && a >= 50.0 && b in 1.0..100.0 && b % 1.0 == 0.0 && a > b * 8.0) {
                    return b to a
                }
                return (if (a > 0.0) a else 1.0) to b
            } else {
                if (b > a && b >= 50.0 && a in 1.0..100.0 && a % 1.0 == 0.0 && b > a * 8.0) {
                    return a to b
                }
                return (if (b > 0.0) b else 1.0) to a
            }
        }

        return if (a > b) {
            if (b in 0.01..500.0 && a >= 10.0) {
                (if (b > 0.0) b else 1.0) to a
            } else {
                (if (a > 0.0) a else 1.0) to b
            }
        } else {
            if (a in 0.01..500.0 && b >= 10.0) {
                (if (a > 0.0) a else 1.0) to b
            } else {
                (if (a > 0.0) a else 1.0) to b
            }
        }
    }

    /**
     * Extrait uniquement les lignes d'articles (Référence, Désignation, Quantité, Prix Unitaire)
     * en regroupant automatiquement les désignations sur plusieurs lignes.
     */
    fun extractProductLinesFromRows(
        rows: List<String>,
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): List<OcrExtractedLine> {
        return extractProductLinesFromRowsWithBoxes(
            rowsWithBoxes = rows.map { ReconstructedRowWithBox(it, null) },
            catalogProducts = catalogProducts,
            columnConfig = columnConfig
        )
    }

    fun extractProductLinesFromRowsWithBoxes(
        rowsWithBoxes: List<ReconstructedRowWithBox>,
        catalogProducts: List<ProductCatalogEntity> = emptyList(),
        defaultTvaRate: Double = 19.0,
        preferredQteBeforePu: Boolean? = null,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): List<OcrExtractedLine> {
        val rows = rowsWithBoxes.map { it.text }
        val headerRowIndex = rows.indexOfFirst { isTableHeaderRow(it) }

        val headerOrder = if (!columnConfig.isAuto) {
            columnConfig.qteBeforePu
        } else if (headerRowIndex >= 0) {
            val headerSlice = rows.slice(headerRowIndex..minOf(headerRowIndex + 2, rows.lastIndex))
            detectHeaderColumnOrder(headerSlice) ?: preferredQteBeforePu
        } else {
            preferredQteBeforePu
        }

        // Passe 1 : Lignes situées après l'en-tête du tableau (ou toutes les lignes si pas d'en-tête), multi-nombres requis
        val primaryCandidates = if (headerRowIndex >= 0) {
            rowsWithBoxes.drop(headerRowIndex + 1)
        } else {
            rowsWithBoxes
        }
        val pass1 = runExtractionPass(
            candidateRows = primaryCandidates,
            catalogProducts = catalogProducts,
            insideTable = true,
            requireMultipleNumbers = true,
            headerOrder = headerOrder,
            defaultTvaRate = defaultTvaRate,
            columnConfig = columnConfig
        )
        if (pass1.isNotEmpty()) return pass1

        // Passe 2 : Si un faux en-tête a été détecté trop bas, analyser toutes les lignes depuis le début
        if (headerRowIndex > 0) {
            val pass2 = runExtractionPass(
                candidateRows = rowsWithBoxes,
                catalogProducts = catalogProducts,
                insideTable = false,
                requireMultipleNumbers = true,
                headerOrder = headerOrder,
                defaultTvaRate = defaultTvaRate,
                columnConfig = columnConfig
            )
            if (pass2.isNotEmpty()) return pass2
        }

        // Passe 3 : Bons de livraison ou documents avec une seule colonne numérique (ex: Désignation + Quantité ou Désignation + Prix)
        val pass3 = runExtractionPass(
            candidateRows = primaryCandidates,
            catalogProducts = catalogProducts,
            insideTable = true,
            requireMultipleNumbers = false,
            headerOrder = headerOrder,
            defaultTvaRate = defaultTvaRate,
            columnConfig = columnConfig
        )
        if (pass3.isNotEmpty()) return pass3

        // Passe 4 : Secours global sur toutes les lignes avec un seul nombre
        if (headerRowIndex > 0) {
            val pass4 = runExtractionPass(
                candidateRows = rowsWithBoxes,
                catalogProducts = catalogProducts,
                insideTable = false,
                requireMultipleNumbers = false,
                headerOrder = headerOrder,
                defaultTvaRate = defaultTvaRate,
                columnConfig = columnConfig
            )
            if (pass4.isNotEmpty()) return pass4
        }

        return emptyList()
    }

    private fun mergeBoundingBoxes(
        boxA: NormalizedBoundingBox?,
        boxB: NormalizedBoundingBox?
    ): NormalizedBoundingBox? {
        return if (boxA != null && boxB != null) {
            NormalizedBoundingBox(
                left = minOf(boxA.left, boxB.left),
                top = minOf(boxA.top, boxB.top),
                right = maxOf(boxA.right, boxB.right),
                bottom = maxOf(boxA.bottom, boxB.bottom),
                pageIndex = boxA.pageIndex
            )
        } else {
            boxA ?: boxB
        }
    }

    /**
     * Détecte si une ligne contient uniquement les colonnes chiffrées d'un article (ex: "10   450.00   4500.00" ou "5   1200 DA")
     * suite à une légère inclinaison de la photo ayant séparé la désignation (à gauche) et les montants (à droite).
     */
    private fun isPurelyNumericColumnsRow(row: String): Boolean {
        val clean = row.trim()
        if (clean.isEmpty() || clean.length > 65) return false
        if (isTableHeaderRow(clean) || isTableFooterOrTotalRow(clean) || isNonProductMetadataRow(clean)) return false
        // Ignorer les dates isolées
        if (Regex("\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}\\b").containsMatchIn(clean)) return false
        val strippedUnits = clean.replace(Regex("(?i)\\b(da|dzd|ht|ttc|u|pce|pcs|m|ml|kg)\\b"), "").trim()
        val letterCount = strippedUnits.count { it.isLetter() }
        if (letterCount > 2) return false
        val numbers = extractNumbersFromText(clean)
        return numbers.isNotEmpty()
    }

    private fun runExtractionPass(
        candidateRows: List<ReconstructedRowWithBox>,
        catalogProducts: List<ProductCatalogEntity>,
        insideTable: Boolean,
        requireMultipleNumbers: Boolean,
        headerOrder: Boolean?,
        defaultTvaRate: Double,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): List<OcrExtractedLine> {
        val results = mutableListOf<OcrExtractedLine>()
        var i = 0
        while (i < candidateRows.size) {
            val item = candidateRows[i]
            val row = item.text.trim()
            if (row.length < 2) {
                i++
                continue
            }

            if (isTableHeaderRow(row)) {
                i++
                continue
            }

            if (isTableFooterOrTotalRow(row)) {
                if (results.isNotEmpty()) {
                    break
                } else {
                    i++
                    continue
                }
            }

            if (isNonProductMetadataRow(row)) {
                i++
                continue
            }

            val parsedLine = tryParseProductRow(
                row = row,
                catalogProducts = catalogProducts,
                insideTable = insideTable,
                requireMultipleNumbers = requireMultipleNumbers,
                headerHasQteBeforePu = headerOrder,
                boundingBox = item.boundingBox,
                defaultTvaRate = defaultTvaRate,
                columnConfig = columnConfig
            )
            if (parsedLine != null) {
                results.add(parsedLine)
                i++
                continue
            }

            // Cas d'une photo légèrement inclinée : Ligne i = Désignation seule, Ligne i+1 = Colonnes numériques (Qté / PU / Total)
            if (i + 1 < candidateRows.size) {
                val nextItem = candidateRows[i + 1]
                val nextRow = nextItem.text.trim()
                if (row.count { it.isLetter() } >= 3 && isPurelyNumericColumnsRow(nextRow)) {
                    val combinedText = "$row   $nextRow"
                    val combinedBox = mergeBoundingBoxes(item.boundingBox, nextItem.boundingBox)
                    val pairedParsed = tryParseProductRow(
                        row = combinedText,
                        catalogProducts = catalogProducts,
                        insideTable = insideTable,
                        requireMultipleNumbers = requireMultipleNumbers,
                        headerHasQteBeforePu = headerOrder,
                        boundingBox = combinedBox,
                        defaultTvaRate = defaultTvaRate,
                        columnConfig = columnConfig
                    )
                    if (pairedParsed != null) {
                        results.add(pairedParsed)
                        i += 2
                        continue
                    }
                }
            }

            if (results.isNotEmpty() && isMultiLineDesignationContinuation(row)) {
                // Regrouper correctement une désignation qui s'étend sur plusieurs lignes
                val prev = results.removeAt(results.lastIndex)
                val mergedDes = "${prev.designation} ${row.trim()}".replace(Regex("\\s{2,}"), " ").trim()
                val mergedBox = mergeBoundingBoxes(prev.boundingBox, item.boundingBox)
                results.add(prev.copy(designation = mergedDes, boundingBox = mergedBox))
            }

            i++
        }
        return results
    }

    /**
     * Détecte si une ligne sans colonnes numériques est la suite de la désignation de l'article précédent.
     */
    private fun isMultiLineDesignationContinuation(row: String): Boolean {
        val clean = row.trim()
        if (clean.length !in 3..100) return false
        if (isNonProductMetadataRow(clean) || isTableHeaderRow(clean) || isTableFooterOrTotalRow(clean)) return false
        val letterCount = clean.count { it.isLetter() }
        if (letterCount < 3) return false
        val standaloneNumberRegex = Regex("(?<![A-Za-z0-9\\-/.,])(\\d{1,3}(?:[\\s.]\\d{3})+(?:[,.]\\d{1,2})?|\\d+(?:[,.]\\d{1,3})?)(?![A-Za-z0-9\\-/.,])")
        val standaloneNumbers = standaloneNumberRegex.findAll(clean).toList()
        // Une ligne de continuation de désignation a au plus 1 nombre isolé (les codes comme 32-201 ou 30mA ne sont pas des colonnes de prix)
        return standaloneNumbers.size <= 1
    }

    private fun normalizeRowForNumberExtraction(rawRow: String): String {
        return rawRow
            .replace(Regex("(?<=\\d)[OoQD](?=\\d)"), "0")
            .replace(Regex("(?<=\\d)[lI|](?=\\d)"), "1")
            // Séparer un prix décimal (ex: 361,13) collé par l'OCR au taux de TVA (19 ou 09) : "361,1319" -> "361,13   19%"
            .replace(Regex("(\\d+)\\s*([,.])\\s*(\\d{2})(19|09|00)(?!\\d)"), "$1$2$3   $4%")
            // Supprimer les espaces accidentels autour de la virgule décimale : "361, 13" -> "361,13"
            .replace(Regex("(\\d+)\\s*,\\s*(\\d{2})(?!\\d)"), "$1,$2")
            .replace(Regex("(?i)\\b(DA|DZD|EUR|HT|TTC)\\b"), " ")
    }

    /**
     * Tente d'extraire (Référence, Désignation, Quantité, Prix Unitaire, Remise, TVA, Total HT) d'une ligne horizontale OCR.
     * Prend en charge :
     * - Quantité avant désignation (ex: "10   CAB-125   Câble 3G2.5   250.00   2500.00" ou "10   CAB-125   Câble 3G2.5   250.00")
     * - Référence avant quantité (ex: "CAB-125   100   Câble électrique 3G2.5   250.00   25000.00")
     * - Désignation avant référence (ex: "Câble électrique 3G2.5   CAB-125   100   250.00   25000.00")
     * - Standard avec ou sans colonne TVA/Remise : Réf | Désignation | Qté | PU HT | [TVA] | Total HT
     */
    fun tryParseProductRow(
        row: String,
        catalogProducts: List<ProductCatalogEntity>,
        insideTable: Boolean = true,
        requireMultipleNumbers: Boolean = false,
        headerHasQteBeforePu: Boolean? = null,
        boundingBox: NormalizedBoundingBox? = null,
        defaultTvaRate: Double = 19.0,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): OcrExtractedLine? {
        if (isNonProductMetadataRow(row)) return null

        var preExtractedTrailingRef = ""
        val cleanedForNumbers = normalizeRowForNumberExtraction(row).let { normRow ->
            if (!columnConfig.isAuto && columnConfig.refAfterNumbers) {
                val trailingSkuMatch = Regex("^(.+?\\d)\\s+([A-Z0-9][A-Z0-9\\-_./]{1,14})\\s*$").find(normRow.trim())
                val candidateSku = trailingSkuMatch?.groupValues?.getOrNull(2).orEmpty()
                val isUnitWord = Regex("^(?i)(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ|DA|DZD|HT|TTC)$").matches(candidateSku)
                if (trailingSkuMatch != null && !isUnitWord && candidateSku.any { it.isLetter() } && candidateSku.any { it.isDigit() }) {
                    preExtractedTrailingRef = candidateSku.trim()
                    trailingSkuMatch.groupValues[1].trim()
                } else {
                    normRow
                }
            } else {
                normRow
            }
        }

        // Ne fusionner les milliers séparés par un espace QUE s'il s'agit d'un seul espace (et non d'un séparateur de colonnes "   ")
        val numberRegex = Regex("(?<![A-Za-z0-9\\-/.,])(\\d{1,3}(?:[ .]\\d{3})+(?:[,.]\\d{1,2})?|\\d+(?:[,.]\\d{1,3})?)(?![A-Za-z0-9\\-/.,])")
        val atomicNumberRegex = Regex("(?<![A-Za-z0-9\\-/.,])(\\d+(?:[,.]\\d{1,3})?)(?![A-Za-z0-9\\-/.,])")
        val allMatches = numberRegex.findAll(cleanedForNumbers).toList()
        if (allMatches.isEmpty()) return null

        val explicitQteFirst = !columnConfig.isAuto && columnConfig.qteBeforeDesignation && !columnConfig.puBeforeDesignation && columnConfig.qteBeforeRef
        val explicitPuFirst = !columnConfig.isAuto && columnConfig.puBeforeDesignation && !columnConfig.qteBeforeDesignation
        val explicitBothNumbersBeforeDes = !columnConfig.isAuto && columnConfig.qteBeforeDesignation && columnConfig.puBeforeDesignation
        val explicitRefThenQte = !columnConfig.isAuto && columnConfig.qteBeforeDesignation && !columnConfig.qteBeforeRef && columnConfig.includeReference

        // Cas 0 : Structure où Qts ET P.U.HT sont placés AVANT la Désignation (ex: "10   450,00   Disjoncteur 20A")
        if (explicitBothNumbersBeforeDes && allMatches.size >= 2) {
            val m0 = allMatches[0]
            val m1 = allMatches[1]
            val val0 = parseFrenchDouble(m0.value)
            val val1 = parseFrenchDouble(m1.value)
            val noLettersBeforeM1 = cleanedForNumbers.substring(0, m1.range.first).none { it.isLetter() }
            if (val0 != null && val1 != null && noLettersBeforeM1 && m1.range.last + 1 < cleanedForNumbers.length) {
                val rawAfter = cleanedForNumbers.substring(m1.range.last + 1).trim()
                if (rawAfter.count { it.isLetter() } >= 2 && !isNonProductMetadataRow(rawAfter)) {
                    val (finalRef, cleanDes) = extractRefAndCleanDesignation(rawAfter, columnConfig)
                    if (cleanDes.count { it.isLetter() } >= 2) {
                        val (qty, puVal) = if (columnConfig.qteBeforePu) (val0 to val1) else (val1 to val0)
                        val matchedCatalog = findMatchingCatalogProduct(finalRef, cleanDes, catalogProducts)
                        return OcrExtractedLine(
                            pageIndex = boundingBox?.pageIndex ?: 0,
                            reference = preExtractedTrailingRef.ifBlank { finalRef.ifBlank { if (columnConfig.includeReference) matchedCatalog?.reference.orEmpty() else "" } },
                            designation = matchedCatalog?.name ?: cleanDes,
                            quantite = if (qty > 0.0) qty else 1.0,
                            prixUnitaire = if (puVal > 0.0) puVal else (matchedCatalog?.prixAchat ?: matchedCatalog?.defaultUnitPrice ?: 0.0),
                            tauxTva = defaultTvaRate,
                            confidence = OcrConfidenceLevel.MEDIUM,
                            confidenceNote = "Ordre appliqué : ${columnConfig.summaryLabel()}",
                            boundingBox = boundingBox,
                            selected = true,
                            matchedCatalogProductName = matchedCatalog?.name
                        )
                    }
                }
            }
        }

        // Cas 1 : Structure "Qts (ou P.U.HT) | [Réf] | Désignation | P.U.HT (ou Qts) | [TVA] | [Montant HT]"
        // Actif automatiquement si Qté × PU = Montant HT, OU explicitement lorsque l'utilisateur a mis Qts (ou P.U.HT) en premier
        if (allMatches.size >= 2 && (explicitQteFirst || explicitPuFirst || (columnConfig.isAuto && allMatches.size >= 3))) {
            val m0 = allMatches[0]
            val val0 = parseFrenchDouble(m0.value)
            val noLettersBeforeM0 = cleanedForNumbers.substring(0, m0.range.first).none { it.isLetter() }
            if (val0 != null && noLettersBeforeM0 && m0.range.first <= 6 && val0 in 0.01..50000.0) {
                val trailingAfterDes = mutableListOf<MatchResult>()
                for (idx in allMatches.indices.reversed()) {
                    if (idx == 0) break
                    val m = allMatches[idx]
                    if (trailingAfterDes.isEmpty()) {
                        val afterLast = cleanedForNumbers.substring(m.range.last + 1)
                            .replace(Regex("(?i)\\b(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ)\\b"), "")
                        if (afterLast.any { it.isLetter() }) continue
                        trailingAfterDes.add(0, m)
                    } else {
                        val nextM = trailingAfterDes.first()
                        val between = cleanedForNumbers.substring(m.range.last + 1, nextM.range.first)
                            .replace(Regex("(?i)\\b(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ)\\b"), "")
                        if (between.any { it.isLetter() }) break
                        trailingAfterDes.add(0, m)
                    }
                }

                if (trailingAfterDes.isNotEmpty()) {
                    val firstTail = trailingAfterDes.first()
                    if (firstTail.range.first > m0.range.last + 1) {
                        val rawDes = cleanedForNumbers.substring(m0.range.last + 1, firstTail.range.first).trim()
                        val tailNums = normalizeSplitDecimalAndTvaTokens(
                            trailingAfterDes.mapNotNull { parseFrenchDouble(it.value) }
                        )
                        if (rawDes.count { it.isLetter() } >= 2 && !isNonProductMetadataRow(rawDes) && tailNums.isNotEmpty()) {
                            val lastNum = tailNums.last()
                            val matchedFactor = if (tailNums.size >= 2) {
                                tailNums.dropLast(1).firstOrNull { p ->
                                    p > 0.0 && abs(val0 * p - lastNum) <= max(2.0, lastNum * 0.05)
                                }
                            } else null

                            val isVerifiedMath = matchedFactor != null
                            if (isVerifiedMath || explicitQteFirst || explicitPuFirst) {
                                val otherVal = matchedFactor ?: tailNums.first()
                                val (resolvedQty, resolvedPu) = if (explicitPuFirst) {
                                    (if (otherVal > 0.0) otherVal else 1.0) to val0
                                } else {
                                    val0 to otherVal
                                }
                                val totVal = if (tailNums.size >= 2 && lastNum != 19.0 && lastNum != 9.0) lastNum else null
                                val (finalRef, cleanDes) = extractRefAndCleanDesignation(rawDes, columnConfig)
                                if (cleanDes.count { it.isLetter() } >= 2) {
                                    val matchedCatalog = findMatchingCatalogProduct(finalRef, cleanDes, catalogProducts)
                                    val finalPrice = if (resolvedPu > 0.0) resolvedPu else (matchedCatalog?.prixAchat ?: matchedCatalog?.defaultUnitPrice ?: 0.0)
                                    return OcrExtractedLine(
                                        pageIndex = boundingBox?.pageIndex ?: 0,
                                        reference = preExtractedTrailingRef.ifBlank { finalRef.ifBlank { if (columnConfig.includeReference) matchedCatalog?.reference.orEmpty() else "" } },
                                        designation = matchedCatalog?.name ?: cleanDes,
                                        quantite = resolvedQty,
                                        prixUnitaire = finalPrice,
                                        tauxTva = defaultTvaRate,
                                        extractedMontantHt = totVal,
                                        confidence = if (isVerifiedMath) OcrConfidenceLevel.HIGH else OcrConfidenceLevel.MEDIUM,
                                        confidenceNote = if (isVerifiedMath) "Vérifié (${columnConfig.summaryLabel()})" else "Ordre appliqué : ${columnConfig.summaryLabel()}",
                                        boundingBox = boundingBox,
                                        selected = true,
                                        matchedCatalogProductName = matchedCatalog?.name
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cas 2 : Structure "Réf | Qté | Désignation | P.U.HT | [Montant HT]"
        // Exemple : "CAB-125   100   Câble électrique 3G2.5   250.00   25000.00"
        if (columnConfig.isAuto || explicitRefThenQte) {
            val leadingRefAndQtyMatch = Regex("^\\s*([A-Z0-9][A-Z0-9\\-_./]{1,14})\\s+(\\d+(?:[,.]\\d{1,2})?)\\s+([A-Za-zÀ-ÿ].+)").find(cleanedForNumbers)
            if (leadingRefAndQtyMatch != null && allMatches.size >= 2) {
                val candidateRef = leadingRefAndQtyMatch.groupValues[1].trim()
                val candidateQty = parseFrenchDouble(leadingRefAndQtyMatch.groupValues[2])
                val restOfLine = leadingRefAndQtyMatch.groupValues[3]
                val restNumbers = numberRegex.findAll(restOfLine).toList()
                if (candidateRef.any { it.isDigit() } && candidateQty != null && candidateQty > 0.0 && restNumbers.isNotEmpty()) {
                    val tailNums = normalizeSplitDecimalAndTvaTokens(restNumbers.mapNotNull { parseFrenchDouble(it.value) })
                    val puVal = tailNums.firstOrNull()
                    val totVal = if (tailNums.size >= 2) tailNums.last() else null
                    val mathMatches = puVal != null && totVal != null && abs(candidateQty * puVal - totVal) <= max(2.0, totVal * 0.05)
                    if (puVal != null && (mathMatches || (explicitRefThenQte && restNumbers.isNotEmpty()))) {
                        val firstRestMatch = restNumbers.drop(maxOf(0, restNumbers.size - tailNums.size)).firstOrNull() ?: restNumbers.first()
                        val desOnly = restOfLine.substring(0, firstRestMatch.range.first).trim()
                        if (desOnly.count { it.isLetter() } >= 2 && !isNonProductMetadataRow(desOnly)) {
                            val matchedCatalog = findMatchingCatalogProduct(candidateRef, desOnly, catalogProducts)
                            return OcrExtractedLine(
                                pageIndex = boundingBox?.pageIndex ?: 0,
                                reference = if (columnConfig.includeReference) candidateRef else "",
                                designation = matchedCatalog?.name ?: desOnly,
                                quantite = candidateQty,
                                prixUnitaire = puVal,
                                tauxTva = defaultTvaRate,
                                extractedMontantHt = totVal,
                                confidence = if (mathMatches) OcrConfidenceLevel.HIGH else OcrConfidenceLevel.MEDIUM,
                                confidenceNote = "Vérifié (Réf → Quantité → Désignation → P.U.HT)",
                                boundingBox = boundingBox,
                                selected = true,
                                matchedCatalogProductName = matchedCatalog?.name
                            )
                        }
                    }
                }
            }
        }

        // Cas standard : Extraire TOUTES les colonnes numériques situées après le dernier mot de la désignation
        // (évite de tronquer la colonne Qté lorsqu'il y a 4 ou 5 colonnes numériques : Qté | PU HT | TVA | Montant HT)
        val firstMatch = allMatches.first()
        val hasLeadingRowIndex = firstMatch.range.first <= 2 && allMatches.size >= 3 &&
            (parseFrenchDouble(firstMatch.value)?.let { it in 1.0..200.0 && it % 1.0 == 0.0 } == true)

        val candidateMatches = if (hasLeadingRowIndex) allMatches.drop(1) else allMatches
        if (candidateMatches.isEmpty()) return null

        val tailMatches = mutableListOf<MatchResult>()
        for (idx in candidateMatches.indices.reversed()) {
            val m = candidateMatches[idx]
            if (tailMatches.isEmpty()) {
                val afterLast = cleanedForNumbers.substring(m.range.last + 1)
                    .replace(Regex("(?i)\\b(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ)\\b"), "")
                if (afterLast.any { it.isLetter() }) {
                    // Ce nombre est suivi de texte de désignation, ce n'est pas une colonne de fin de ligne
                    continue
                }
                tailMatches.add(0, m)
            } else {
                val nextMatch = tailMatches.first()
                val between = cleanedForNumbers.substring(m.range.last + 1, nextMatch.range.first)
                    .replace(Regex("(?i)\\b(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ)\\b"), "")
                if (between.any { it.isLetter() }) {
                    // On a atteint le texte de la désignation (ex: "24" dans "Hublot 24 W Round")
                    break
                }
                tailMatches.add(0, m)
            }
        }

        if (tailMatches.isEmpty()) return null
        val firstTailMatch = tailMatches.first()

        val textBeforeNumbers = cleanedForNumbers.substring(0, firstTailMatch.range.first)
            .let { rawPrefix ->
                // Si l'utilisateur a choisi explicitement une colonne Réf en début de ligne (ex: Réf numérique), on ne supprime que les numéros d'ordre "1." / "1)"
                if (!columnConfig.isAuto && columnConfig.includeReference && columnConfig.refBeforeDesignation) {
                    rawPrefix.replace(Regex("^\\s*\\d{1,2}[.)]\\s+"), "")
                } else {
                    rawPrefix.replace(Regex("^\\s*\\d{1,3}[.)\\-\\s]+"), "")
                }
            }
            .replace(Regex("\\b(?i)(U|PCS|PC|PCE|ML|M|KG|LOT|BOITE|ROULEAU|UNITE|UNITÉ)\\b\\s*$"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        if (textBeforeNumbers.count { it.isLetter() } < 2) return null
        if (isNonProductMetadataRow(textBeforeNumbers)) return null

        val normDes = textBeforeNumbers.lowercase(Locale.FRENCH)
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('à', 'a')
        if (Regex("^\\s*(total|sous[- ]?total|montant|t\\.?v\\.?a|timbre|remise|net\\s+a|solde|ref|reference|designation|qte|quantite|prix|date|facture|bon|devis|client|fournisseur|adresse|tel|rc|nif|nis|rib)\\b").containsMatchIn(normDes)) {
            return null
        }
        if (!insideTable && Regex("\\b(facture|devis|livraison|client|fournisseur|alger|bejaia|oran|constantine|date|page)\\b").containsMatchIn(normDes)) {
            return null
        }

        // Si un espace simple entre Qté (ex: 309) et PU HT (ex: 361,13) a fusionné les deux en un seul nombre de milliers (309 361,13),
        // vérifier si le découpage atomique permet de satisfaire Qté × PU = Total
        val tailSubstring = cleanedForNumbers.substring(firstTailMatch.range.first)
        val groupedNums = normalizeSplitDecimalAndTvaTokens(tailMatches.mapNotNull { parseFrenchDouble(it.value) })
        val atomicRaw = atomicNumberRegex.findAll(tailSubstring).mapNotNull { parseFrenchDouble(it.value) }.toList()
        val nums = if (groupedNums.size < atomicRaw.size && atomicRaw.size >= 3) {
            // Tester la recomposition uniquement du dernier montant (ex: "111" + "589,17" -> 111589.17) tout en gardant Qté et PU séparés
            val lastTwoMerged = if (atomicRaw.size >= 4) {
                val secondLast = atomicRaw[atomicRaw.size - 2]
                val last = atomicRaw.last()
                if (secondLast in 1.0..999.0 && secondLast % 1.0 == 0.0 && last < 1000.0) {
                    atomicRaw.dropLast(2) + (secondLast * 1000.0 + last)
                } else atomicRaw
            } else atomicRaw
            val normCandidate = normalizeSplitDecimalAndTvaTokens(lastTwoMerged)
            val candidateTotal = normCandidate.lastOrNull() ?: 0.0
            val hasExactPair = normCandidate.size >= 3 && (0 until normCandidate.lastIndex).any { i ->
                (i + 1 until normCandidate.lastIndex).any { j ->
                    abs(normCandidate[i] * normCandidate[j] - candidateTotal) <= max(2.0, candidateTotal * 0.02)
                }
            }
            if (hasExactPair) normCandidate else groupedNums
        } else {
            groupedNums
        }

        if (nums.isEmpty()) return null
        if (requireMultipleNumbers && nums.size < 2) return null

        val (extractedRef, cleanDesignation) = extractRefAndCleanDesignation(textBeforeNumbers, columnConfig)
        if (cleanDesignation.count { it.isLetter() } < 2 || isNonProductMetadataRow(cleanDesignation)) {
            return null
        }

        val matchedCatalog = findMatchingCatalogProduct(extractedRef, cleanDesignation, catalogProducts)
        val catalogPrice = matchedCatalog?.defaultUnitPrice ?: matchedCatalog?.prixAchat ?: matchedCatalog?.prixVente

        val effectiveQteBeforePu = if (!columnConfig.isAuto) columnConfig.qteBeforePu else headerHasQteBeforePu
        val (qty, unitPrice) = resolveQtyAndPrice(
            candidateNumbers = nums,
            headerHasQteBeforePu = effectiveQteBeforePu,
            catalogPriceHint = catalogPrice,
            strictColumnOrder = !columnConfig.isAuto
        )

        val finalRef = if (columnConfig.includeReference) {
            preExtractedTrailingRef.ifBlank { extractedRef.ifBlank { matchedCatalog?.reference.orEmpty() } }
        } else {
            ""
        }
        val finalPrice = if (unitPrice > 0.0) {
            unitPrice
        } else {
            catalogPrice ?: 0.0
        }

        val detectedLineVat = nums.drop(1).dropLast(1).firstOrNull { it == 19.0 || it == 9.0 } ?: defaultTvaRate
        val extractedTotalHt = if (nums.size >= 3) {
            val lastNum = nums.last()
            if (lastNum == 19.0 || lastNum == 9.0) null else lastNum
        } else null
        val mathVerified = extractedTotalHt != null && abs(qty * finalPrice - extractedTotalHt) <= max(2.0, extractedTotalHt * 0.05)
        val (confLevel, confNote) = when {
            mathVerified -> OcrConfidenceLevel.HIGH to "Vérifié (Qts × PU HT = Total HT)"
            extractedTotalHt != null && extractedTotalHt > 0.0 -> OcrConfidenceLevel.LOW to "Écart entre Qts × PU HT et Total lu (${CurrencyUtils.formatQty(extractedTotalHt)})"
            finalPrice > 0.0 -> OcrConfidenceLevel.MEDIUM to "Reconnaissance sur 2 colonnes numériques"
            else -> OcrConfidenceLevel.LOW to "Prix unitaire manquant ou égal à 0"
        }

        return OcrExtractedLine(
            pageIndex = boundingBox?.pageIndex ?: 0,
            reference = finalRef,
            designation = matchedCatalog?.name ?: cleanDesignation,
            quantite = if (qty > 0.0) qty else 1.0,
            prixUnitaire = finalPrice,
            tauxTva = detectedLineVat,
            extractedMontantHt = extractedTotalHt,
            confidence = confLevel,
            confidenceNote = confNote,
            boundingBox = boundingBox,
            selected = true,
            matchedCatalogProductName = matchedCatalog?.name
        )
    }

    /**
     * Extrait la référence et la désignation selon la configuration des colonnes choisie par l'utilisateur
     * (ex: "CAB-125   Câble électrique 3G2.5", "Disjoncteur 20A   DIS-020", ou sans colonne Référence).
     */
    fun extractRefAndCleanDesignation(
        rawText: String,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): Pair<String, String> {
        var cleanDesignation = rawText.trim()
        if (!columnConfig.includeReference) {
            return "" to cleanDesignation.trim(' ', '-', '|', ':', ';', '.')
        }

        var extractedRef = ""
        val bracketRefMatch = Regex("^\\[([^\\]]+)]\\s*(.+)").find(cleanDesignation)
        if (bracketRefMatch != null) {
            extractedRef = bracketRefMatch.groupValues[1].trim()
            cleanDesignation = bracketRefMatch.groupValues[2].trim()
        } else if (!columnConfig.isAuto && !columnConfig.refBeforeDesignation) {
            // L'utilisateur a placé la Référence APRÈS la Désignation (ex: Désignation / Réf / Quantité / P.U.HT)
            val trailingRefMatch = Regex("^(.+?[A-Za-zÀ-ÿ0-9])\\s+([A-Z0-9][A-Z0-9\\-_./]{1,14})$").find(cleanDesignation)
            if (trailingRefMatch != null && trailingRefMatch.groupValues[2].any { it.isDigit() }) {
                cleanDesignation = trailingRefMatch.groupValues[1].trim()
                extractedRef = trailingRefMatch.groupValues[2].trim()
            }
        } else {
            // Cas A : Référence au début ("CAB-125 Câble électrique 3G2.5" ou code SKU numérique quand Réf est choisie explicitement)
            val codeTokenMatch = Regex("^([A-Z0-9][A-Z0-9\\-_./]{1,14})\\s+([A-Za-zÀ-ÿ].+)").find(cleanDesignation)
            val token1 = codeTokenMatch?.groupValues?.getOrNull(1).orEmpty()
            val isSkuToken = token1.any { it.isDigit() } && (
                token1.any { it.isLetter() || it == '-' || it == '_' || it == '/' } ||
                    (!columnConfig.isAuto && columnConfig.refBeforeDesignation && token1.length >= 2)
                )
            if (codeTokenMatch != null && isSkuToken) {
                extractedRef = token1.trim()
                cleanDesignation = codeTokenMatch.groupValues[2].trim()
            } else {
                // Cas B : Désignation avant Référence ("Câble électrique 3G2.5 CAB-125" ou "Contacteur tripolaire 25A REF-CT25")
                val trailingRefMatch = Regex("^(.+?[A-Za-zÀ-ÿ0-9])\\s+([A-Z]{2,6}[\\-_][A-Z0-9\\-_./]{1,12})$").find(cleanDesignation)
                if (trailingRefMatch != null && trailingRefMatch.groupValues[2].any { it.isDigit() }) {
                    cleanDesignation = trailingRefMatch.groupValues[1].trim()
                    extractedRef = trailingRefMatch.groupValues[2].trim()
                }
            }
        }
        cleanDesignation = cleanDesignation.trim(' ', '-', '|', ':', ';', '.')
        return extractedRef to cleanDesignation
    }

    private fun findMatchingCatalogProduct(
        ref: String,
        designation: String,
        catalogProducts: List<ProductCatalogEntity>
    ): ProductCatalogEntity? {
        if (catalogProducts.isEmpty()) return null
        if (ref.isNotBlank()) {
            catalogProducts.find {
                it.reference.trim().equals(ref.trim(), ignoreCase = true) ||
                    it.barcode.trim().equals(ref.trim(), ignoreCase = true)
            }?.let { return it }
        }
        val cleanDes = designation.trim().lowercase(Locale.FRENCH)
        catalogProducts.find { it.name.trim().lowercase(Locale.FRENCH) == cleanDes }?.let { return it }
        return catalogProducts.find { prod ->
            val pName = prod.name.trim().lowercase(Locale.FRENCH)
            pName.length >= 5 && (cleanDes.contains(pName) || pName.contains(cleanDes))
        }
    }

    private fun extractNumbersFromText(text: String): List<Double> {
        val numberRegex = Regex("(\\d{1,3}(?:[\\s.]\\d{3})+(?:[,.]\\d{1,2})?|\\d+(?:[,.]\\d{1,2})?)")
        return numberRegex.findAll(text).mapNotNull { parseFrenchDouble(it.value) }.toList()
    }

    fun parseManualRowToLine(
        rawRow: String,
        catalogProducts: List<ProductCatalogEntity> = emptyList()
    ): OcrExtractedLine {
        val parsed = tryParseProductRow(
            row = rawRow,
            catalogProducts = catalogProducts,
            insideTable = true,
            requireMultipleNumbers = false
        )
        if (parsed != null) return parsed
        val (ref, des) = extractRefAndCleanDesignation(rawRow)
        val nums = extractNumbersFromText(rawRow)
        val (qty, pu) = when {
            nums.size >= 2 -> resolveQtyAndPrice(nums, headerHasQteBeforePu = true, catalogPriceHint = null)
            nums.size == 1 -> if (nums[0] >= 50.0) (1.0 to nums[0]) else (nums[0].coerceAtLeast(1.0) to 0.0)
            else -> 1.0 to 0.0
        }
        return OcrExtractedLine(
            reference = ref,
            designation = des.ifBlank { rawRow.trim() },
            quantite = qty,
            prixUnitaire = pu,
            confidence = OcrConfidenceLevel.MEDIUM,
            confidenceNote = "Ajouté manuellement depuis le texte brut",
            userVerified = true,
            selected = true
        )
    }

    fun parseFrenchDouble(raw: String): Double? {
        val cleaned = raw.trim()
            .replace(" ", "")
            .replace("\u00A0", "")
            .let { s ->
                if (s.contains('.') && s.contains(',')) {
                    if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                        s.replace(".", "").replace(',', '.')
                    } else {
                        s.replace(",", "")
                    }
                } else {
                    s.replace(',', '.')
                }
            }
        return cleaned.toDoubleOrNull()
    }
}
