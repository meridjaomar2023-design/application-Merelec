package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.ProductCatalogEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

private const val TAG = "IntelligentOcrEngine"
private const val PREFS_OCR_INTELLIGENT = "merelec_intelligent_ocr_prefs"
private const val KEY_PARTNER_PROFILES_JSON = "ocr_partner_profiles_v1"
private const val KEY_ARCHIVE_HISTORY_JSON = "ocr_archive_history_v1"

enum class OcrTransformationAction(val title: String, val subtitle: String) {
    KEEP_ORIGINAL(
        title = "A. Conserver le document original",
        subtitle = "Enregistrer la facture ou le bon fournisseur tel quel dans les Achats sans modifier son contenu commercial"
    ),
    TRANSFORM_TO_CLIENT(
        title = "B. Transformer en document client",
        subtitle = "Convertir directement en Facture Client, BL Client, Devis ou BC Client avec majoration et TVA"
    ),
    SAVE_AND_TRANSFORM(
        title = "C. Enregistrer et Transformer (Double opération liée)",
        subtitle = "Conserver le document fournisseur original dans Achats ET créer un document client lié dans Ventes"
    );

    val label: String get() = title
    val description: String get() = subtitle
}

data class OcrPartnerProfile(
    val partnerName: String,
    val preferredQteBeforePu: Boolean? = null,
    val columnOrderSummary: String = "Standard (Réf | Désignation | Qts | PU HT)",
    val defaultMarginPct: Double = 20.0,
    val usualTvaRate: Double = 19.0,
    val address: String = "",
    val phone: String = "",
    val rc: String = "",
    val nif: String = "",
    val nis: String = "",
    val articleImposition: String = "",
    val learnedDesignations: Map<String, String> = emptyMap(),
    val scanCount: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val scansCount: Int get() = scanCount
    val preferredColumnOrder: String get() = columnOrderSummary
    val defaultMajorationPct: Double get() = defaultMarginPct
    val defaultTvaRate: Double get() = usualTvaRate
    val learnedAliases: Map<String, String> get() = learnedDesignations
}

data class OcrArchiveRecord(
    val id: String = "OCR-ARC-${System.currentTimeMillis()}",
    val importedAt: Long = System.currentTimeMillis(),
    val supplierOrPartnerName: String,
    val targetClientName: String = "",
    val invoiceNumber: String = "",
    val bonNumber: String = "",
    val orderReference: String = "",
    val sourceDocumentType: OperationType = OperationType.FACTURE_FOURNISSEUR,
    val targetDocumentType: OperationType = OperationType.BL_CLIENT,
    val actionMode: OcrTransformationAction = OcrTransformationAction.KEEP_ORIGINAL,
    val totalHt: Double = 0.0,
    val totalTtc: Double = 0.0,
    val linesCount: Int = 0,
    val pageCount: Int = 1,
    val rawOcrText: String = "",
    val manualCorrectionsSummary: String = "",
    val savedImagePaths: List<String> = emptyList(),
    val linkedSupplierDocId: String = "",
    val linkedClientDocId: String = "",
    val status: String = "Archivé & Validé",
    val documentDate: String = ""
) {
    val timestamp: Long get() = importedAt
    val partnerName: String get() = supplierOrPartnerName
    val documentType: String get() = sourceDocumentType.label
    val originalDocId: String get() = linkedSupplierDocId
    val transformedClientDocId: String get() = linkedClientDocId
    val pageImagePaths: List<String> get() = savedImagePaths
}

object IntelligentOcrEngine {

    // 1. Strict JSON Schema for Firebase AI Logic (Gemini 3.8 Flash)
    private val lineItemSchema = Schema.obj(
        properties = mapOf(
            "pageIndex" to Schema.integer(description = "0-based page index where the item appears"),
            "reference" to Schema.string(description = "Product reference or SKU code only. Never put product description here."),
            "designation" to Schema.string(description = "Full product designation/description, merging multi-line descriptions. Never include totals or footers."),
            "quantite" to Schema.double(description = "Quantity of the item (Qty / Qté). Never confuse with unit price."),
            "prixUnitaireHt" to Schema.double(description = "Unit price excluding tax (PU HT / P.U.H.T / Unit Price). Never confuse with quantity or total line amount."),
            "remisePct" to Schema.double(description = "Discount percentage on this line (0 if none)"),
            "tauxTva" to Schema.double(description = "VAT percentage on this line (e.g., 19.0, 9.0, or 0.0)"),
            "montantHt" to Schema.double(description = "Total line amount HT printed on the document"),
            "confidence" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Confidence level: HIGH if clear and math matches, MEDIUM if slightly blurry, LOW if ambiguous"
            ),
            "confidenceNote" to Schema.string(description = "Short explanation in French of the confidence or any detected discrepancy"),
            "boxLeft" to Schema.double(description = "Normalized left coordinate (0.0 to 1.0) of the row on the page"),
            "boxTop" to Schema.double(description = "Normalized top coordinate (0.0 to 1.0) of the row on the page"),
            "boxRight" to Schema.double(description = "Normalized right coordinate (0.0 to 1.0) of the row on the page"),
            "boxBottom" to Schema.double(description = "Normalized bottom coordinate (0.0 to 1.0) of the row on the page")
        ),
        optionalProperties = listOf(
            "pageIndex",
            "reference",
            "remisePct",
            "tauxTva",
            "montantHt",
            "confidenceNote",
            "boxLeft",
            "boxTop",
            "boxRight",
            "boxBottom"
        )
    )

    private val ocrDocumentSchema = Schema.obj(
        properties = mapOf(
            "documentType" to Schema.enumeration(
                values = listOf(
                    "FACTURE_FOURNISSEUR",
                    "FACTURE",
                    "BL_FOURNISSEUR",
                    "BL_CLIENT",
                    "BON_COMMANDE",
                    "DEVIS_FOURNISSEUR",
                    "DEVIS_CLIENT"
                ),
                description = "Detected commercial document type"
            ),
            "partnerName" to Schema.string(description = "Supplier or client company name (never invent if missing)"),
            "partnerAddress" to Schema.string(description = "Supplier or client address"),
            "partnerPhone" to Schema.string(description = "Supplier or client phone number"),
            "partnerRc" to Schema.string(description = "Registre du Commerce (RC) number"),
            "partnerNif" to Schema.string(description = "Numéro d'Identification Fiscale (NIF)"),
            "partnerNis" to Schema.string(description = "Numéro d'Identification Statistique (NIS)"),
            "partnerArticleImposition" to Schema.string(description = "Article d'imposition (AI)"),
            "partnerRib" to Schema.string(description = "Bank account RIB / IBAN"),
            "invoiceNumber" to Schema.string(description = "Invoice number (N° Facture)"),
            "invoiceDate" to Schema.string(description = "Invoice date (JJ/MM/AAAA)"),
            "bonNumber" to Schema.string(description = "Delivery note number (N° Bon de livraison)"),
            "bonDate" to Schema.string(description = "Delivery note date"),
            "orderReference" to Schema.string(description = "Order reference (Référence de commande / BC)"),
            "paymentConditions" to Schema.string(description = "Payment terms or payment method (Espèces, Chèque, Virement, À terme)"),
            "columnOrderDescription" to Schema.string(description = "Detected order of columns in the table"),
            "qteBeforePu" to Schema.boolean(description = "True if Quantity column appears to the left of Unit Price column"),
            "appliquerTva" to Schema.boolean(description = "True if VAT (TVA) is applied on the document"),
            "defaultTauxTva" to Schema.double(description = "Main VAT rate on the document (e.g. 19.0 or 9.0)"),
            "detectedTotalHt" to Schema.double(description = "Total HT printed at the bottom of the document"),
            "detectedTotalTva" to Schema.double(description = "Total TVA printed at the bottom of the document"),
            "detectedTotalTtc" to Schema.double(description = "Total TTC printed at the bottom of the document"),
            "lines" to Schema.array(
                items = lineItemSchema,
                description = "Normalized list of product lines ONLY (strictly exclude headers, subtotals, Total HT, TVA, Timbre, Total TTC)"
            )
        ),
        optionalProperties = listOf(
            "partnerAddress",
            "partnerPhone",
            "partnerRc",
            "partnerNif",
            "partnerNis",
            "partnerArticleImposition",
            "partnerRib",
            "invoiceNumber",
            "invoiceDate",
            "bonNumber",
            "bonDate",
            "orderReference",
            "paymentConditions",
            "columnOrderDescription",
            "qteBeforePu",
            "detectedTotalHt",
            "detectedTotalTva",
            "detectedTotalTtc"
        )
    )

    private val ocrGenerationConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = ocrDocumentSchema
        }
    }

    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = ocrGenerationConfig
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // =========================================================================
    // 1. IMPORTATION PDF MULTIPAGES & REDRESSEMENT / AMÉLIORATION D'IMAGE
    // =========================================================================

    suspend fun renderPdfPagesToBitmaps(
        context: Context,
        pdfUri: Uri,
        maxPages: Int = 12
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<Bitmap>()
        try {
            context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = 2.0f
                            val w = (page.width * scale).toInt().coerceIn(600, 2200)
                            val h = (page.height * scale).toInt().coerceIn(800, 2800)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pages.add(bmp)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF pages", e)
        }
        pages
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (_: Exception) {
            source
        }
    }

    /**
     * Recadre automatiquement les bordures sombres autour d'un document photographié.
     */
    fun autoCropDocumentMargins(source: Bitmap): Bitmap {
        return try {
            val w = source.width
            val h = source.height
            if (w < 120 || h < 120) return source

            val marginX = (w * 0.03f).toInt()
            val marginY = (h * 0.03f).toInt()
            var top = 0
            var bottom = h - 1
            var left = 0
            var right = w - 1

            fun isDarkBorderPixel(px: Int): Boolean {
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)
                val lum = (r * 299 + g * 587 + b * 114) / 1000
                return lum < 75
            }

            // Détecter si les bords extérieurs sont sombres (table / fond)
            val centerX = w / 2
            val centerY = h / 2
            while (top < marginY * 3 && isDarkBorderPixel(source.getPixel(centerX, top))) top += 2
            while (bottom > h - marginY * 3 && isDarkBorderPixel(source.getPixel(centerX, bottom))) bottom -= 2
            while (left < marginX * 3 && isDarkBorderPixel(source.getPixel(left, centerY))) left += 2
            while (right > w - marginX * 3 && isDarkBorderPixel(source.getPixel(right, centerY))) right -= 2

            val newW = (right - left).coerceAtLeast(w / 2)
            val newH = (bottom - top).coerceAtLeast(h / 2)
            if (newW >= w - 8 && newH >= h - 8) {
                source
            } else {
                Bitmap.createBitmap(source, left, top, newW, newH)
            }
        } catch (_: Exception) {
            source
        }
    }

    /**
     * Améliore la lisibilité d'une facture photographiée (augmentation du contraste et éclaircissement du fond papier).
     */
    fun enhanceDocumentReadability(source: Bitmap): Bitmap {
        return try {
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val contrast = 1.38f
            val brightness = -28f
            val cm = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(source, 0f, 0f, paint)
            result
        } catch (_: Exception) {
            source
        }
    }

    suspend fun saveImmutableOriginalBitmaps(
        context: Context,
        pages: List<Bitmap>,
        archiveId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val savedPaths = mutableListOf<String>()
        try {
            val dir = File(context.filesDir, "ocr_archives")
            if (!dir.exists()) dir.mkdirs()
            pages.forEachIndexed { idx, bmp ->
                val outFile = File(dir, "${archiveId}_page_${idx + 1}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                savedPaths.add(outFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save immutable original copy", e)
        }
        savedPaths
    }

    // =========================================================================
    // 2. ANALYSE HYBRIDE IA (GEMINI 3.8 FLASH + ML KIT + MÉMOIRE FOURNISSEUR)
    // =========================================================================

    /**
     * Étape 1 (Instantanée < 1s) : Reconnaissance structurelle locale ML Kit + Mémoire Fournisseur.
     * Permet d'afficher immédiatement les résultats dès l'importation d'une image ou d'un PDF sans attendre le réseau.
     */
    suspend fun analyzeDocumentPagesLocalFast(
        context: Context,
        pages: List<Bitmap>,
        knownPartners: List<PartnerEntity>,
        catalogProducts: List<ProductCatalogEntity>,
        ownCompanyName: String,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): Result<InvoiceOcrResult> = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Aucune page à analyser."))
        }

        val partnerProfiles = loadAllPartnerProfiles(context)

        val mlKitPageResults = pages.mapIndexed { pageIdx, bmp ->
            InvoiceOcrHelper.recognizeBitmapWithMlKitSuspend(
                bitmap = bmp,
                pageIndex = pageIdx,
                knownPartners = knownPartners,
                catalogProducts = catalogProducts,
                ownCompanyName = ownCompanyName,
                preferredQteBeforePu = if (!columnConfig.isAuto) columnConfig.qteBeforePu else null,
                columnConfig = columnConfig
            )
        }

        val combinedRawText = mlKitPageResults.mapIndexed { idx, res ->
            if (pages.size > 1) "--- PAGE ${idx + 1} ---\n${res.rawText}" else res.rawText
        }.joinToString("\n\n")
        val combinedRows = mlKitPageResults.flatMap { it.reconstructedRows }
        val firstPageMl = mlKitPageResults.first()
        val matchedProfile = partnerProfiles.find {
            it.partnerName.equals(firstPageMl.detectedPartnerName, ignoreCase = true)
        }

        val refinedMlLines = if (columnConfig.isAuto && matchedProfile?.preferredQteBeforePu != null) {
            mlKitPageResults.flatMap { pageRes ->
                InvoiceOcrHelper.parseRawInvoiceText(
                    rawText = pageRes.rawText,
                    reconstructedRows = pageRes.reconstructedRows,
                    knownPartners = knownPartners,
                    catalogProducts = catalogProducts,
                    ownCompanyName = ownCompanyName,
                    preferredQteBeforePu = matchedProfile.preferredQteBeforePu,
                    columnConfig = columnConfig
                ).extractedLines
            }
        } else {
            mlKitPageResults.flatMap { it.extractedLines }
        }

        val mergedMlResult = firstPageMl.copy(
            rawText = combinedRawText,
            reconstructedRows = combinedRows,
            extractedLines = applyLearnedDesignations(refinedMlLines, matchedProfile, catalogProducts),
            detectedTotalHt = mlKitPageResults.mapNotNull { it.detectedTotalHt }.lastOrNull(),
            detectedTotalTtc = mlKitPageResults.mapNotNull { it.detectedTotalTtc }.lastOrNull(),
            detectedTotalTva = mlKitPageResults.mapNotNull { it.detectedTotalTva }.lastOrNull(),
            pageCount = pages.size
        )

        Result.success(mergedMlResult)
    }

    private fun downscaleBitmapForAi(bmp: Bitmap, maxDimension: Int = 1280): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxDimension && h <= maxDimension) return bmp
        val ratio = minOf(maxDimension.toFloat() / w.toFloat(), maxDimension.toFloat() / h.toFloat())
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
        } catch (_: Exception) {
            bmp
        }
    }

    /**
     * Étape 2 (Enrichissement IA asynchrone non-bloquant, timeout court 15s) :
     * Affinage par Firebase AI Logic (Gemini 3.8 Flash) avec schéma JSON strict.
     */
    suspend fun refineWithAi(
        context: Context,
        pages: List<Bitmap>,
        baseMlResult: InvoiceOcrResult,
        catalogProducts: List<ProductCatalogEntity>,
        ownCompanyName: String,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): InvoiceOcrResult? = withContext(Dispatchers.IO) {
        val partnerProfiles = loadAllPartnerProfiles(context)
        val matchedProfile = partnerProfiles.find {
            it.partnerName.equals(baseMlResult.detectedPartnerName, ignoreCase = true)
        }

        try {
            val partnerMemoryContext = if (matchedProfile != null) {
                "Profil fournisseur connu pour « ${matchedProfile.partnerName} » : " +
                    "Ordre habituel des colonnes = ${matchedProfile.columnOrderSummary}, " +
                    "Qté avant PU = ${matchedProfile.preferredQteBeforePu ?: "auto"}, " +
                    "TVA habituelle = ${matchedProfile.usualTvaRate}%."
            } else {
                "Aucun profil mémorisé préalable pour ce fournisseur."
            }

            val userColumnOrderDirective = if (!columnConfig.isAuto) {
                "INSTRUCTION PRIORITAIRE DE L'UTILISATEUR SUR L'ORDRE DES COLONNES : " +
                    "L'utilisateur a spécifié que les colonnes du document sont dans l'ordre exact : « ${columnConfig.summaryLabel()} » " +
                    "(Colonne Réf présente = ${columnConfig.includeReference}, Quantité avant P.U.HT = ${columnConfig.qteBeforePu}, Quantité avant Désignation = ${columnConfig.qteBeforeDesignation}). " +
                    "Respecte strictement cet ordre de colonnes pour extraire chaque ligne !"
            } else {
                "Mode détection automatique de l'ordre des colonnes."
            }

            val catalogContext = catalogProducts.take(60).joinToString("; ") { prod ->
                val refPart = if (prod.reference.isNotBlank()) "[${prod.reference}] " else ""
                "$refPart${prod.name} (PU=${prod.defaultUnitPrice ?: prod.prixAchat ?: 0.0})"
            }

            val promptText = """
                Tu es un expert comptable et commercial spécialisé dans l'extraction de factures, bons de livraison, bons de commande et devis (en français, arabe et anglais).
                Entreprise utilisatrice (à ne PAS confondre avec le fournisseur/client tiers) : "$ownCompanyName".
                $partnerMemoryContext
                $userColumnOrderDirective
                Extrait du catalogue d'articles connus : $catalogContext
                
                RÈGLES ABSOLUES D'EXTRACTION :
                1. Identifie l'ordre exact des colonnes du tableau, même s'il change (ex: Quantité avant Désignation, Référence avant Quantité, Désignation avant Référence, PU HT avant Quantité).
                2. Reconnais toutes les abréviations :
                   - Prix unitaire HT : PU, PU HT, P.U.H.T, Prix unitaire, Unit Price, السعر الفردي.
                   - Quantité : Qté, Qte, Qty, Quantity, Qts, الكمية.
                   - Désignation : Désignation, Article, Description, Libellé, Item, Product, التعيين.
                3. Ne confonds JAMAIS :
                   - Référence et Désignation.
                   - Quantité et Prix Unitaire HT.
                   - Prix Unitaire HT et Montant Total HT.
                   - Ligne d'article et ligne de Total (Total HT, TVA, Timbre, Total TTC, Net à payer, Arrêté...).
                4. ATTENTION AUX DÉCIMALES COLLÉES AU TAUX DE TVA (19% ou 9%) :
                   - Sur de nombreuses factures algériennes, la colonne P.U HT comportant 2 centimes (ex: "361,13") est immédiatement suivie de la colonne TVA ("19" ou "19%").
                   - Ne lis JAMAIS "361,13" + "19" comme deux nombres "361" et "1319", et ne décale JAMAIS le Prix Unitaire (361) dans la colonne Quantité en écrasant la vraie quantité (ex: 309) !
                   - Vérifie toujours : Quantité × Prix Unitaire HT = Montant HT (ex: 309 × 361,13 = 111 589,17).
                5. Si une désignation d'article s'étend sur plusieurs lignes, regroupe son texte dans une seule ligne d'article.
                6. N'invente JAMAIS une information absente (laisse une chaîne vide "" ou 0.0).
                7. Vérifie systématiquement le calcul : Quantité × Prix Unitaire HT × (1 - Remise/100) = Montant HT. Si un écart existe ou si la lecture est incertaine, mets confidence = "LOW" ou "MEDIUM".
                
                Texte OCR brut repéré par page pour t'aider à croiser les positions :
                ${baseMlResult.rawText}
            """.trimIndent()

            val compactPages = pages.take(4).map { downscaleBitmapForAi(it, 1280) }

            val response = withTimeout(45_000L) {
                aiModel.generateContent(
                    content {
                        compactPages.forEach { bmp ->
                            image(bmp)
                        }
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text
            if (!jsonText.isNullOrBlank()) {
                return@withContext parseAiJsonToOcrResult(
                    jsonString = jsonText,
                    fallbackMlResult = baseMlResult,
                    catalogProducts = catalogProducts,
                    partnerProfile = matchedProfile,
                    pageCount = pages.size
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Firebase AI OCR timed out after 45s, keeping fast local structural OCR", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI OCR call failed, keeping fast local structural OCR", e)
        }
        null
    }

    /**
     * Assistant Commercial IA Gemini 3.8 Flash en streaming :
     * Permet à l'utilisateur d'interroger Gemini sur la facture scannée, les écarts de prix,
     * les marges commerciales ou de générer un récapitulatif commercial.
     */
    fun askGeminiCommercialAssistantStream(
        userQuestion: String,
        pages: List<Bitmap>,
        ocrContextSummary: String
    ): Flow<String> = flow {
        val compactPages = pages.take(2).map { downscaleBitmapForAi(it, 1024) }
        val fullPrompt = """
            Tu es l'Assistant IA Commercial et Comptable Gemini (MERELEC Gestion Commerciale).
            Réponds en français de manière claire, structurée et professionnelle.
            
            Contexte du document commercial actuellement ouvert :
            $ocrContextSummary
            
            Demande de l'utilisateur :
            $userQuestion
        """.trimIndent()

        val stream = assistantModel.generateContentStream(
            content {
                compactPages.forEach { bmp ->
                    image(bmp)
                }
                text(fullPrompt)
            }
        )
        stream.collect { chunk ->
            val piece = chunk.text
            if (!piece.isNullOrEmpty()) {
                emit(piece)
            }
        }
    }

    suspend fun analyzeDocumentPages(
        context: Context,
        pages: List<Bitmap>,
        knownPartners: List<PartnerEntity>,
        catalogProducts: List<ProductCatalogEntity>,
        ownCompanyName: String,
        columnConfig: OcrColumnConfig = OcrColumnConfig()
    ): Result<InvoiceOcrResult> = withContext(Dispatchers.IO) {
        val localOutcome = analyzeDocumentPagesLocalFast(
            context = context,
            pages = pages,
            knownPartners = knownPartners,
            catalogProducts = catalogProducts,
            ownCompanyName = ownCompanyName,
            columnConfig = columnConfig
        )
        val mergedMlResult = localOutcome.getOrElse { return@withContext Result.failure(it) }
        val aiRefined = refineWithAi(
            context = context,
            pages = pages,
            baseMlResult = mergedMlResult,
            catalogProducts = catalogProducts,
            ownCompanyName = ownCompanyName,
            columnConfig = columnConfig
        )
        Result.success(aiRefined ?: mergedMlResult)
    }

    /**
     * Parse directement le JSON structuré garanti par `responseSchema` (sans regex ni nettoyage markdown)
     * et croise avec les coordonnées `NormalizedBoundingBox` détectées par ML Kit.
     */
    fun parseAiJsonToOcrResult(
        jsonString: String,
        fallbackMlResult: InvoiceOcrResult,
        catalogProducts: List<ProductCatalogEntity>,
        partnerProfile: OcrPartnerProfile?,
        pageCount: Int
    ): InvoiceOcrResult {
        val json = JSONObject(jsonString)

        val docTypeStr = json.optString("documentType", fallbackMlResult.suggestedOperationType.name)
        val suggestedType = try {
            OperationType.valueOf(docTypeStr)
        } catch (_: Exception) {
            fallbackMlResult.suggestedOperationType
        }

        val partnerName = json.optString("partnerName", "").trim().ifBlank { fallbackMlResult.detectedPartnerName }
        val partnerAddress = json.optString("partnerAddress", "").trim().ifBlank { fallbackMlResult.detectedAddress }
        val partnerPhone = json.optString("partnerPhone", "").trim().ifBlank { fallbackMlResult.detectedPhone }
        val partnerRc = json.optString("partnerRc", "").trim().ifBlank { fallbackMlResult.detectedRc }
        val partnerNif = json.optString("partnerNif", "").trim().ifBlank { fallbackMlResult.detectedNif }
        val partnerNis = json.optString("partnerNis", "").trim().ifBlank { fallbackMlResult.detectedNis }
        val partnerAi = json.optString("partnerArticleImposition", "").trim().ifBlank { fallbackMlResult.detectedArticleImposition }
        val partnerRib = json.optString("partnerRib", "").trim().ifBlank { fallbackMlResult.detectedRib }

        val invoiceNumber = json.optString("invoiceNumber", "").trim().ifBlank { fallbackMlResult.detectedInvoiceNumber }
        val invoiceDate = json.optString("invoiceDate", "").trim().ifBlank { fallbackMlResult.detectedDateText }
        val bonNumber = json.optString("bonNumber", "").trim().ifBlank { fallbackMlResult.detectedBonNumber }
        val bonDate = json.optString("bonDate", "").trim().ifBlank { fallbackMlResult.detectedBonDate }
        val orderRef = json.optString("orderReference", "").trim().ifBlank { fallbackMlResult.detectedOrderReference }
        val paymentCond = json.optString("paymentConditions", "").trim().ifBlank { fallbackMlResult.detectedPaymentConditions }
        val colOrderDesc = json.optString("columnOrderDescription", "").trim().ifBlank { fallbackMlResult.detectedColumnOrderDescription }

        val appliquerTva = json.optBoolean("appliquerTva", fallbackMlResult.appliquerTva)
        val defaultTauxTva = json.optDouble("defaultTauxTva", fallbackMlResult.tauxTva).let {
            if (it.isNaN() || it <= 0.0) fallbackMlResult.tauxTva else it
        }

        val detectedTotalHt = json.optDouble("detectedTotalHt", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: fallbackMlResult.detectedTotalHt
        val detectedTotalTva = json.optDouble("detectedTotalTva", Double.NaN).takeIf { !it.isNaN() && it >= 0.0 }
            ?: fallbackMlResult.detectedTotalTva
        val detectedTotalTtc = json.optDouble("detectedTotalTtc", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: fallbackMlResult.detectedTotalTtc

        val linesArray = json.optJSONArray("lines") ?: JSONArray()
        val parsedLines = mutableListOf<OcrExtractedLine>()

        for (i in 0 until linesArray.length()) {
            val item = linesArray.optJSONObject(i) ?: continue
            val rawDes = item.optString("designation", "").trim()
            if (rawDes.isBlank() ||
                InvoiceOcrHelper.isTableHeaderRow(rawDes) ||
                InvoiceOcrHelper.isTableFooterOrTotalRow(rawDes)
            ) {
                continue
            }

            val rawRef = item.optString("reference", "").trim()
            var qty = item.optDouble("quantite", 1.0).let { if (it.isNaN() || it <= 0.0) 1.0 else it }
            var puHt = item.optDouble("prixUnitaireHt", 0.0).let { if (it.isNaN() || it < 0.0) 0.0 else it }
            val remise = item.optDouble("remisePct", 0.0).let { if (it.isNaN() || it < 0.0) 0.0 else it.coerceIn(0.0, 100.0) }
            val tva = item.optDouble("tauxTva", defaultTauxTva).let { if (it.isNaN() || it < 0.0) defaultTauxTva else it }
            var montantHt = item.optDouble("montantHt", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            val pageIdx = item.optInt("pageIndex", 0).coerceAtLeast(0)

            val matchedMlLine = fallbackMlResult.extractedLines.find { ml ->
                ml.designation.contains(rawDes.take(10), ignoreCase = true) ||
                    rawDes.contains(ml.designation.take(10), ignoreCase = true) ||
                    (rawRef.isNotBlank() && ml.reference.equals(rawRef, ignoreCase = true))
            }

            // Protection contre le bug où l'IA a lu "361,13 19" comme Quantité=361 et PU=1319 :
            // Si la ligne locale ML Kit a un calcul exact (Qté × PU = Montant HT) alors que l'IA a un écart,
            // ou si puHt est un entier 4 chiffres finissant par 19/09 (centimes collés à la TVA), on restaure les valeurs vérifiées !
            if (matchedMlLine != null &&
                matchedMlLine.confidence == OcrConfidenceLevel.HIGH &&
                !matchedMlLine.hasCalculationMismatch &&
                matchedMlLine.prixUnitaire > 0.0
            ) {
                val aiExpected = round(qty * puHt * (1.0 - remise / 100.0) * 100.0) / 100.0
                val refTotal = montantHt ?: matchedMlLine.extractedMontantHt
                if (refTotal != null && refTotal > 0.0 && abs(aiExpected - refTotal) > max(2.0, refTotal * 0.02)) {
                    qty = matchedMlLine.quantite
                    puHt = matchedMlLine.prixUnitaire
                    montantHt = matchedMlLine.extractedMontantHt ?: montantHt
                }
            } else if (qty % 1.0 == 0.0 && puHt % 1.0 == 0.0 && puHt in 1000.0..9999.0) {
                val puInt = puHt.toInt()
                val tail2 = puInt % 100
                if (tail2 == 19 || tail2 == 9) {
                    val cents = puInt / 100
                    val reconstructedPu = round((qty + cents / 100.0) * 100.0) / 100.0
                    val refTotal = montantHt ?: matchedMlLine?.extractedMontantHt
                    if (refTotal != null && refTotal > reconstructedPu) {
                        val impliedQty = round(refTotal / reconstructedPu)
                        if (impliedQty >= 1.0 && abs(impliedQty * reconstructedPu - refTotal) <= max(2.0, refTotal * 0.015)) {
                            qty = impliedQty
                            puHt = reconstructedPu
                        }
                    }
                }
            }

            val boxL = item.optDouble("boxLeft", Double.NaN)
            val boxT = item.optDouble("boxTop", Double.NaN)
            val boxR = item.optDouble("boxRight", Double.NaN)
            val boxB = item.optDouble("boxBottom", Double.NaN)
            val boundingBox = if (!boxL.isNaN() && !boxT.isNaN() && !boxR.isNaN() && !boxB.isNaN() && boxR > boxL && boxB > boxT) {
                NormalizedBoundingBox(
                    left = boxL.toFloat().coerceIn(0f, 1f),
                    top = boxT.toFloat().coerceIn(0f, 1f),
                    right = boxR.toFloat().coerceIn(0f, 1f),
                    bottom = boxB.toFloat().coerceIn(0f, 1f),
                    pageIndex = pageIdx
                )
            } else {
                matchedMlLine?.boundingBox
            }

            val learnedDes = partnerProfile?.learnedDesignations?.get(rawDes.lowercase(Locale.FRENCH)) ?: rawDes
            val matchedCatalog = catalogProducts.find { prod ->
                (rawRef.isNotBlank() && prod.reference.equals(rawRef, ignoreCase = true)) ||
                    prod.name.equals(learnedDes, ignoreCase = true)
            }

            val expectedHt = round(qty * puHt * (1.0 - remise / 100.0) * 100.0) / 100.0
            val mathMismatch = montantHt != null && montantHt > 0.0 && abs(expectedHt - montantHt) > max(2.0, montantHt * 0.05)

            val aiConfStr = item.optString("confidence", "HIGH")
            val baseConf = when {
                mathMismatch || puHt <= 0.0 -> OcrConfidenceLevel.LOW
                aiConfStr == "LOW" -> OcrConfidenceLevel.LOW
                aiConfStr == "MEDIUM" -> OcrConfidenceLevel.MEDIUM
                else -> OcrConfidenceLevel.HIGH
            }
            val confNote = item.optString("confidenceNote", "").trim().ifBlank {
                when {
                    mathMismatch -> "Écart de calcul détecté : Qts × PU HT = ${CurrencyUtils.formatQty(expectedHt)} ≠ ${CurrencyUtils.formatQty(montantHt ?: 0.0)}"
                    baseConf == OcrConfidenceLevel.HIGH -> "Reconnaissance IA fiable (Qts × PU HT vérifié)"
                    baseConf == OcrConfidenceLevel.MEDIUM -> "Reconnaissance IA à vérifier"
                    else -> "Donnée incertaine ou prix manquant"
                }
            }

            parsedLines.add(
                OcrExtractedLine(
                    pageIndex = pageIdx,
                    reference = rawRef.ifBlank { matchedCatalog?.reference.orEmpty() },
                    designation = matchedCatalog?.name ?: learnedDes,
                    quantite = qty,
                    prixUnitaire = if (puHt > 0.0) puHt else (matchedCatalog?.defaultUnitPrice ?: matchedCatalog?.prixAchat ?: 0.0),
                    remisePct = remise,
                    tauxTva = tva,
                    extractedMontantHt = montantHt,
                    confidence = baseConf,
                    confidenceNote = confNote,
                    boundingBox = boundingBox,
                    selected = true,
                    matchedCatalogProductName = matchedCatalog?.name
                )
            )
        }

        val finalLines = if (parsedLines.isNotEmpty()) parsedLines else fallbackMlResult.extractedLines

        return fallbackMlResult.copy(
            detectedInvoiceNumber = invoiceNumber,
            detectedPartnerName = partnerName,
            detectedDateText = invoiceDate,
            suggestedOperationType = suggestedType,
            appliquerTva = appliquerTva,
            tauxTva = defaultTauxTva,
            extractedLines = finalLines,
            detectedTotalHt = detectedTotalHt,
            detectedTotalTtc = detectedTotalTtc,
            detectedTotalTva = detectedTotalTva,
            detectedBonNumber = bonNumber,
            detectedBonDate = bonDate,
            detectedOrderReference = orderRef,
            detectedPaymentConditions = paymentCond,
            detectedAddress = partnerAddress,
            detectedPhone = partnerPhone,
            detectedRc = partnerRc,
            detectedNif = partnerNif,
            detectedNis = partnerNis,
            detectedArticleImposition = partnerAi,
            detectedRib = partnerRib,
            detectedColumnOrderDescription = colOrderDesc,
            analyzedByAi = true,
            pageCount = pageCount
        )
    }

    private fun applyLearnedDesignations(
        lines: List<OcrExtractedLine>,
        profile: OcrPartnerProfile?,
        catalogProducts: List<ProductCatalogEntity>
    ): List<OcrExtractedLine> {
        if (profile == null || profile.learnedDesignations.isEmpty()) return lines
        return lines.map { line ->
            val corrected = profile.learnedDesignations[line.designation.trim().lowercase(Locale.FRENCH)]
            if (!corrected.isNullOrBlank()) {
                val matchedCat = catalogProducts.find { it.name.equals(corrected, ignoreCase = true) }
                line.copy(
                    designation = corrected,
                    reference = line.reference.ifBlank { matchedCat?.reference.orEmpty() },
                    matchedCatalogProductName = matchedCat?.name ?: line.matchedCatalogProductName
                )
            } else {
                line
            }
        }
    }

    // =========================================================================
    // 3. MÉMOIRE DOCUMENTAIRE DES FOURNISSEURS ET CLIENTS
    // =========================================================================

    fun loadAllPartnerProfiles(context: Context): List<OcrPartnerProfile> {
        val prefs = context.getSharedPreferences(PREFS_OCR_INTELLIGENT, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PARTNER_PROFILES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<OcrPartnerProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pName = obj.optString("partnerName", "").trim()
                if (pName.isEmpty()) continue
                val learnedObj = obj.optJSONObject("learnedDesignations")
                val learnedMap = mutableMapOf<String, String>()
                if (learnedObj != null) {
                    val keys = learnedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        learnedMap[k] = learnedObj.optString(k, "")
                    }
                }
                val hasPrefOrder = obj.has("preferredQteBeforePu") && !obj.isNull("preferredQteBeforePu")
                list.add(
                    OcrPartnerProfile(
                        partnerName = pName,
                        preferredQteBeforePu = if (hasPrefOrder) obj.optBoolean("preferredQteBeforePu", true) else null,
                        columnOrderSummary = obj.optString("columnOrderSummary", "Standard"),
                        defaultMarginPct = obj.optDouble("defaultMarginPct", 20.0),
                        usualTvaRate = obj.optDouble("usualTvaRate", 19.0),
                        address = obj.optString("address", ""),
                        phone = obj.optString("phone", ""),
                        rc = obj.optString("rc", ""),
                        nif = obj.optString("nif", ""),
                        nis = obj.optString("nis", ""),
                        articleImposition = obj.optString("articleImposition", ""),
                        learnedDesignations = learnedMap,
                        scanCount = obj.optInt("scanCount", 1),
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.lastUpdated }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun findPartnerProfile(context: Context, partnerName: String): OcrPartnerProfile? {
        val clean = partnerName.trim()
        if (clean.isEmpty()) return null
        return loadAllPartnerProfiles(context).find {
            it.partnerName.trim().equals(clean, ignoreCase = true)
        }
    }

    fun recordPartnerLearning(
        context: Context,
        partnerName: String,
        ocrResult: InvoiceOcrResult,
        correctedLines: List<OcrExtractedLine>,
        appliedMarginPct: Double,
        appliedTvaRate: Double
    ) {
        val cleanName = partnerName.trim()
        if (cleanName.isBlank() || cleanName.equals("Client Comptoir", ignoreCase = true) || cleanName.equals("Fournisseur Divers", ignoreCase = true)) {
            return
        }
        val existingList = loadAllPartnerProfiles(context).toMutableList()
        val idx = existingList.indexOfFirst { it.partnerName.equals(cleanName, ignoreCase = true) }
        val previous = if (idx >= 0) existingList[idx] else null

        val updatedLearned = (previous?.learnedDesignations ?: emptyMap()).toMutableMap()
        ocrResult.extractedLines.forEach { rawLine ->
            val corrected = correctedLines.find { it.id == rawLine.id }
            if (corrected != null &&
                rawLine.designation.isNotBlank() &&
                corrected.designation.isNotBlank() &&
                !rawLine.designation.equals(corrected.designation, ignoreCase = true)
            ) {
                updatedLearned[rawLine.designation.trim().lowercase(Locale.FRENCH)] = corrected.designation.trim()
            }
        }

        val qteBeforePu = when {
            ocrResult.detectedColumnOrderDescription.contains("Quantité (Qts) → Prix", ignoreCase = true) -> true
            ocrResult.detectedColumnOrderDescription.contains("Prix Unitaire (PU HT) → Quantité", ignoreCase = true) -> false
            else -> previous?.preferredQteBeforePu
        }

        val updatedProfile = OcrPartnerProfile(
            partnerName = cleanName,
            preferredQteBeforePu = qteBeforePu,
            columnOrderSummary = ocrResult.detectedColumnOrderDescription.ifBlank { previous?.columnOrderSummary ?: "Standard" },
            defaultMarginPct = if (appliedMarginPct > 0.0) appliedMarginPct else (previous?.defaultMarginPct ?: 20.0),
            usualTvaRate = if (appliedTvaRate > 0.0) appliedTvaRate else (previous?.usualTvaRate ?: 19.0),
            address = ocrResult.detectedAddress.ifBlank { previous?.address.orEmpty() },
            phone = ocrResult.detectedPhone.ifBlank { previous?.phone.orEmpty() },
            rc = ocrResult.detectedRc.ifBlank { previous?.rc.orEmpty() },
            nif = ocrResult.detectedNif.ifBlank { previous?.nif.orEmpty() },
            nis = ocrResult.detectedNis.ifBlank { previous?.nis.orEmpty() },
            articleImposition = ocrResult.detectedArticleImposition.ifBlank { previous?.articleImposition.orEmpty() },
            learnedDesignations = updatedLearned,
            scanCount = (previous?.scanCount ?: 0) + 1,
            lastUpdated = System.currentTimeMillis()
        )

        if (idx >= 0) {
            existingList[idx] = updatedProfile
        } else {
            existingList.add(0, updatedProfile)
        }

        val arr = JSONArray()
        existingList.take(150).forEach { prof ->
            val learnedJson = JSONObject()
            prof.learnedDesignations.forEach { (k, v) -> learnedJson.put(k, v) }
            arr.put(
                JSONObject().apply {
                    put("partnerName", prof.partnerName)
                    if (prof.preferredQteBeforePu != null) put("preferredQteBeforePu", prof.preferredQteBeforePu)
                    put("columnOrderSummary", prof.columnOrderSummary)
                    put("defaultMarginPct", prof.defaultMarginPct)
                    put("usualTvaRate", prof.usualTvaRate)
                    put("address", prof.address)
                    put("phone", prof.phone)
                    put("rc", prof.rc)
                    put("nif", prof.nif)
                    put("nis", prof.nis)
                    put("articleImposition", prof.articleImposition)
                    put("learnedDesignations", learnedJson)
                    put("scanCount", prof.scanCount)
                    put("lastUpdated", prof.lastUpdated)
                }
            )
        }
        context.getSharedPreferences(PREFS_OCR_INTELLIGENT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PARTNER_PROFILES_JSON, arr.toString())
            .apply()
    }

    // =========================================================================
    // 4. HISTORIQUE ET ARCHIVAGE DES DOCUMENTS OCR
    // =========================================================================

    fun loadArchiveHistory(context: Context): List<OcrArchiveRecord> {
        val prefs = context.getSharedPreferences(PREFS_OCR_INTELLIGENT, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ARCHIVE_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<OcrArchiveRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pathsArr = obj.optJSONArray("savedImagePaths") ?: JSONArray()
                val paths = (0 until pathsArr.length()).mapNotNull { pathsArr.optString(it).takeIf { s -> s.isNotBlank() } }
                val srcType = try {
                    OperationType.valueOf(obj.optString("sourceDocumentType", "FACTURE_FOURNISSEUR"))
                } catch (_: Exception) {
                    OperationType.FACTURE_FOURNISSEUR
                }
                val dstType = try {
                    OperationType.valueOf(obj.optString("targetDocumentType", "BL_CLIENT"))
                } catch (_: Exception) {
                    OperationType.BL_CLIENT
                }
                val mode = try {
                    OcrTransformationAction.valueOf(obj.optString("actionMode", "KEEP_ORIGINAL"))
                } catch (_: Exception) {
                    OcrTransformationAction.KEEP_ORIGINAL
                }
                list.add(
                    OcrArchiveRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        importedAt = obj.optLong("importedAt", System.currentTimeMillis()),
                        supplierOrPartnerName = obj.optString("supplierOrPartnerName", ""),
                        targetClientName = obj.optString("targetClientName", ""),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        bonNumber = obj.optString("bonNumber", ""),
                        orderReference = obj.optString("orderReference", ""),
                        sourceDocumentType = srcType,
                        targetDocumentType = dstType,
                        actionMode = mode,
                        totalHt = obj.optDouble("totalHt", 0.0),
                        totalTtc = obj.optDouble("totalTtc", 0.0),
                        linesCount = obj.optInt("linesCount", 0),
                        pageCount = obj.optInt("pageCount", 1),
                        rawOcrText = obj.optString("rawOcrText", ""),
                        manualCorrectionsSummary = obj.optString("manualCorrectionsSummary", ""),
                        savedImagePaths = paths,
                        linkedSupplierDocId = obj.optString("linkedSupplierDocId", ""),
                        linkedClientDocId = obj.optString("linkedClientDocId", ""),
                        status = obj.optString("status", "Archivé")
                    )
                )
            }
            list.sortedByDescending { it.importedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveArchiveRecord(context: Context, record: OcrArchiveRecord) {
        val current = loadArchiveHistory(context).toMutableList()
        current.removeAll { it.id == record.id }
        current.add(0, record)

        val arr = JSONArray()
        current.take(200).forEach { rec ->
            val pathsArr = JSONArray()
            rec.savedImagePaths.forEach { pathsArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("importedAt", rec.importedAt)
                    put("supplierOrPartnerName", rec.supplierOrPartnerName)
                    put("targetClientName", rec.targetClientName)
                    put("invoiceNumber", rec.invoiceNumber)
                    put("bonNumber", rec.bonNumber)
                    put("orderReference", rec.orderReference)
                    put("sourceDocumentType", rec.sourceDocumentType.name)
                    put("targetDocumentType", rec.targetDocumentType.name)
                    put("actionMode", rec.actionMode.name)
                    put("totalHt", rec.totalHt)
                    put("totalTtc", rec.totalTtc)
                    put("linesCount", rec.linesCount)
                    put("pageCount", rec.pageCount)
                    put("rawOcrText", rec.rawOcrText.take(4000))
                    put("manualCorrectionsSummary", rec.manualCorrectionsSummary.take(1000))
                    put("savedImagePaths", pathsArr)
                    put("linkedSupplierDocId", rec.linkedSupplierDocId)
                    put("linkedClientDocId", rec.linkedClientDocId)
                    put("status", rec.status)
                }
            )
        }
        context.getSharedPreferences(PREFS_OCR_INTELLIGENT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ARCHIVE_HISTORY_JSON, arr.toString())
            .apply()
    }
}
