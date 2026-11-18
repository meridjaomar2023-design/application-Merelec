package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.CompanySettings
import com.example.data.model.GroupedOperationLine
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.groupLignesByParent
import com.example.data.repository.CompanySettingsManager
import com.example.ui.viewmodel.CreancesSummary
import com.example.ui.viewmodel.PartnerBalanceItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points at 72dpi (210mm)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points at 72dpi (297mm)
    private const val TEXT_PRECISION_SCALE = 8f // Subpixel precision scale to eliminate 72dpi kerning jitter

    private fun configureNormalKerning(paint: Paint, isBold: Boolean = false) {
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.isLinearText = true
        paint.hinting = Paint.HINTING_OFF
        paint.letterSpacing = 0f
        paint.textScaleX = 1.0f
        paint.isFakeBoldText = false
        paint.typeface = Typeface.create(
            Typeface.SANS_SERIF,
            if (isBold) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun createNormalPaint(colorInt: Int = Color.rgb(30, 41, 59)): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            configureNormalKerning(this, isBold = false)
            color = colorInt
        }

    private fun createBoldPaint(colorInt: Int = Color.rgb(15, 23, 42)): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            configureNormalKerning(this, isBold = true)
            color = colorInt
        }

    private fun measureCleanText(paint: Paint, text: String): Float {
        if (text.isEmpty()) return 0f
        val origSize = paint.textSize
        paint.letterSpacing = 0f
        paint.textScaleX = 1.0f
        paint.isSubpixelText = true
        paint.isLinearText = true
        paint.hinting = Paint.HINTING_OFF
        paint.textSize = origSize * TEXT_PRECISION_SCALE
        val width = paint.measureText(text) / TEXT_PRECISION_SCALE
        paint.textSize = origSize
        return width
    }

    private fun Canvas.drawCleanText(text: String, x: Float, y: Float, paint: Paint) {
        if (text.isEmpty()) return
        val origSize = paint.textSize
        paint.letterSpacing = 0f
        paint.textScaleX = 1.0f
        paint.isSubpixelText = true
        paint.isLinearText = true
        paint.hinting = Paint.HINTING_OFF
        save()
        translate(x, y)
        scale(1f / TEXT_PRECISION_SCALE, 1f / TEXT_PRECISION_SCALE)
        paint.textSize = origSize * TEXT_PRECISION_SCALE
        drawText(text, 0f, 0f, paint)
        paint.textSize = origSize
        restore()
    }

    /**
     * Blocs de coordonnées de l'en-tête PDF réorganisables par l'utilisateur
     */
    enum class HeaderBlockType(val label: String, val description: String) {
        SLOGAN("Slogan / Activité", "Activité principale ou slogan commercial"),
        CONTACT("Adresse, Téléphone & Email", "Siège social, numéros de téléphone, email et site web"),
        FISCAL("Identifiants Fiscaux (RC, NIF, NIS, AI, NIN)", "Registre de commerce et identifiants légaux"),
        BANQUE("Coordonnées Bancaires (Banque & RIB)", "Nom de la banque et numéro de compte RIB");

        companion object {
            fun fromOrderString(orderStr: String): List<HeaderBlockType> {
                val parsed = orderStr.split(",")
                    .mapNotNull { token -> entries.find { it.name.equals(token.trim(), ignoreCase = true) } }
                    .distinct()
                val missing = entries.filter { !parsed.contains(it) }
                return parsed + missing
            }
        }
    }

    /**
     * Colonnes réorganisables pour les Devis, Bons (BL / BC / BR) et Factures
     */
    enum class DocPdfColumn(
        val label: String,
        val shortHeader: String,
        val weight: Float,
        val defaultSelected: Boolean = true
    ) {
        NUMERO("N° d'ordre", "N°", 0.55f, true),
        REFERENCE("Référence Article", "Réf", 1.15f, false),
        DESIGNATION("Désignation des articles", "Désignation des articles", 3.35f, true),
        QUANTITE("Quantité", "Qté", 0.85f, true),
        UNITE("Unité", "Unité", 0.75f, true),
        PU_HT("Prix Unitaire HT", "P.U HT", 1.40f, true),
        TOTAL_HT("Total HT", "Total HT", 1.50f, true);

        companion object {
            fun defaultColumns(): List<DocPdfColumn> = entries.filter { it.defaultSelected }

            fun fromOrderString(orderStr: String): List<DocPdfColumn> {
                if (orderStr.isBlank()) return defaultColumns()
                val parsed = orderStr.split(",")
                    .mapNotNull { token -> entries.find { it.name.equals(token.trim(), ignoreCase = true) } }
                    .distinct()
                return parsed.ifEmpty { defaultColumns() }
            }
        }
    }

    /**
     * Colonnes réorganisables pour l'État des Créances Clients en PDF
     */
    enum class CreancePdfColumn(
        val label: String,
        val shortHeader: String,
        val weight: Float,
        val defaultSelected: Boolean = true
    ) {
        NUMERO("N° d'ordre", "N°", 0.5f, true),
        CLIENT("Client & Raison Sociale", "Client & Raison Sociale", 2.4f, true),
        TELEPHONE_VILLE("Téléphone & Ville", "Téléphone / Ville", 1.6f, true),
        IDENTIFIANTS_FISCAUX("NIF / RC du Client", "NIF / RC", 1.5f, false),
        TOTAL_FACTURE("Total Facturé / BL (DA)", "Facturé (DA)", 1.35f, true),
        TOTAL_ENCAISSE("Total Encaissé (DA)", "Encaissé (DA)", 1.35f, true),
        CREANCE_DUE("Créance Restante Dûe (DA)", "Créance Dûe (DA)", 1.45f, true),
        TAUX_RECOUVREMENT("Taux de Recouvrement (%)", "Recouvr.", 0.8f, true);

        companion object {
            fun defaultColumns(): List<CreancePdfColumn> = entries.filter { it.defaultSelected }

            fun fromOrderString(orderStr: String): List<CreancePdfColumn> {
                if (orderStr.isBlank()) return defaultColumns()
                val parsed = orderStr.split(",")
                    .mapNotNull { token -> entries.find { it.name.equals(token.trim(), ignoreCase = true) } }
                    .distinct()
                return parsed.ifEmpty { defaultColumns() }
            }
        }
    }

    /**
     * Colonnes sélectionnables et réorganisables pour l'export PDF du Catalogue d'Articles
     */
    enum class ArticlePdfColumn(
        val label: String,
        val shortHeader: String,
        val weight: Float,
        val defaultSelected: Boolean = true
    ) {
        NUMERO("N° d'ordre", "N°", 0.55f, true),
        REFERENCE("Référence", "Réf", 1.25f, true),
        DESIGNATION("Désignation de l'Article", "Désignation", 2.5f, true),
        FAMILLE("Famille", "Famille", 1.3f, true),
        MARQUE("Marque", "Marque", 1.2f, true),
        PRIX_VENTE("Prix de vente (DA)", "P. Vente (DA)", 1.3f, true),
        PRIX_ACHAT("Prix d'achat (DA)", "P. Achat (DA)", 1.3f, true),
        STOCK("Stock actuel", "Stock", 1.0f, true),
        UNITE("Unité", "Unité", 0.7f, false),
        CODE_BARRES("Code-barres", "Code-barres", 1.3f, false),
        VALEUR_STOCK("Valeur Stock (DA)", "Valeur (DA)", 1.35f, false);

        companion object {
            fun defaultColumns(): List<ArticlePdfColumn> = entries.filter { it.defaultSelected }

            fun fromOrderString(orderStr: String): List<ArticlePdfColumn> {
                if (orderStr.isBlank()) return defaultColumns()
                val parsed = orderStr.split(",")
                    .mapNotNull { token -> entries.find { it.name.equals(token.trim(), ignoreCase = true) } }
                    .distinct()
                return parsed.ifEmpty { defaultColumns() }
            }
        }
    }

    private fun loadCompanyLogoBitmap(context: Context): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val logoFile = File(context.filesDir, "custom_logo.png")
            if (logoFile.exists() && logoFile.length() > 0L) {
                BitmapFactory.decodeFile(logoFile.absolutePath, opts)
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.logo_merelec, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retourne le fichier PNG HD du Cachet Humide (personnalisé s'il existe, sinon génère un cachet humide officiel en PNG HD transparent).
     */
    fun getOrCreateWetStampPngFile(context: Context, settings: CompanySettings): File {
        val customFile = File(context.filesDir, "custom_stamp.png")
        if (customFile.exists() && customFile.length() > 0L) {
            return customFile
        }
        val defaultFile = File(context.filesDir, "default_stamp.png")
        try {
            val width = 840
            val height = 380
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val inkColor = Color.argb(232, 21, 76, 176) // Bleu encre cachet humide

            c.save()
            c.rotate(-3.0f, width / 2f, height / 2f)

            val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 10f
                color = inkColor
            }
            val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3.8f
                color = inkColor
            }
            val titlePaint = createBoldPaint(inkColor).apply {
                textAlign = Paint.Align.CENTER
                textSize = 42f
            }
            val subPaint = createBoldPaint(inkColor).apply {
                textAlign = Paint.Align.CENTER
                textSize = 26f
            }
            val detailPaint = createNormalPaint(inkColor).apply {
                textAlign = Paint.Align.CENTER
                textSize = 23f
            }

            val outerRect = RectF(32f, 32f, width - 32f, height - 32f)
            val innerRect = RectF(48f, 48f, width - 48f, height - 48f)
            c.drawRoundRect(outerRect, 26f, 26f, outerStroke)
            c.drawRoundRect(innerRect, 18f, 18f, innerStroke)

            val compName = settings.companyName.ifBlank { "GESTION MERÉLEC" }.uppercase(Locale.FRENCH)
            c.drawCleanText(fitTextToWidth(compName, titlePaint, width - 130f), width / 2f, 108f, titlePaint)

            val slogan = settings.activitySlogan.ifBlank { "Équipements & Réalisations Électriques" }
            c.drawCleanText(fitTextToWidth(slogan, subPaint, width - 130f), width / 2f, 150f, subPaint)

            c.drawLine(76f, 170f, width - 76f, 170f, innerStroke)

            val addrTel = listOfNotNull(
                settings.address.ifBlank { null },
                settings.phone.ifBlank { null }?.let { "Tél: $it" }
            ).joinToString(" • ").ifBlank { "Algérie" }
            c.drawCleanText(fitTextToWidth(addrTel, detailPaint, width - 130f), width / 2f, 212f, detailPaint)

            val rcNif = listOfNotNull(
                settings.rc.ifBlank { null }?.let { "RC: $it" },
                settings.nif.ifBlank { null }?.let { "NIF: $it" }
            ).joinToString("  •  ")
            if (rcNif.isNotBlank()) {
                c.drawCleanText(fitTextToWidth(rcNif, detailPaint, width - 130f), width / 2f, 252f, detailPaint)
            }

            c.drawLine(76f, 272f, width - 76f, 272f, innerStroke)
            c.drawCleanText("★  CACHET HUMIDE OFFICIEL  ★", width / 2f, 314f, subPaint)

            c.restore()

            FileOutputStream(defaultFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
        }
        return defaultFile
    }

    fun loadOrCreateWetStampBitmap(context: Context, settings: CompanySettings): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val file = getOrCreateWetStampPngFile(context, settings)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath, opts) else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Dessine uniquement le Cachet Humide (PNG HD) à l'intérieur du cadre réservé à l'entreprise,
     * avec prise en charge de l'agrandissement proportionnel (pdfStampScale en %, de 80% à 250%).
     * La signature stylo a été supprimée conformément à la demande.
     */
    private fun drawWetStampAndSignatureInBox(
        context: Context,
        canvas: Canvas,
        settings: CompanySettings,
        box: RectF,
        titleBottomY: Float
    ) {
        if (!settings.pdfShowStampAndSignature) return

        val stampBmp = loadOrCreateWetStampBitmap(context, settings) ?: return
        val bmpW = stampBmp.width.toFloat().coerceAtLeast(1f)
        val bmpH = stampBmp.height.toFloat().coerceAtLeast(1f)

        val availableTop = (titleBottomY + 2f).coerceAtLeast(box.top + 16f)
        val availableHeight = (box.bottom - availableTop - 4f).coerceAtLeast(36f)

        val scaleFactor = (settings.pdfStampScale.coerceIn(80, 250)) / 100f

        // Dimensions de référence fixes à 100% (97x44 pt) pour garantir une proportionnalité linéaire stricte
        // jusqu'à 225% (218.25x99 pt) et 250% (242.5x110 pt) sans plafonnement par la largeur du cadre (251.5 pt).
        val targetMaxW = 97f * scaleFactor
        val targetMaxH = 44f * scaleFactor

        val maxBoxW = (box.width() - 4f).coerceAtLeast(60f)
        val maxBoxH = availableHeight.coerceAtLeast(36f)

        // Facteur d'échelle unique préservant strictement le ratio largeur/hauteur du cachet PNG
        val proportionalRatio = minOf(
            targetMaxW / bmpW,
            targetMaxH / bmpH,
            maxBoxW / bmpW,
            maxBoxH / bmpH
        ).coerceAtLeast(0.01f)

        val drawW = bmpW * proportionalRatio
        val drawH = bmpH * proportionalRatio

        val centerX = box.centerX()
        val centerY = availableTop + availableHeight / 2f
        val destRect = RectF(
            centerX - drawW / 2f,
            centerY - drawH / 2f,
            centerX + drawW / 2f,
            centerY + drawH / 2f
        )

        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawBitmap(stampBmp, null, destRect, bmpPaint)
    }

    /**
     * Dessine l'en-tête complet de l'entreprise (Logo HD sans perte + Coordonnées réorganisées) + Badge optionnel à droite
     * et retourne la nouvelle ordonnée Y après l'en-tête.
     */
    private fun drawCompanyHeaderBlock(
        context: Context,
        canvas: Canvas,
        settings: CompanySettings,
        left: Float,
        right: Float,
        startY: Float,
        primaryColor: Int,
        rightBadgeTitle: String? = null,
        rightDocNumber: String? = null,
        rightDateStr: String? = null
    ): Float {
        val boldPaint = createBoldPaint(Color.rgb(2, 132, 199))
        val grayPaint = createNormalPaint(Color.rgb(100, 116, 139))
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val hdLogoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val y = startY
        val hasRightBadge = !rightBadgeTitle.isNullOrBlank()
        val rightColumnLeft = if (hasRightBadge) right - 182f else right
        val availableHeaderWidth = (rightColumnLeft - left - 10f).coerceAtLeast(180f)

        val logoSize = settings.pdfLogoSize.coerceIn(34, 84).toFloat()
        val logoBitmap = if (settings.pdfShowLogo) loadCompanyLogoBitmap(context) else null
        val logoPos = settings.pdfLogoPosition.uppercase(Locale.ROOT)

        var textStartX = left
        var textMaxWidth = availableHeaderWidth
        var currentTextY = y + 13f
        var logoBottomY = y

        if (logoBitmap != null && logoBitmap.width > 0 && logoBitmap.height > 0) {
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val drawW: Float
            val drawH: Float
            if (aspect >= 1f) {
                drawW = logoSize
                drawH = (logoSize / aspect).coerceAtLeast(20f)
            } else {
                drawH = logoSize
                drawW = (logoSize * aspect).coerceAtLeast(20f)
            }

            when (logoPos) {
                "CENTER" -> {
                    val centerX = if (hasRightBadge) {
                        left + (availableHeaderWidth - drawW) / 2f
                    } else {
                        (left + right - drawW) / 2f
                    }
                    val destRect = RectF(centerX, y, centerX + drawW, y + drawH)
                    // Dessin direct du Bitmap HD original sans redimensionnement destructif
                    canvas.drawBitmap(logoBitmap, null, destRect, hdLogoPaint)
                    logoBottomY = y + drawH + 4f
                    currentTextY = logoBottomY + 11f
                }
                "RIGHT" -> {
                    val logoX = if (hasRightBadge) {
                        rightColumnLeft - drawW - 10f
                    } else {
                        right - drawW
                    }
                    val destRect = RectF(logoX, y, logoX + drawW, y + drawH)
                    canvas.drawBitmap(logoBitmap, null, destRect, hdLogoPaint)
                    logoBottomY = y + drawH
                    textMaxWidth = (logoX - left - 10f).coerceAtLeast(140f)
                }
                else -> { // LEFT
                    val destRect = RectF(left, y, left + drawW, y + drawH)
                    canvas.drawBitmap(logoBitmap, null, destRect, hdLogoPaint)
                    logoBottomY = y + drawH
                    textStartX = left + drawW + 10f
                    textMaxWidth = (rightColumnLeft - textStartX - 10f).coerceAtLeast(140f)
                }
            }
        }

        val isCenteredText = logoPos == "CENTER" && !hasRightBadge
        val textOriginX = if (isCenteredText) (left + right) / 2f else textStartX
        if (isCenteredText) {
            boldPaint.textAlign = Paint.Align.CENTER
            grayPaint.textAlign = Paint.Align.CENTER
            textMaxWidth = right - left - 20f
        }

        // 1. Nom de l'entreprise
        boldPaint.textSize = 14.5f
        boldPaint.color = Color.rgb(2, 132, 199)
        val companyLines = wrapTextToWidth(settings.companyName.uppercase(), boldPaint, textMaxWidth)
        companyLines.forEach { line ->
            canvas.drawCleanText(line, textOriginX, currentTextY, boldPaint)
            currentTextY += 14.5f
        }

        // 2. Blocs de coordonnées selon l'ordre choisi par l'utilisateur
        val orderedBlocks = HeaderBlockType.fromOrderString(settings.pdfHeaderBlocksOrder)
        for (block in orderedBlocks) {
            when (block) {
                HeaderBlockType.SLOGAN -> {
                    if (settings.pdfShowSlogan && settings.activitySlogan.isNotBlank()) {
                        grayPaint.textSize = 8.6f
                        grayPaint.color = Color.rgb(71, 85, 105)
                        wrapTextToWidth(settings.activitySlogan, grayPaint, textMaxWidth).forEach { line ->
                            canvas.drawCleanText(line, textOriginX, currentTextY, grayPaint)
                            currentTextY += 10.5f
                        }
                    }
                }
                HeaderBlockType.CONTACT -> {
                    val phones = listOfNotNull(
                        settings.phone.ifBlank { null },
                        settings.phoneSecondary.ifBlank { null }
                    ).joinToString(" / ")
                    val contactParts = listOfNotNull(
                        if (settings.pdfShowAddress) settings.address.ifBlank { null } else null,
                        if (settings.pdfShowPhone && phones.isNotBlank()) "Tél: $phones" else null,
                        if (settings.pdfShowEmailWebsite) settings.email.ifBlank { null } else null,
                        if (settings.pdfShowEmailWebsite) settings.website.ifBlank { null } else null
                    ).joinToString(" • ")

                    if (contactParts.isNotBlank()) {
                        grayPaint.textSize = 8.1f
                        grayPaint.color = Color.rgb(100, 116, 139)
                        wrapTextToWidth(contactParts, grayPaint, textMaxWidth).forEach { line ->
                            canvas.drawCleanText(line, textOriginX, currentTextY, grayPaint)
                            currentTextY += 10f
                        }
                    }
                }
                HeaderBlockType.FISCAL -> {
                    if (settings.pdfShowFiscalIds) {
                        val fiscalParts = listOfNotNull(
                            settings.rc.ifBlank { null }?.let { "RC N°: $it" },
                            settings.articleImposition.ifBlank { null }?.let { "N° article: $it" },
                            settings.nif.ifBlank { null }?.let { "NIF N°: $it" },
                            settings.nis.ifBlank { null }?.let { "NIS N°: $it" },
                            settings.nin.ifBlank { null }?.let { "NIN N°: $it" }
                        ).joinToString("  •  ")
                        if (fiscalParts.isNotBlank()) {
                            grayPaint.textSize = 7.5f
                            grayPaint.color = Color.rgb(100, 116, 139)
                            wrapTextToWidth(fiscalParts, grayPaint, textMaxWidth).forEach { line ->
                                canvas.drawCleanText(line, textOriginX, currentTextY, grayPaint)
                                currentTextY += 9.5f
                            }
                        }
                    }
                }
                HeaderBlockType.BANQUE -> {
                    if (settings.pdfShowBankRib) {
                        val bankParts = listOfNotNull(
                            settings.bankName.ifBlank { null }?.let { "Banque: $it" },
                            settings.bankRib.ifBlank { null }?.let { "RIB: $it" }
                        ).joinToString("  •  ")
                        if (bankParts.isNotBlank()) {
                            grayPaint.textSize = 7.5f
                            grayPaint.color = Color.rgb(71, 85, 105)
                            wrapTextToWidth(bankParts, grayPaint, textMaxWidth).forEach { line ->
                                canvas.drawCleanText(line, textOriginX, currentTextY, grayPaint)
                                currentTextY += 9.5f
                            }
                        }
                    }
                }
            }
        }

        boldPaint.textAlign = Paint.Align.LEFT
        grayPaint.textAlign = Paint.Align.LEFT

        // 3. Badge du document à droite (si fourni)
        var rightHeaderEndY = y
        if (hasRightBadge && !rightBadgeTitle.isNullOrBlank()) {
            fillPaint.color = primaryColor
            val badgeRect = RectF(rightColumnLeft, y, right, y + 26f)
            canvas.drawRoundRect(badgeRect, 6f, 6f, fillPaint)

            boldPaint.color = Color.WHITE
            boldPaint.textSize = 11f
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawCleanText(fitTextToWidth(rightBadgeTitle, boldPaint, badgeRect.width() - 8f), badgeRect.centerX(), y + 17f, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT

            var rY = y + 41f
            if (!rightDocNumber.isNullOrBlank()) {
                boldPaint.color = Color.rgb(15, 23, 42)
                boldPaint.textSize = 10.2f
                canvas.drawCleanText(rightDocNumber, rightColumnLeft, rY, boldPaint)
                rY += 13f
            }
            if (!rightDateStr.isNullOrBlank()) {
                grayPaint.textSize = 8.6f
                grayPaint.color = Color.rgb(100, 116, 139)
                canvas.drawCleanText(rightDateStr, rightColumnLeft, rY, grayPaint)
                rY += 10f
            }
            rightHeaderEndY = rY
        }

        return maxOf(currentTextY + 4f, logoBottomY + 6f, rightHeaderEndY + 4f)
    }

    fun isConversionNoteLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("Document converti à partir", ignoreCase = true) ||
            trimmed.startsWith("Document converti a partir", ignoreCase = true) ||
            trimmed.startsWith("Converti depuis", ignoreCase = true) ||
            trimmed.startsWith("Facture regroupant les BL", ignoreCase = true) ||
            trimmed.startsWith("Bon de Livraison regroupant les Devis", ignoreCase = true)
    }

    fun extractConversionNote(notes: String): String? {
        return notes.lines()
            .map { it.trim() }
            .firstOrNull { isConversionNoteLine(it) }
    }

    fun stripConversionNotes(notes: String): String {
        return notes.lines()
            .filterNot { isConversionNoteLine(it) }
            .joinToString("\n")
            .trim()
    }

    fun generatePdf(
        context: Context,
        item: OperationWithLignes,
        settings: CompanySettings? = null,
        partner: PartnerEntity? = null,
        kitCompositionsMap: Map<String, List<ArticleCompositionEntity>> = emptyMap(),
        forcePrintKitComponents: Boolean = false,
        selectedColumns: List<DocPdfColumn>? = null,
        productCatalogMap: Map<String, ProductCatalogEntity> = emptyMap()
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val op = item.operation
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1)
            .setContentRect(android.graphics.Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT))
            .create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawDocumentPage(
            context = context,
            canvas = canvas,
            item = item,
            settings = activeSettings,
            partner = partner,
            kitCompositionsMap = kitCompositionsMap,
            forcePrintKitComponents = forcePrintKitComponents || activeSettings.pdfShowKitComponents,
            selectedColumns = selectedColumns ?: DocPdfColumn.fromOrderString(activeSettings.pdfDocColumnsOrder),
            productCatalogMap = productCatalogMap
        )

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val prefix = when (op.typeOperation) {
            OperationType.FACTURE -> "Facture"
            OperationType.FACTURE_FOURNISSEUR -> "FactureFournisseur"
            OperationType.BON_COMMANDE -> "BonCommande"
            OperationType.BL_CLIENT -> "BonLivraison"
            OperationType.BL_FOURNISSEUR -> "BonFournisseur"
            OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> "Devis"
            OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> "Avoir"
            OperationType.REGLEMENT -> "RecuReglement"
        }
        val sanitizedId = op.id.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(docsDir, "${prefix}_${sanitizedId}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    private fun drawDocumentPage(
        context: Context,
        canvas: Canvas,
        item: OperationWithLignes,
        settings: CompanySettings,
        partner: PartnerEntity? = null,
        kitCompositionsMap: Map<String, List<ArticleCompositionEntity>> = emptyMap(),
        forcePrintKitComponents: Boolean = false,
        selectedColumns: List<DocPdfColumn> = DocPdfColumn.defaultColumns(),
        productCatalogMap: Map<String, ProductCatalogEntity> = emptyMap()
    ) {
        val op = item.operation
        val isReglement = op.typeOperation == OperationType.REGLEMENT

        val primaryColor = when (op.typeOperation) {
            OperationType.FACTURE -> Color.rgb(217, 119, 6) // Amber/Gold #D97706
            OperationType.FACTURE_FOURNISSEUR -> Color.rgb(67, 56, 202) // Indigo #4338CA
            OperationType.BON_COMMANDE -> Color.rgb(124, 58, 237) // Purple #7C3AED
            OperationType.BL_CLIENT -> Color.rgb(2, 132, 199) // Sky Blue #0284C7
            OperationType.BL_FOURNISSEUR -> Color.rgb(99, 102, 241) // Indigo #6366F1
            OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> Color.rgb(14, 165, 233) // Cyan/Sky #0EA5E9
            OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> Color.rgb(225, 29, 72) // Rose/Red #E11D48
            OperationType.REGLEMENT -> Color.rgb(5, 150, 105) // Emerald #059669
        }

        val textPaint = createNormalPaint(Color.rgb(30, 41, 59))
        val boldPaint = createBoldPaint(Color.rgb(15, 23, 42))
        val grayPaint = createNormalPaint(Color.rgb(100, 116, 139))

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(203, 213, 225)
        }

        val left = 16f
        val right = PAGE_WIDTH - 16f
        val contentWidth = right - left

        val docTitle = when (op.typeOperation) {
            OperationType.FACTURE -> "FACTURE"
            OperationType.FACTURE_FOURNISSEUR -> "FACTURE FOURNISSEUR"
            OperationType.BON_COMMANDE -> "BON DE COMMANDE"
            OperationType.BL_CLIENT -> "BON DE LIVRAISON"
            OperationType.BL_FOURNISSEUR -> "BON DE RÉCEPTION"
            OperationType.DEVIS_CLIENT -> "DEVIS"
            OperationType.AVOIR_CLIENT -> "FACTURE D'AVOIR"
            OperationType.DEVIS_FOURNISSEUR -> "DEVIS FOURNISSEUR"
            OperationType.AVOIR_FOURNISSEUR -> "BON D'AVOIR FOURNISSEUR"
            OperationType.REGLEMENT -> "REÇU DE RÈGLEMENT"
        }

        var y = drawCompanyHeaderBlock(
            context = context,
            canvas = canvas,
            settings = settings,
            left = left,
            right = right,
            startY = 16f,
            primaryColor = primaryColor,
            rightBadgeTitle = docTitle,
            rightDocNumber = "N° ${op.id}",
            rightDateStr = "Date : ${DateUtils.formatDateTime(op.dateTime)}"
        )

        // Horizontal Separator
        canvas.drawLine(left, y, right, y, strokePaint)
        y += 12f

        // 2. Client / Destinataire Card
        val partnerFiscal = if (settings.pdfShowPartnerFiscal && partner != null) {
            listOfNotNull(
                partner.rc.ifBlank { null }?.let { "RC N°: $it" },
                partner.articleImposition.ifBlank { null }?.let { "AI: $it" },
                partner.nif.ifBlank { null }?.let { "NIF N°: $it" },
                partner.nis.ifBlank { null }?.let { "NIS N°: $it" },
                partner.nin.ifBlank { null }?.let { "NIN N°: $it" },
                partner.rib.ifBlank { null }?.let { "RIB: $it" }
            ).joinToString("  •  ")
        } else ""

        val partnerContact = partner?.let { p ->
            listOfNotNull(
                p.address.ifBlank { null },
                p.phone.ifBlank { null }?.let { "Tél: $it" }
            ).joinToString(" • ")
        } ?: ""

        val partnerBoxHeight = when {
            partnerFiscal.isNotBlank() && partnerContact.isNotBlank() -> 76f
            partnerFiscal.isNotBlank() || partnerContact.isNotBlank() -> 64f
            else -> 52f
        }
        fillPaint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(left, y, right, y + partnerBoxHeight), 8f, 8f, fillPaint)
        canvas.drawRoundRect(RectF(left, y, right, y + partnerBoxHeight), 8f, 8f, strokePaint)

        val partnerRole = if (op.typeOperation.isPurchaseDocument) "FOURNISSEUR" else "CLIENT / DESTINATAIRE"
        boldPaint.textSize = 9f
        boldPaint.color = primaryColor
        canvas.drawCleanText(partnerRole, left + 12f, y + 15f, boldPaint)

        if (settings.pdfShowPaymentMethod && op.modeReglement.isNotBlank()) {
            grayPaint.textSize = 8.5f
            grayPaint.textAlign = Paint.Align.RIGHT
            canvas.drawCleanText("Règlement : ${op.modeReglement}", right - 12f, y + 15f, grayPaint)
            grayPaint.textAlign = Paint.Align.LEFT
        }

        boldPaint.textSize = 13f
        boldPaint.color = Color.rgb(15, 23, 42)
        canvas.drawCleanText(op.clientFournisseur, left + 12f, y + 32f, boldPaint)

        var pInfoY = y + 46f
        if (partnerContact.isNotBlank()) {
            textPaint.textSize = 8.8f
            canvas.drawCleanText(partnerContact, left + 12f, pInfoY, textPaint)
            pInfoY += 12f
        }
        if (partnerFiscal.isNotBlank()) {
            grayPaint.textSize = 7.8f
            canvas.drawCleanText(partnerFiscal, left + 12f, pInfoY, grayPaint)
        } else if (partnerContact.isBlank() && settings.pdfShowPaymentMethod && op.modeReglement.isNotBlank()) {
            grayPaint.textSize = 8.8f
            canvas.drawCleanText("Mode de règlement prévu : ${op.modeReglement}", left + 12f, pInfoY, grayPaint)
        }

        y += partnerBoxHeight + 14f

        // 3. Table of Product Lines (avec intercaractère normal et colonnes réorganisables)
        val activeCols = selectedColumns.ifEmpty { DocPdfColumn.defaultColumns() }
        val totalColWeight = activeCols.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = activeCols.map { col -> contentWidth * (col.weight / totalColWeight) }

        val conversionNote = extractConversionNote(op.notes)
        val userNotesWithoutConversion = stripConversionNotes(op.notes)
        val showBottomConversionBanner = settings.pdfShowConversionNote && !conversionNote.isNullOrBlank()
        val conversionBannerReserve = if (showBottomConversionBanner) 14f else 0f

        val summaryBoxHeight = if (op.appliquerTva) 100f else 78f
        val showSignatureAndStampBlock = settings.pdfShowSignatures || settings.pdfShowStampAndSignature
        val stampScaleFactor = (settings.pdfStampScale.coerceIn(80, 250)) / 100f
        val signBoxH = (30f + 44f * stampScaleFactor).coerceAtLeast(80f)
        val arreteReserve = if (settings.pdfShowAmountInWords) 54f else 0f
        val signReserve = if (showSignatureAndStampBlock) (signBoxH + 10f) else 0f
        val maxTableBottomY = (PAGE_HEIGHT - 24f - conversionBannerReserve - summaryBoxHeight - arreteReserve - signReserve).coerceAtLeast(y + 90f)

        if (op.typeOperation.hasProductLines && item.lignes.isNotEmpty()) {
            val tableTopY = y
            val tableHeaderHeight = 24f
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(left, y, right, y + tableHeaderHeight, fillPaint)
            canvas.drawRect(left, y, right, y + tableHeaderHeight, strokePaint)

            boldPaint.textSize = 9.4f
            boldPaint.color = Color.rgb(51, 65, 85)

            var curX = left
            activeCols.forEachIndexed { idx, col ->
                val w = colWidths[idx]
                val headerTxt = fitTextToWidth(col.shortHeader, boldPaint, w - 8f)
                canvas.drawCleanText(headerTxt, curX + 5f, y + 16f, boldPaint)
                curX += w
            }

            y += tableHeaderHeight

            textPaint.textSize = 9.5f
            val rowHeight = 21f
            val groupedRows = groupLignesByParent(item.lignes)
            var hiddenGroupsCount = 0

            for ((index, group) in groupedRows.withIndex()) {
                val hasMoreAfter = index < groupedRows.lastIndex
                val rowLimitY = if (hasMoreAfter) maxTableBottomY - 18f else maxTableBottomY
                if (y + rowHeight > rowLimitY) {
                    hiddenGroupsCount = groupedRows.size - index
                    break
                }
                when (group) {
                    is GroupedOperationLine.ExplodedParentGroup -> {
                        fillPaint.color = Color.rgb(245, 243, 255)
                        canvas.drawRect(left, y, right, y + rowHeight, fillPaint)
                        canvas.drawRect(left, y, right, y + rowHeight, strokePaint)

                        var parentX = left
                        activeCols.forEachIndexed { colIdx, col ->
                            val w = colWidths[colIdx]
                            val maxW = w - 8f
                            when (col) {
                                DocPdfColumn.NUMERO -> {
                                    boldPaint.textSize = 9f
                                    boldPaint.color = Color.rgb(91, 33, 182)
                                    canvas.drawCleanText("${group.displayIndex}", parentX + 5f, y + 14f, boldPaint)
                                }
                                DocPdfColumn.REFERENCE -> {
                                    val ref = productCatalogMap[group.parentName.trim().lowercase(Locale.FRENCH)]?.reference ?: "-"
                                    grayPaint.textSize = 8.4f
                                    canvas.drawCleanText(fitTextToWidth(ref, grayPaint, maxW), parentX + 5f, y + 14f, grayPaint)
                                }
                                DocPdfColumn.DESIGNATION -> {
                                    boldPaint.textSize = 9f
                                    boldPaint.color = Color.rgb(91, 33, 182)
                                    canvas.drawCleanText(fitTextToWidth("[Parent] ${group.parentName}", boldPaint, maxW), parentX + 5f, y + 14f, boldPaint)
                                }
                                DocPdfColumn.QUANTITE -> {
                                    boldPaint.textSize = 9f
                                    boldPaint.color = Color.rgb(91, 33, 182)
                                    canvas.drawCleanText("1", parentX + 5f, y + 14f, boldPaint)
                                }
                                DocPdfColumn.UNITE -> {
                                    grayPaint.textSize = 8.6f
                                    canvas.drawCleanText("Lot", parentX + 5f, y + 14f, grayPaint)
                                }
                                DocPdfColumn.PU_HT -> {
                                    grayPaint.textSize = 8.4f
                                    canvas.drawCleanText(fitTextToWidth("Somme fils", grayPaint, maxW), parentX + 5f, y + 14f, grayPaint)
                                }
                                DocPdfColumn.TOTAL_HT -> {
                                    boldPaint.textSize = 9f
                                    boldPaint.color = Color.rgb(91, 33, 182)
                                    canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatQty(group.totalHt), boldPaint, maxW), parentX + 5f, y + 14f, boldPaint)
                                }
                            }
                            parentX += w
                        }
                        y += rowHeight

                        val childRowHeight = 18f
                        for ((cIdx, child) in group.children.withIndex()) {
                            if (y + childRowHeight > maxTableBottomY) break
                            if (cIdx % 2 == 1) {
                                fillPaint.color = Color.rgb(250, 250, 255)
                                canvas.drawRect(left, y, right, y + childRowHeight, fillPaint)
                            }
                            canvas.drawRect(left, y, right, y + childRowHeight, strokePaint)

                            var childX = left
                            val branch = if (cIdx == group.children.lastIndex) "└─" else "├─"
                            activeCols.forEachIndexed { colIdx, col ->
                                val w = colWidths[colIdx]
                                val maxW = w - 8f
                                when (col) {
                                    DocPdfColumn.NUMERO -> {
                                        grayPaint.textSize = 8.4f
                                        canvas.drawCleanText("${group.displayIndex}.${cIdx + 1}", childX + 5f, y + 13f, grayPaint)
                                    }
                                    DocPdfColumn.REFERENCE -> {
                                        val ref = productCatalogMap[child.produit.trim().lowercase(Locale.FRENCH)]?.reference ?: "-"
                                        grayPaint.textSize = 8.2f
                                        canvas.drawCleanText(fitTextToWidth(ref, grayPaint, maxW), childX + 5f, y + 13f, grayPaint)
                                    }
                                    DocPdfColumn.DESIGNATION -> {
                                        textPaint.textSize = 8.8f
                                        canvas.drawCleanText(fitTextToWidth("$branch ${child.produit}", textPaint, maxW), childX + 8f, y + 13f, textPaint)
                                    }
                                    DocPdfColumn.QUANTITE -> {
                                        textPaint.textSize = 8.8f
                                        canvas.drawCleanText(CurrencyUtils.formatQty(child.quantite), childX + 5f, y + 13f, textPaint)
                                    }
                                    DocPdfColumn.UNITE -> {
                                        val unit = productCatalogMap[child.produit.trim().lowercase(Locale.FRENCH)]?.unit ?: "U"
                                        grayPaint.textSize = 8.4f
                                        canvas.drawCleanText(unit, childX + 5f, y + 13f, grayPaint)
                                    }
                                    DocPdfColumn.PU_HT -> {
                                        textPaint.textSize = 8.8f
                                        val puText = child.prixUnitaire?.let { CurrencyUtils.formatQty(it) } ?: "-"
                                        canvas.drawCleanText(fitTextToWidth(puText, textPaint, maxW), childX + 5f, y + 13f, textPaint)
                                    }
                                    DocPdfColumn.TOTAL_HT -> {
                                        boldPaint.textSize = 8.8f
                                        boldPaint.color = Color.rgb(15, 23, 42)
                                        canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatQty(child.totalLigne), boldPaint, maxW), childX + 5f, y + 13f, boldPaint)
                                    }
                                }
                                childX += w
                            }
                            y += childRowHeight
                        }
                    }

                    is GroupedOperationLine.StandardLine -> {
                        val ligne = group.ligne
                        if (index % 2 == 1) {
                            fillPaint.color = Color.rgb(248, 250, 252)
                            canvas.drawRect(left, y, right, y + rowHeight, fillPaint)
                        }
                        canvas.drawRect(left, y, right, y + rowHeight, strokePaint)

                        val matchedCatalog = productCatalogMap[ligne.produit.trim().lowercase(Locale.FRENCH)]
                        var cellX = left
                        activeCols.forEachIndexed { colIdx, col ->
                            val w = colWidths[colIdx]
                            val maxW = w - 8f
                            when (col) {
                                DocPdfColumn.NUMERO -> {
                                    textPaint.textSize = 9.2f
                                    canvas.drawCleanText("${group.displayIndex}", cellX + 5f, y + 14f, textPaint)
                                }
                                DocPdfColumn.REFERENCE -> {
                                    val ref = matchedCatalog?.reference?.ifBlank { matchedCatalog.barcode }?.ifBlank { "-" } ?: "-"
                                    grayPaint.textSize = 8.6f
                                    canvas.drawCleanText(fitTextToWidth(ref, grayPaint, maxW), cellX + 5f, y + 14f, grayPaint)
                                }
                                DocPdfColumn.DESIGNATION -> {
                                    textPaint.textSize = 9.2f
                                    canvas.drawCleanText(fitTextToWidth(ligne.produit, textPaint, maxW), cellX + 5f, y + 14f, textPaint)
                                }
                                DocPdfColumn.QUANTITE -> {
                                    textPaint.textSize = 9.2f
                                    canvas.drawCleanText(CurrencyUtils.formatQty(ligne.quantite), cellX + 5f, y + 14f, textPaint)
                                }
                                DocPdfColumn.UNITE -> {
                                    val unit = matchedCatalog?.unit?.ifBlank { "U" } ?: "U"
                                    grayPaint.textSize = 8.8f
                                    canvas.drawCleanText(unit, cellX + 5f, y + 14f, grayPaint)
                                }
                                DocPdfColumn.PU_HT -> {
                                    textPaint.textSize = 9.2f
                                    val puText = ligne.prixUnitaire?.let { CurrencyUtils.formatQty(it) } ?: "-"
                                    canvas.drawCleanText(fitTextToWidth(puText, textPaint, maxW), cellX + 5f, y + 14f, textPaint)
                                }
                                DocPdfColumn.TOTAL_HT -> {
                                    boldPaint.textSize = 9.2f
                                    boldPaint.color = Color.rgb(15, 23, 42)
                                    canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatQty(ligne.totalLigne), boldPaint, maxW), cellX + 5f, y + 14f, boldPaint)
                                }
                            }
                            cellX += w
                        }

                        y += rowHeight

                        if (forcePrintKitComponents) {
                            val kitComps = kitCompositionsMap[ligne.produit.trim().lowercase(Locale.FRENCH)]
                            if (!kitComps.isNullOrEmpty()) {
                                val subRowHeight = 16f
                                for (comp in kitComps) {
                                    if (y + subRowHeight > maxTableBottomY) break
                                    fillPaint.color = Color.rgb(240, 253, 250)
                                    canvas.drawRect(left, y, right, y + subRowHeight, fillPaint)
                                    canvas.drawRect(left, y, right, y + subRowHeight, strokePaint)

                                    var subX = left
                                    activeCols.forEachIndexed { colIdx, col ->
                                        val w = colWidths[colIdx]
                                        val maxW = w - 8f
                                        grayPaint.textSize = 8.2f
                                        when (col) {
                                            DocPdfColumn.NUMERO -> {}
                                            DocPdfColumn.REFERENCE -> canvas.drawCleanText("-", subX + 5f, y + 11.5f, grayPaint)
                                            DocPdfColumn.DESIGNATION -> canvas.drawCleanText(fitTextToWidth("↳ ${comp.componentName}", grayPaint, maxW), subX + 8f, y + 11.5f, grayPaint)
                                            DocPdfColumn.QUANTITE -> canvas.drawCleanText(CurrencyUtils.formatQty(comp.quantite * ligne.quantite), subX + 5f, y + 11.5f, grayPaint)
                                            DocPdfColumn.UNITE -> canvas.drawCleanText(comp.unit, subX + 5f, y + 11.5f, grayPaint)
                                            DocPdfColumn.PU_HT -> canvas.drawCleanText("Inclus", subX + 5f, y + 11.5f, grayPaint)
                                            DocPdfColumn.TOTAL_HT -> canvas.drawCleanText("Kit", subX + 5f, y + 11.5f, grayPaint)
                                        }
                                        subX += w
                                    }
                                    y += subRowHeight
                                }
                            }
                        }
                    }
                }
            }

            if (hiddenGroupsCount > 0) {
                val moreRowH = 16f
                fillPaint.color = Color.rgb(241, 245, 249)
                canvas.drawRect(left, y, right, y + moreRowH, fillPaint)
                canvas.drawRect(left, y, right, y + moreRowH, strokePaint)
                grayPaint.textSize = 8.2f
                grayPaint.color = Color.rgb(71, 85, 105)
                canvas.drawCleanText(
                    "... + $hiddenGroupsCount autre(s) ligne(s) d'articles incluse(s) dans le Total Brut HT ci-dessous",
                    left + 8f,
                    y + 11.5f,
                    grayPaint
                )
                y += moreRowH
            }

            // Extend table frame and column vertical grid lines to maxTableBottomY for full-page A4 layout in Adobe Acrobat
            val finalTableBottomY = maxOf(y, maxTableBottomY)
            canvas.drawRect(left, tableTopY, right, finalTableBottomY, strokePaint)
            var colLineX = left
            for (colIdx in 0 until activeCols.size - 1) {
                colLineX += colWidths[colIdx]
                canvas.drawLine(colLineX, tableTopY, colLineX, finalTableBottomY, strokePaint)
            }
            y = finalTableBottomY
        } else if (isReglement) {
            fillPaint.color = Color.rgb(240, 253, 244)
            canvas.drawRoundRect(RectF(left, y, right, y + 60f), 8f, 8f, fillPaint)
            canvas.drawRoundRect(RectF(left, y, right, y + 60f), 8f, 8f, strokePaint)

            boldPaint.textSize = 12f
            boldPaint.color = Color.rgb(21, 128, 61)
            canvas.drawCleanText("Règlement Encaissé : ${CurrencyUtils.formatDa(op.montantVerse)}", left + 16f, y + 26f, boldPaint)

            grayPaint.textSize = 10f
            canvas.drawCleanText("Mode de paiement : ${op.modeReglement} • Reçu conforme", left + 16f, y + 44f, grayPaint)

            y += 74f
        }

        y += 10f

        // 4. Totaux & TVA Summary Block (Bottom Right)
        val summaryWidth = 230f
        val summaryLeft = right - summaryWidth

        fillPaint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(summaryLeft, y, right, y + summaryBoxHeight), 8f, 8f, fillPaint)
        canvas.drawRoundRect(RectF(summaryLeft, y, right, y + summaryBoxHeight), 8f, 8f, strokePaint)

        var sumY = y + 18f
        textPaint.textSize = 9.5f
        boldPaint.textSize = 9.5f
        boldPaint.color = Color.rgb(15, 23, 42)

        canvas.drawCleanText("Total Brut HT :", summaryLeft + 12f, sumY, textPaint)
        val htText = CurrencyUtils.formatDa(item.totalHt)
        canvas.drawCleanText(htText, right - 12f - measureCleanText(boldPaint, htText), sumY, boldPaint)
        sumY += 16f

        if (op.appliquerTva) {
            canvas.drawCleanText("TVA (${op.tauxTva}%) :", summaryLeft + 12f, sumY, textPaint)
            val tvaText = CurrencyUtils.formatDa(item.montantTva)
            canvas.drawCleanText(tvaText, right - 12f - measureCleanText(boldPaint, tvaText), sumY, boldPaint)
            sumY += 16f
        }

        canvas.drawLine(summaryLeft + 8f, sumY - 4f, right - 8f, sumY - 4f, strokePaint)
        boldPaint.textSize = 11f
        boldPaint.color = primaryColor
        val ttcLabel = if (op.appliquerTva) "Total Net TTC :" else "Total Net à Payer :"
        canvas.drawCleanText(ttcLabel, summaryLeft + 12f, sumY + 8f, boldPaint)
        val ttcText = CurrencyUtils.formatDa(item.totalTtc)
        canvas.drawCleanText(ttcText, right - 12f - measureCleanText(boldPaint, ttcText), sumY + 8f, boldPaint)
        sumY += 20f

        if (op.montantVerse > 0.0) {
            textPaint.textSize = 9f
            canvas.drawCleanText("Versement / Acompte :", summaryLeft + 12f, sumY, textPaint)
            val verseStr = CurrencyUtils.formatDa(op.montantVerse)
            canvas.drawCleanText(verseStr, right - 12f - measureCleanText(textPaint, verseStr), sumY, textPaint)
            sumY += 14f

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.rgb(220, 38, 38)
            canvas.drawCleanText("Reste à Payer :", summaryLeft + 12f, sumY, boldPaint)
            val resteStr = CurrencyUtils.formatDa(item.resteAPayer)
            canvas.drawCleanText(resteStr, right - 12f - measureCleanText(boldPaint, resteStr), sumY, boldPaint)
        }

        // Left of totals: Notes and mentions (sans la note de conversion si elle est masquée pour le PDF)
        val notesForBox = if (settings.pdfShowConversionNote) op.notes else userNotesWithoutConversion
        if (settings.pdfShowNotes && notesForBox.isNotBlank()) {
            val notesBox = RectF(left, y, summaryLeft - 14f, y + summaryBoxHeight)
            fillPaint.color = Color.rgb(248, 250, 252)
            canvas.drawRoundRect(notesBox, 8f, 8f, fillPaint)
            canvas.drawRoundRect(notesBox, 8f, 8f, strokePaint)

            boldPaint.textSize = 9f
            boldPaint.color = Color.rgb(71, 85, 105)
            canvas.drawCleanText("Notes & Remarques :", left + 10f, y + 16f, boldPaint)

            grayPaint.textSize = 8.8f
            val wrappedNotes = wrapTextToWidth(notesForBox, grayPaint, notesBox.width() - 20f)
            var noteY = y + 30f
            wrappedNotes.take(4).forEach { nLine ->
                canvas.drawCleanText(nLine, left + 10f, noteY, grayPaint)
                noteY += 11f
            }
        }

        y += summaryBoxHeight + 8f

        // 4bis. Mention légale : Montant en lettres avec intercaractère normal
        if (settings.pdfShowAmountInWords) {
            val amountForWords = if (isReglement) op.montantVerse else item.totalTtc
            val arretePrefix = CurrencyUtils.formatArretePrefix(op.typeOperation)
            val wordsAmount = CurrencyUtils.amountToFrenchWordsDa(amountForWords)

            boldPaint.textSize = 9.2f
            val wrappedWords = wrapTextToWidth(wordsAmount, boldPaint, contentWidth - 20f)
            val arreteBoxHeight = 28f + (wrappedWords.size * 12.5f)

            fillPaint.color = Color.rgb(241, 245, 249)
            val arreteBox = RectF(left, y, right, y + arreteBoxHeight)
            canvas.drawRoundRect(arreteBox, 6f, 6f, fillPaint)
            canvas.drawRoundRect(arreteBox, 6f, 6f, strokePaint)

            grayPaint.textSize = 8.6f
            grayPaint.color = Color.rgb(71, 85, 105)
            canvas.drawCleanText(arretePrefix, left + 10f, y + 14f, grayPaint)

            boldPaint.color = Color.rgb(15, 23, 42)
            var wordsY = y + 27f
            wrappedWords.forEach { lineStr ->
                canvas.drawCleanText(lineStr, left + 10f, wordsY, boldPaint)
                wordsY += 12.5f
            }

            y += arreteBoxHeight + 8f
        }

        // 5. Cachet Humide PNG Block (Devis, Factures, Bons, Règlements) — sans signature stylo
        if (showSignatureAndStampBlock) {
            val signBoxW = (contentWidth - 20f) / 2f
            val signTopY = y.coerceAtMost(PAGE_HEIGHT - signBoxH - (if (showBottomConversionBanner) 36f else 26f))
            val maxSignTextWidth = signBoxW - 16f

            val boxClient = RectF(left, signTopY, left + signBoxW, signTopY + signBoxH)
            canvas.drawRoundRect(boxClient, 6f, 6f, strokePaint)
            boldPaint.textSize = 8.2f
            boldPaint.color = Color.rgb(100, 116, 139)
            val clientTitle = when (op.typeOperation) {
                OperationType.REGLEMENT -> "VISA DU PAYEUR / CLIENT"
                OperationType.DEVIS_CLIENT, OperationType.DEVIS_FOURNISSEUR -> "BON POUR ACCORD & VISA CLIENT"
                OperationType.FACTURE, OperationType.FACTURE_FOURNISSEUR, OperationType.AVOIR_CLIENT, OperationType.AVOIR_FOURNISSEUR -> "VISA & ACCUSÉ DE RÉCEPTION"
                else -> "ACCUSÉ DE RÉCEPTION & VISA CLIENT"
            }
            val clientSignLines = wrapTextToWidth(clientTitle, boldPaint, maxSignTextWidth)
            var clientSignY = signTopY + 13f
            clientSignLines.take(2).forEach { line ->
                canvas.drawCleanText(line, boxClient.left + 8f, clientSignY, boldPaint)
                clientSignY += 10f
            }

            val boxMerelec = RectF(left + signBoxW + 20f, signTopY, right, signTopY + signBoxH)
            canvas.drawRoundRect(boxMerelec, 6f, 6f, strokePaint)
            boldPaint.textSize = 8.2f
            val merelecSignTitle = "POUR ${settings.companyName.uppercase()} - CACHET HUMIDE"
            val merelecSignLines = wrapTextToWidth(merelecSignTitle, boldPaint, maxSignTextWidth)
            var merelecSignY = signTopY + 13f
            merelecSignLines.take(2).forEach { line ->
                canvas.drawCleanText(line, boxMerelec.left + 8f, merelecSignY, boldPaint)
                merelecSignY += 10f
            }

            drawWetStampAndSignatureInBox(
                context = context,
                canvas = canvas,
                settings = settings,
                box = boxMerelec,
                titleBottomY = merelecSignY - 4f
            )
        }

        // Mention en bas du document converti (BL ou Facture) : "Document converti à partir de la facture N° ..." (masquable via pdfShowConversionNote)
        if (showBottomConversionBanner && !conversionNote.isNullOrBlank()) {
            boldPaint.textSize = 8.4f
            boldPaint.color = Color.rgb(67, 56, 202)
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawCleanText(
                fitTextToWidth(conversionNote, boldPaint, contentWidth),
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 22f,
                boldPaint
            )
            boldPaint.textAlign = Paint.Align.LEFT
        }

        // Footer at page bottom
        grayPaint.textSize = 7.8f
        grayPaint.textAlign = Paint.Align.CENTER
        val footerText = if (settings.pdfCustomFooter.isNotBlank()) {
            "${settings.pdfCustomFooter} • ${settings.companyName} (${DateUtils.formatDateTime(System.currentTimeMillis())})"
        } else {
            "${settings.companyName} • Document généré le ${DateUtils.formatDateTime(System.currentTimeMillis())} • Logiciel de Gestion Commerciale"
        }
        canvas.drawCleanText(
            fitTextToWidth(footerText, grayPaint, contentWidth),
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 10f,
            grayPaint
        )
    }

    fun generatePartnerStatementPdf(
        context: Context,
        partner: PartnerEntity,
        operations: List<OperationWithLignes>,
        totalBl: Double,
        totalReglements: Double,
        soldeRestantDu: Double,
        settings: CompanySettings? = null
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1)
            .setContentRect(android.graphics.Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT))
            .create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawStatementPage(context, canvas, partner, operations, totalBl, totalReglements, soldeRestantDu, activeSettings)

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val typePrefix = if (partner.type == PartnerType.CLIENT) "Releve_Client" else "Releve_Fournisseur"
        val sanitizedName = partner.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(docsDir, "${typePrefix}_${sanitizedName}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    private fun drawStatementPage(
        context: Context,
        canvas: Canvas,
        partner: PartnerEntity,
        operations: List<OperationWithLignes>,
        totalBl: Double,
        totalReglements: Double,
        soldeRestantDu: Double,
        settings: CompanySettings
    ) {
        val isClient = partner.type == PartnerType.CLIENT
        val primaryColor = if (isClient) Color.rgb(2, 132, 199) else Color.rgb(99, 102, 241)

        val textPaint = createNormalPaint(Color.rgb(30, 41, 59))
        val boldPaint = createBoldPaint(Color.rgb(15, 23, 42))
        val grayPaint = createNormalPaint(Color.rgb(100, 116, 139))

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(203, 213, 225)
        }

        val left = 18f
        val right = PAGE_WIDTH - 18f
        val contentWidth = right - left
        var y = 18f

        val badgeTitle = if (isClient) "RELEVÉ CLIENT" else "RELEVÉ FOURNISSEUR"
        y = drawCompanyHeaderBlock(
            context = context,
            canvas = canvas,
            settings = settings,
            left = left,
            right = right,
            startY = y,
            primaryColor = primaryColor,
            rightBadgeTitle = badgeTitle,
            rightDocNumber = partner.name.take(18),
            rightDateStr = "Édité le ${DateUtils.formatDate(System.currentTimeMillis())}"
        )

        canvas.drawLine(left, y, right, y, strokePaint)
        y += 12f

        // Partner Box
        val fiscalLine = if (settings.pdfShowPartnerFiscal) {
            listOfNotNull(
                partner.rc.ifBlank { null }?.let { "RC N°: $it" },
                partner.articleImposition.ifBlank { null }?.let { "N° article: $it" },
                partner.nif.ifBlank { null }?.let { "NIF N°: $it" },
                partner.nis.ifBlank { null }?.let { "NIS N°: $it" },
                partner.nin.ifBlank { null }?.let { "NIN N°: $it" },
                partner.rib.ifBlank { null }?.let { "RIB: $it" }
            ).joinToString("  •  ")
        } else ""
        val hasFiscal = fiscalLine.isNotBlank()
        val boxHeight = if (hasFiscal) 66f else 52f
        val partnerBox = RectF(left, y, right, y + boxHeight)
        fillPaint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(partnerBox, 8f, 8f, fillPaint)
        canvas.drawRoundRect(partnerBox, 8f, 8f, strokePaint)

        boldPaint.textSize = 10.5f
        boldPaint.color = Color.rgb(15, 23, 42)
        canvas.drawCleanText("Partenaire : ${partner.name}", left + 14f, y + 18f, boldPaint)

        textPaint.textSize = 8.5f
        val contactLine = listOfNotNull(
            partner.phone.ifBlank { null }?.let { "Tél: $it" },
            partner.address.ifBlank { null }?.let { "Adresse: $it" }
        ).joinToString("  •  ").ifEmpty { "Aucune coordonnée spécifique enregistrée" }
        canvas.drawCleanText(contactLine, left + 14f, y + 34f, textPaint)

        if (hasFiscal) {
            grayPaint.textSize = 7.5f
            grayPaint.color = Color.rgb(71, 85, 105)
            canvas.drawCleanText(fiscalLine, left + 14f, y + 49f, grayPaint)
        }

        y += boxHeight + 12f

        // 3 Summary Cards: Total Facturé/Livré | Total Règlements | Solde Restant Dû
        if (settings.pdfCreanceShowSummary) {
            val cardW = (contentWidth - 20f) / 3f
            val cardH = 44f

            val c1 = RectF(left, y, left + cardW, y + cardH)
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(c1, 6f, 6f, fillPaint)
            canvas.drawRoundRect(c1, 6f, 6f, strokePaint)
            grayPaint.textSize = 8f
            canvas.drawCleanText(if (isClient) "TOTAL FACTURÉ / BL" else "TOTAL ACHATS", left + 8f, y + 15f, grayPaint)
            boldPaint.textSize = 11f
            boldPaint.color = primaryColor
            canvas.drawCleanText(CurrencyUtils.formatDa(totalBl), left + 8f, y + 33f, boldPaint)

            val c2Left = left + cardW + 10f
            val c2 = RectF(c2Left, y, c2Left + cardW, y + cardH)
            canvas.drawRoundRect(c2, 6f, 6f, fillPaint)
            canvas.drawRoundRect(c2, 6f, 6f, strokePaint)
            canvas.drawCleanText("TOTAL RÈGLEMENTS", c2Left + 8f, y + 15f, grayPaint)
            boldPaint.color = Color.rgb(5, 150, 105)
            canvas.drawCleanText(CurrencyUtils.formatDa(totalReglements), c2Left + 8f, y + 33f, boldPaint)

            val c3Left = c2Left + cardW + 10f
            val c3 = RectF(c3Left, y, right, y + cardH)
            fillPaint.color = if (soldeRestantDu > 0) Color.rgb(254, 242, 242) else Color.rgb(240, 253, 244)
            canvas.drawRoundRect(c3, 6f, 6f, fillPaint)
            strokePaint.color = if (soldeRestantDu > 0) Color.rgb(252, 165, 165) else Color.rgb(187, 247, 208)
            canvas.drawRoundRect(c3, 6f, 6f, strokePaint)
            strokePaint.color = Color.rgb(203, 213, 225)
            canvas.drawCleanText(if (isClient) "SOLDE RESTANT DÛ" else "SOLDE EN COURS", c3Left + 8f, y + 15f, grayPaint)
            boldPaint.color = if (soldeRestantDu > 0) Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
            canvas.drawCleanText(CurrencyUtils.formatDa(soldeRestantDu), c3Left + 8f, y + 33f, boldPaint)

            y += cardH + 14f
        }

        // Table Header
        val colDate = left + 6f
        val colRef = left + 96f
        val colType = left + 190f
        val colDebit = right - 180f
        val colCredit = right - 92f

        val headerH = 22f
        fillPaint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(left, y, right, y + headerH, fillPaint)
        canvas.drawLine(left, y + headerH, right, y + headerH, strokePaint)

        boldPaint.textSize = 8.5f
        boldPaint.color = Color.rgb(71, 85, 105)
        canvas.drawCleanText("DATE", colDate, y + 15f, boldPaint)
        canvas.drawCleanText("RÉFÉRENCE", colRef, y + 15f, boldPaint)
        canvas.drawCleanText("NATURE", colType, y + 15f, boldPaint)
        canvas.drawCleanText(if (isClient) "DÉBIT / CRÉANCE" else "DÉBIT / DETTE", colDebit, y + 15f, boldPaint)
        canvas.drawCleanText("CRÉDIT (REG)", colCredit, y + 15f, boldPaint)

        y += headerH

        val showSignatureAndStampBlock = settings.pdfShowSignatures || settings.pdfShowStampAndSignature
        val stampScaleFactor = (settings.pdfStampScale.coerceIn(80, 250)) / 100f
        val signBoxH = (30f + 44f * stampScaleFactor).coerceAtLeast(76f)
        val maxTableY = if (showSignatureAndStampBlock) PAGE_HEIGHT - signBoxH - 42f else PAGE_HEIGHT - 44f

        val filteredOps = operations.filterNot {
            com.example.data.repository.OperationRepository.isImportedCreanceOperation(it.operation)
        }
        val sortedOps = filteredOps.sortedBy { it.operation.dateTime }

        val opsNetRestant = (totalBl - totalReglements).coerceAtLeast(0.0)
        val baseCreance = (soldeRestantDu - opsNetRestant).coerceAtLeast(0.0)
        val rowH = 22f
        var rowIndex = 0

        // Si le client/fournisseur possède une créance restante importée (hors BL), on l'affiche comme "Créance restante" (jamais comme BL)
        if (baseCreance > 0.01) {
            val textY = y + 14.5f
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(51, 65, 85)

            canvas.drawCleanText(DateUtils.formatDate(System.currentTimeMillis()), colDate, textY, textPaint)
            canvas.drawCleanText("SOLDE-INIT", colRef, textY, textPaint)
            canvas.drawCleanText(if (isClient) "Créance restante" else "Dette fournisseur", colType, textY, textPaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.rgb(220, 38, 38)
            canvas.drawCleanText(CurrencyUtils.formatDa(baseCreance), colDebit, textY, boldPaint)
            canvas.drawCleanText("-", colCredit + 10f, textY, textPaint)

            y += rowH
            canvas.drawLine(left, y, right, y, strokePaint)
            rowIndex++
        }

        val maxRows = 20
        val displayedOps = sortedOps.take(maxRows)

        for (opWithLignes in displayedOps) {
            if (y + rowH > maxTableY) break
            val op = opWithLignes.operation
            val isDebit = op.typeOperation != OperationType.REGLEMENT
            val amount = if (isDebit) opWithLignes.totalTtc else op.montantVerse

            if (rowIndex % 2 == 1) {
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(left, y, right, y + rowH, fillPaint)
            }

            val textY = y + 14.5f
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(51, 65, 85)

            canvas.drawCleanText(DateUtils.formatDate(op.dateTime), colDate, textY, textPaint)
            canvas.drawCleanText(op.id.take(14), colRef, textY, textPaint)
            canvas.drawCleanText(op.typeOperation.label, colType, textY, textPaint)

            if (isDebit) {
                boldPaint.textSize = 8.5f
                boldPaint.color = Color.rgb(15, 23, 42)
                val debitStr = CurrencyUtils.formatDa(amount)
                canvas.drawCleanText(debitStr, colDebit, textY, boldPaint)
                canvas.drawCleanText("-", colCredit + 10f, textY, textPaint)
            } else {
                canvas.drawCleanText("-", colDebit + 10f, textY, textPaint)
                boldPaint.textSize = 8.5f
                boldPaint.color = Color.rgb(5, 150, 105)
                val creditStr = CurrencyUtils.formatDa(amount)
                canvas.drawCleanText(creditStr, colCredit, textY, boldPaint)
            }

            y += rowH
            canvas.drawLine(left, y, right, y, strokePaint)
            rowIndex++
        }

        if (sortedOps.size > maxRows) {
            grayPaint.textSize = 8f
            canvas.drawCleanText("... et ${sortedOps.size - maxRows} autre(s) opération(s) non affichée(s) sur cette page.", left + 6f, y + 14f, grayPaint)
            y += 20f
        }

        if (showSignatureAndStampBlock) {
            y = (PAGE_HEIGHT - signBoxH - 26f).coerceAtLeast(y + 12f)
            val signBoxW = (contentWidth - 20f) / 2f
            val maxSignW = signBoxW - 16f

            val boxVisa = RectF(left, y, left + signBoxW, y + signBoxH)
            canvas.drawRoundRect(boxVisa, 6f, 6f, strokePaint)
            boldPaint.textSize = 8f
            boldPaint.color = Color.rgb(100, 116, 139)
            wrapTextToWidth("VISA PARTENAIRE / BON POUR ACCORD", boldPaint, maxSignW).take(2).forEachIndexed { idx, line ->
                canvas.drawCleanText(line, boxVisa.left + 8f, y + 13f + idx * 10f, boldPaint)
            }

            val boxStp = RectF(left + signBoxW + 20f, y, right, y + signBoxH)
            canvas.drawRoundRect(boxStp, 6f, 6f, strokePaint)
            val stpLines = wrapTextToWidth("POUR ${settings.companyName.uppercase()} - CACHET HUMIDE", boldPaint, maxSignW).take(2)
            var stpBottomY = y + 13f
            stpLines.forEach { line ->
                canvas.drawCleanText(line, boxStp.left + 8f, stpBottomY, boldPaint)
                stpBottomY += 10f
            }

            drawWetStampAndSignatureInBox(
                context = context,
                canvas = canvas,
                settings = settings,
                box = boxStp,
                titleBottomY = stpBottomY - 4f
            )
        }

        grayPaint.textSize = 7.8f
        grayPaint.textAlign = Paint.Align.CENTER
        val footerText = if (settings.pdfCustomFooter.isNotBlank()) {
            "${settings.pdfCustomFooter} • ${settings.companyName}"
        } else {
            "${settings.companyName} • Relevé de compte certifié conforme • Logiciel de Gestion"
        }
        canvas.drawCleanText(
            fitTextToWidth(footerText, grayPaint, contentWidth),
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 16f,
            grayPaint
        )
    }

    private fun fitTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty() || maxWidth <= 8f) return ""
        if (measureCleanText(paint, text) <= maxWidth) return text
        var truncated = text
        while (truncated.length > 1 && measureCleanText(paint, "$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    /**
     * Génère la Liste / Catalogue d'Articles en PDF (A4) avec logo, coordonnées réorganisées et choix/ordre dynamique des colonnes.
     */
    fun generateArticlesCatalogPdf(
        context: Context,
        products: List<ProductCatalogEntity>,
        settings: CompanySettings? = null,
        selectedColumns: List<ArticlePdfColumn>? = null
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val activeColumns = (selectedColumns ?: ArticlePdfColumn.fromOrderString(activeSettings.pdfArticleColumnsOrder))
            .ifEmpty { ArticlePdfColumn.defaultColumns() }
        val pdfDocument = PdfDocument()

        val left = 18f
        val right = PAGE_WIDTH - 18f
        val contentWidth = right - left

        val itemsPerPage = 25
        val sortedProducts = products
        val totalPages = if (sortedProducts.isEmpty()) 1 else (sortedProducts.size + itemsPerPage - 1) / itemsPerPage

        val totalStockValuation = sortedProducts.sumOf {
            val unitPrice = it.prixVente ?: it.defaultUnitPrice ?: it.prixAchat ?: 0.0
            unitPrice * it.stockActuel
        }
        val totalStockQty = sortedProducts.sumOf { it.stockActuel }

        val boldPaint = Paint().apply { isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val textPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(30, 41, 59) }
        val grayPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(100, 116, 139) }
        val fillPaint = Paint().apply { isAntiAlias = true }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(226, 232, 240)
        }

        val primaryColor = Color.rgb(0, 159, 227)
        val totalWeight = activeColumns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = activeColumns.map { col -> contentWidth * (col.weight / totalWeight) }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1)
                .setContentRect(android.graphics.Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT))
                .create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = 18f

            // En-tête entreprise avec Logo & Coordonnées réorganisées
            y = drawCompanyHeaderBlock(
                context = context,
                canvas = canvas,
                settings = activeSettings,
                left = left,
                right = right,
                startY = y,
                primaryColor = primaryColor,
                rightBadgeTitle = "CATALOGUE ARTICLES",
                rightDocNumber = "${sortedProducts.size} référence(s)",
                rightDateStr = "Édité le ${DateUtils.formatDate(System.currentTimeMillis())}"
            )

            canvas.drawLine(left, y, right, y, strokePaint)
            y += 8f

            // Synthèse globale du catalogue (Page 1)
            if (pageIndex == 0 && activeSettings.pdfCatalogShowSummary) {
                val sumBoxH = 34f
                val sumBox = RectF(left, y, right, y + sumBoxH)
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(sumBox, 6f, 6f, fillPaint)
                canvas.drawRoundRect(sumBox, 6f, 6f, strokePaint)

                textPaint.textSize = 8.5f
                boldPaint.textSize = 9f
                boldPaint.color = Color.rgb(15, 23, 42)

                val colS1 = left + 10f
                val colS2 = left + contentWidth * 0.32f
                val colS3 = left + contentWidth * 0.65f

                canvas.drawText("Nombre d'articles :", colS1, y + 14f, textPaint)
                canvas.drawText("${sortedProducts.size} références", colS1, y + 26f, boldPaint)

                canvas.drawText("Quantités en stock :", colS2, y + 14f, textPaint)
                canvas.drawText("${CurrencyUtils.formatQty(totalStockQty)} unités", colS2, y + 26f, boldPaint)

                canvas.drawText("Valorisation totale du stock :", colS3, y + 14f, textPaint)
                boldPaint.color = Color.rgb(5, 150, 105)
                canvas.drawText(CurrencyUtils.formatDa(totalStockValuation), colS3, y + 26f, boldPaint)

                y += sumBoxH + 8f
            } else {
                y += 4f
            }

            // En-têtes dynamiques du tableau selon l'ordre choisi
            val tableHeaderH = 20f
            val thBox = RectF(left, y, right, y + tableHeaderH)
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(thBox, 4f, 4f, fillPaint)
            canvas.drawRoundRect(thBox, 4f, 4f, strokePaint)

            val headerFontSize = if (activeColumns.size >= 8) 7.3f else 8f
            boldPaint.textSize = headerFontSize
            boldPaint.color = Color.rgb(51, 65, 85)

            var curHeaderX = left
            activeColumns.forEachIndexed { colIdx, col ->
                val w = colWidths[colIdx]
                val headerTxt = fitTextToWidth(col.shortHeader, boldPaint, w - 6f)
                canvas.drawText(headerTxt, curHeaderX + 4f, y + 13.5f, boldPaint)
                curHeaderX += w
            }
            y += tableHeaderH + 2f

            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, sortedProducts.size)
            val rowFontSize = if (activeColumns.size >= 8) 7.4f else 8f

            for (i in startIndex until endIndex) {
                val prod = sortedProducts[i]
                val rowH = 18f

                if (i % 2 == 1) {
                    fillPaint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(left, y, right, y + rowH, fillPaint)
                }

                var cellX = left
                activeColumns.forEachIndexed { colIdx, col ->
                    val w = colWidths[colIdx]
                    val maxCellTextW = w - 6f

                    when (col) {
                        ArticlePdfColumn.NUMERO -> {
                            textPaint.textSize = rowFontSize
                            textPaint.color = Color.rgb(30, 41, 59)
                            canvas.drawText("${i + 1}", cellX + 4f, y + 12f, textPaint)
                        }
                        ArticlePdfColumn.REFERENCE -> {
                            val refStr = prod.reference.ifBlank { prod.barcode.ifBlank { "-" } }
                            grayPaint.textSize = rowFontSize - 0.3f
                            grayPaint.color = Color.rgb(71, 85, 105)
                            canvas.drawText(fitTextToWidth(refStr, grayPaint, maxCellTextW), cellX + 4f, y + 12f, grayPaint)
                        }
                        ArticlePdfColumn.CODE_BARRES -> {
                            val cbStr = prod.barcode.ifBlank { "-" }
                            grayPaint.textSize = rowFontSize - 0.3f
                            grayPaint.color = Color.rgb(71, 85, 105)
                            canvas.drawText(fitTextToWidth(cbStr, grayPaint, maxCellTextW), cellX + 4f, y + 12f, grayPaint)
                        }
                        ArticlePdfColumn.DESIGNATION -> {
                            boldPaint.textSize = rowFontSize
                            boldPaint.color = Color.rgb(15, 23, 42)
                            canvas.drawText(fitTextToWidth(prod.name, boldPaint, maxCellTextW), cellX + 4f, y + 12f, boldPaint)
                        }
                        ArticlePdfColumn.FAMILLE -> {
                            grayPaint.textSize = rowFontSize - 0.2f
                            grayPaint.color = Color.rgb(71, 85, 105)
                            val famStr = prod.category.ifBlank { "Général" }
                            canvas.drawText(fitTextToWidth(famStr, grayPaint, maxCellTextW), cellX + 4f, y + 12f, grayPaint)
                        }
                        ArticlePdfColumn.MARQUE -> {
                            textPaint.textSize = rowFontSize - 0.2f
                            textPaint.color = Color.rgb(30, 41, 59)
                            val mqStr = prod.marque.ifBlank { "-" }
                            canvas.drawText(fitTextToWidth(mqStr, textPaint, maxCellTextW), cellX + 4f, y + 12f, textPaint)
                        }
                        ArticlePdfColumn.PRIX_ACHAT -> {
                            textPaint.textSize = rowFontSize
                            textPaint.color = Color.rgb(30, 41, 59)
                            val paStr = prod.prixAchat?.let { CurrencyUtils.formatDaSmart(it) } ?: "-"
                            canvas.drawText(fitTextToWidth(paStr, textPaint, maxCellTextW), cellX + 4f, y + 12f, textPaint)
                        }
                        ArticlePdfColumn.PRIX_VENTE -> {
                            boldPaint.textSize = rowFontSize
                            boldPaint.color = Color.rgb(2, 132, 199)
                            val pvVal = prod.prixVente ?: prod.defaultUnitPrice
                            val pvStr = pvVal?.let { CurrencyUtils.formatDaSmart(it) } ?: "-"
                            canvas.drawText(fitTextToWidth(pvStr, boldPaint, maxCellTextW), cellX + 4f, y + 12f, boldPaint)
                        }
                        ArticlePdfColumn.STOCK -> {
                            val stockStr = if (activeColumns.contains(ArticlePdfColumn.UNITE)) {
                                CurrencyUtils.formatQty(prod.stockActuel)
                            } else {
                                "${CurrencyUtils.formatQty(prod.stockActuel)} ${prod.unit}"
                            }
                            boldPaint.textSize = rowFontSize
                            boldPaint.color = when {
                                prod.stockActuel <= 0.0 -> Color.rgb(220, 38, 38)
                                prod.stockActuel <= prod.seuilAlerte -> Color.rgb(217, 119, 6)
                                else -> Color.rgb(5, 150, 105)
                            }
                            canvas.drawText(fitTextToWidth(stockStr, boldPaint, maxCellTextW), cellX + 4f, y + 12f, boldPaint)
                        }
                        ArticlePdfColumn.UNITE -> {
                            grayPaint.textSize = rowFontSize - 0.2f
                            grayPaint.color = Color.rgb(100, 116, 139)
                            canvas.drawText(fitTextToWidth(prod.unit, grayPaint, maxCellTextW), cellX + 4f, y + 12f, grayPaint)
                        }
                        ArticlePdfColumn.VALEUR_STOCK -> {
                            val unitVal = prod.prixVente ?: prod.defaultUnitPrice ?: prod.prixAchat ?: 0.0
                            val lineVal = unitVal * prod.stockActuel
                            boldPaint.textSize = rowFontSize
                            boldPaint.color = Color.rgb(15, 23, 42)
                            canvas.drawText(fitTextToWidth(CurrencyUtils.formatDaSmart(lineVal), boldPaint, maxCellTextW), cellX + 4f, y + 12f, boldPaint)
                        }
                    }
                    cellX += w
                }

                y += rowH
                canvas.drawLine(left, y, right, y, strokePaint)
            }

            // Footer
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(148, 163, 184)
                textSize = 7.5f
                textAlign = Paint.Align.CENTER
            }
            val footerStr = if (activeSettings.pdfCustomFooter.isNotBlank()) {
                "${activeSettings.pdfCustomFooter} • Page ${pageIndex + 1} / $totalPages"
            } else {
                "${activeSettings.companyName} • Page ${pageIndex + 1} / $totalPages • Catalogue Articles (${activeColumns.size} colonnes)"
            }
            canvas.drawText(
                fitTextToWidth(footerStr, footerPaint, contentWidth),
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 16f,
                footerPaint
            )

            pdfDocument.finishPage(page)
        }

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val file = File(docsDir, "Catalogue_Articles_Merelec.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Génère l'État Global Récapitulatif des Créances Clients en PDF (A4) avec logo, coordonnées et colonnes réorganisables
     */
    fun generateGlobalCreancesPdf(
        context: Context,
        clientsBalances: List<PartnerBalanceItem>,
        summary: CreancesSummary,
        settings: CompanySettings? = null,
        selectedColumns: List<CreancePdfColumn>? = null
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val activeColumns = (selectedColumns ?: CreancePdfColumn.fromOrderString(activeSettings.pdfCreanceColumnsOrder))
            .ifEmpty { CreancePdfColumn.defaultColumns() }
        val pdfDocument = PdfDocument()

        val left = 18f
        val right = PAGE_WIDTH - 18f
        val contentWidth = right - left

        val debtors = clientsBalances
        val itemsPerPage = 21
        val totalPages = if (debtors.isEmpty()) 1 else (debtors.size + itemsPerPage - 1) / itemsPerPage

        val boldPaint = createBoldPaint(Color.rgb(15, 23, 42))
        val textPaint = createNormalPaint(Color.rgb(30, 41, 59))
        val grayPaint = createNormalPaint(Color.rgb(100, 116, 139))
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(226, 232, 240)
        }

        val primaryColor = Color.rgb(220, 38, 38)
        val totalWeight = activeColumns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = activeColumns.map { col -> contentWidth * (col.weight / totalWeight) }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1)
                .setContentRect(android.graphics.Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT))
                .create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = 18f

            // En-tête entreprise avec Logo & Coordonnées réorganisées
            y = drawCompanyHeaderBlock(
                context = context,
                canvas = canvas,
                settings = activeSettings,
                left = left,
                right = right,
                startY = y,
                primaryColor = primaryColor,
                rightBadgeTitle = "ÉTAT DES CRÉANCES",
                rightDocNumber = "${debtors.size} partenaire(s)",
                rightDateStr = "Édité le ${DateUtils.formatDate(System.currentTimeMillis())}"
            )

            canvas.drawLine(left, y, right, y, strokePaint)
            y += 10f

            // Synthèse globale (Page 1)
            if (pageIndex == 0 && activeSettings.pdfCreanceShowSummary) {
                val cardW = (contentWidth - 16f) / 3f
                val cardH = 44f

                val b1 = RectF(left, y, left + cardW, y + cardH)
                fillPaint.color = Color.rgb(254, 242, 242)
                canvas.drawRoundRect(b1, 6f, 6f, fillPaint)
                canvas.drawRoundRect(b1, 6f, 6f, strokePaint)
                grayPaint.textSize = 7.5f
                canvas.drawCleanText("TOTAL CRÉANCES CLIENTS DUES", left + 8f, y + 14f, grayPaint)
                boldPaint.textSize = 11f
                boldPaint.color = Color.rgb(220, 38, 38)
                canvas.drawCleanText(CurrencyUtils.formatDa(summary.totalCreancesClients), left + 8f, y + 32f, boldPaint)

                val b2Left = left + cardW + 8f
                val b2 = RectF(b2Left, y, b2Left + cardW, y + cardH)
                fillPaint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(b2, 6f, 6f, fillPaint)
                canvas.drawRoundRect(b2, 6f, 6f, strokePaint)
                canvas.drawCleanText("DETTES FOURNISSEURS", b2Left + 8f, y + 14f, grayPaint)
                boldPaint.color = Color.rgb(79, 70, 229)
                canvas.drawCleanText(CurrencyUtils.formatDa(summary.totalDettesFournisseurs), b2Left + 8f, y + 32f, boldPaint)

                val b3Left = b2Left + cardW + 8f
                val b3 = RectF(b3Left, y, right, y + cardH)
                val isNetPositive = summary.soldeNetCommercial >= 0
                fillPaint.color = if (isNetPositive) Color.rgb(240, 253, 244) else Color.rgb(254, 242, 242)
                canvas.drawRoundRect(b3, 6f, 6f, fillPaint)
                canvas.drawRoundRect(b3, 6f, 6f, strokePaint)
                canvas.drawCleanText("SOLDE NET (CRÉANCES - DETTES)", b3Left + 8f, y + 14f, grayPaint)
                boldPaint.color = if (isNetPositive) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
                canvas.drawCleanText(CurrencyUtils.formatDa(summary.soldeNetCommercial), b3Left + 8f, y + 32f, boldPaint)

                y += cardH + 12f
            } else {
                y += 4f
            }

            // Tableau dynamique des créances selon l'ordre des colonnes
            val tableH = 22f
            val thBox = RectF(left, y, right, y + tableH)
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(thBox, 4f, 4f, fillPaint)
            canvas.drawRoundRect(thBox, 4f, 4f, strokePaint)

            boldPaint.textSize = 7.8f
            boldPaint.color = Color.rgb(51, 65, 85)

            var curHeaderX = left
            activeColumns.forEachIndexed { colIdx, col ->
                val w = colWidths[colIdx]
                val headerTxt = fitTextToWidth(col.shortHeader, boldPaint, w - 6f)
                canvas.drawCleanText(headerTxt, curHeaderX + 4f, y + 15f, boldPaint)
                curHeaderX += w
            }
            y += tableH

            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, debtors.size)

            for (i in startIndex until endIndex) {
                val item = debtors[i]
                val rowH = 22f

                if (i % 2 == 1) {
                    fillPaint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(left, y, right, y + rowH, fillPaint)
                }

                val textY = y + 14.5f
                var cellX = left
                activeColumns.forEachIndexed { colIdx, col ->
                    val w = colWidths[colIdx]
                    val maxW = w - 6f
                    when (col) {
                        CreancePdfColumn.NUMERO -> {
                            textPaint.textSize = 8f
                            textPaint.color = Color.rgb(30, 41, 59)
                            canvas.drawCleanText("${i + 1}", cellX + 4f, textY, textPaint)
                        }
                        CreancePdfColumn.CLIENT -> {
                            boldPaint.textSize = 8f
                            boldPaint.color = Color.rgb(15, 23, 42)
                            canvas.drawCleanText(fitTextToWidth(item.partner.name, boldPaint, maxW), cellX + 4f, textY, boldPaint)
                        }
                        CreancePdfColumn.TELEPHONE_VILLE -> {
                            grayPaint.textSize = 7.5f
                            val phoneStr = listOf(item.partner.phone, item.partner.address).filter { it.isNotBlank() }.joinToString(" • ").ifEmpty { "-" }
                            canvas.drawCleanText(fitTextToWidth(phoneStr, grayPaint, maxW), cellX + 4f, textY, grayPaint)
                        }
                        CreancePdfColumn.IDENTIFIANTS_FISCAUX -> {
                            grayPaint.textSize = 7.3f
                            val fiscStr = listOfNotNull(
                                item.partner.nif.ifBlank { null }?.let { "NIF:$it" },
                                item.partner.rc.ifBlank { null }?.let { "RC:$it" }
                            ).joinToString(" ").ifEmpty { "-" }
                            canvas.drawCleanText(fitTextToWidth(fiscStr, grayPaint, maxW), cellX + 4f, textY, grayPaint)
                        }
                        CreancePdfColumn.TOTAL_FACTURE -> {
                            textPaint.textSize = 8f
                            textPaint.color = Color.rgb(30, 41, 59)
                            canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatDaSmart(item.totalTransactions), textPaint, maxW), cellX + 4f, textY, textPaint)
                        }
                        CreancePdfColumn.TOTAL_ENCAISSE -> {
                            boldPaint.textSize = 8f
                            boldPaint.color = Color.rgb(5, 150, 105)
                            canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatDaSmart(item.totalReglements), boldPaint, maxW), cellX + 4f, textY, boldPaint)
                        }
                        CreancePdfColumn.CREANCE_DUE -> {
                            boldPaint.textSize = 8f
                            boldPaint.color = if (item.soldeRestant > 0.01) Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
                            canvas.drawCleanText(fitTextToWidth(CurrencyUtils.formatDa(item.soldeRestant), boldPaint, maxW), cellX + 4f, textY, boldPaint)
                        }
                        CreancePdfColumn.TAUX_RECOUVREMENT -> {
                            val totalDueBase = (item.totalReglements + item.soldeRestant).coerceAtLeast(item.totalTransactions)
                            val rate = if (totalDueBase > 0.0) {
                                ((item.totalReglements / totalDueBase) * 100).toInt().coerceIn(0, 100)
                            } else 100
                            textPaint.textSize = 7.5f
                            canvas.drawCleanText("$rate %", cellX + 4f, textY, textPaint)
                        }
                    }
                    cellX += w
                }

                y += rowH
                canvas.drawLine(left, y, right, y, strokePaint)
            }

            // Footer
            val footerPaint = createNormalPaint(Color.rgb(148, 163, 184)).apply {
                textSize = 7.5f
                textAlign = Paint.Align.CENTER
            }
            val footerStr = if (activeSettings.pdfCustomFooter.isNotBlank()) {
                "${activeSettings.pdfCustomFooter} • Page ${pageIndex + 1} / $totalPages"
            } else {
                "${activeSettings.companyName} • Page ${pageIndex + 1} / $totalPages • Document de suivi des créances et recouvrement"
            }
            canvas.drawCleanText(
                fitTextToWidth(footerStr, footerPaint, contentWidth),
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 16f,
                footerPaint
            )

            pdfDocument.finishPage(page)
        }

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val file = File(docsDir, "Etat_Creances_Clients_Merelec.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Ouvrir le document PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun sharePdf(context: Context, file: File, subject: String = "Document Merélec") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Veuillez trouver ci-joint le document officiel ($subject) au format PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Partager le PDF avec le client via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    /**
     * Exporte / enregistre le fichier PDF vers un emplacement choisi par l'utilisateur (Téléchargements / Documents).
     */
    fun savePdfToUri(context: Context, pdfFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Lance l'impression directe au format A4 (ISO A4 210x297mm) via le gestionnaire d'impression Android (PrintManager).
     */
    fun printPdfA4(context: Context, file: File, jobName: String = "Impression_Bon_A4_Merelec") {
        val activity = findActivity(context)
        if (activity == null) {
            openPdf(context, file)
            return
        }
        try {
            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                openPdf(context, file)
                return
            }
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Destination d'impression invalide")
                        return
                    }
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                        } else {
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        }
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, adapter, attributes)
        } catch (_: Exception) {
            openPdf(context, file)
        }
    }

    /**
     * Génère un document PDF A4 multi-pages où chaque Bon (Bon de Livraison, Bon de Commande, Bon de Réception, Facture, Devis, Règlement)
     * est imprimé sur sa propre page A4 complète avec Logo HD, articles, montant en lettres et Cachet Humide.
     */
    fun generateFullA4BonsBatchPdf(
        context: Context,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity> = emptyList(),
        kitCompositionsMap: Map<String, List<ArticleCompositionEntity>> = emptyMap(),
        productCatalogMap: Map<String, ProductCatalogEntity> = emptyMap(),
        settings: CompanySettings? = null
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val selectedCols = DocPdfColumn.fromOrderString(activeSettings.pdfDocColumnsOrder)
        val pdfDocument = PdfDocument()

        val sortedOps = operations.sortedByDescending { it.operation.dateTime }
        val partnersByName = partners.associateBy { it.name.trim().lowercase(Locale.FRENCH) }

        if (sortedOps.isEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = createBoldPaint(Color.rgb(100, 116, 139)).apply { textSize = 14f }
            canvas.drawCleanText("Aucun bon à imprimer sur cette sélection.", 60f, 120f, paint)
            pdfDocument.finishPage(page)
        } else {
            sortedOps.forEachIndexed { index, opWithLignes ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val matchedPartner = partnersByName[opWithLignes.operation.clientFournisseur.trim().lowercase(Locale.FRENCH)]

                drawDocumentPage(
                    context = context,
                    canvas = canvas,
                    item = opWithLignes,
                    settings = activeSettings,
                    partner = matchedPartner,
                    kitCompositionsMap = kitCompositionsMap,
                    forcePrintKitComponents = activeSettings.pdfShowKitComponents,
                    selectedColumns = selectedCols,
                    productCatalogMap = productCatalogMap
                )

                pdfDocument.finishPage(page)
            }
        }

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val file = File(docsDir, "Bons_A4_Complets_${DateUtils.formatForExport(System.currentTimeMillis())}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Génère un aperçu Bitmap Ultra-HD (3x) de la première page du PDF (pour prévisualisation nette dans l'application).
     */
    fun renderPdfPreviewBitmap(file: File): Bitmap? {
        return try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            val bitmap = if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bmp = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bmp
            } else null
            renderer.close()
            descriptor.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Génère un document PDF récapitulatif regroupant plusieurs Bons de Livraison et Factures.
     */
    fun generateOperationsBatchPdf(
        context: Context,
        operations: List<OperationWithLignes>,
        reportTitle: String = "JOURNAL DES BONS DE LIVRAISON & FACTURES",
        settings: CompanySettings? = null
    ): File {
        val activeSettings = settings ?: CompanySettingsManager(context).getSettings()
        val pdfDocument = PdfDocument()

        val left = 32f
        val right = PAGE_WIDTH - 32f
        val contentWidth = right - left

        val sortedOps = operations.sortedByDescending { it.operation.dateTime }
        val itemsPerPage = 23
        val totalPages = if (sortedOps.isEmpty()) 1 else (sortedOps.size + itemsPerPage - 1) / itemsPerPage

        val totalHtSum = sortedOps.filter { it.operation.typeOperation != OperationType.REGLEMENT }.sumOf { it.totalHt }
        val totalTvaSum = sortedOps.filter { it.operation.typeOperation != OperationType.REGLEMENT }.sumOf { it.montantTva }
        val totalTtcSum = sortedOps.filter { it.operation.typeOperation != OperationType.REGLEMENT }.sumOf { it.totalTtc }

        val boldPaint = Paint().apply { isAntiAlias = true; isFakeBoldText = true }
        val textPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(30, 41, 59) }
        val grayPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(100, 116, 139) }
        val fillPaint = Paint().apply { isAntiAlias = true }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(203, 213, 225)
        }
        val primaryColor = Color.rgb(2, 132, 199)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = 32f

            y = drawCompanyHeaderBlock(
                context = context,
                canvas = canvas,
                settings = activeSettings,
                left = left,
                right = right,
                startY = y,
                primaryColor = primaryColor,
                rightBadgeTitle = "JOURNAL DOCUMENTS",
                rightDocNumber = "${sortedOps.size} document(s)",
                rightDateStr = "Édité le ${DateUtils.formatDate(System.currentTimeMillis())}"
            )

            canvas.drawLine(left, y, right, y, strokePaint)
            y += 12f

            boldPaint.textSize = 12f
            boldPaint.color = primaryColor
            canvas.drawCleanText(reportTitle, left, y, boldPaint)
            y += 10f

            if (pageIndex == 0) {
                val sumBox = RectF(left, y, right, y + 38f)
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(sumBox, 6f, 6f, fillPaint)
                canvas.drawRoundRect(sumBox, 6f, 6f, strokePaint)

                textPaint.textSize = 8.5f
                boldPaint.textSize = 9.5f
                boldPaint.color = Color.rgb(15, 23, 42)

                canvas.drawCleanText("Documents : ${sortedOps.size}", left + 10f, y + 15f, boldPaint)
                canvas.drawCleanText("Total HT : ${CurrencyUtils.formatDa(totalHtSum)}", left + 130f, y + 15f, textPaint)
                canvas.drawCleanText("Total TVA : ${CurrencyUtils.formatDa(totalTvaSum)}", left + 280f, y + 15f, textPaint)
                boldPaint.color = primaryColor
                canvas.drawCleanText("Total TTC : ${CurrencyUtils.formatDa(totalTtcSum)}", left + 10f, y + 30f, boldPaint)

                y += 46f
            }

            // En-tête du tableau
            val thBox = RectF(left, y, right, y + 20f)
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(thBox, fillPaint)
            canvas.drawLine(left, y + 20f, right, y + 20f, strokePaint)
            boldPaint.textSize = 8f
            boldPaint.color = Color.rgb(51, 65, 85)

            val cDate = left + 4f
            val cRef = left + 70f
            val cType = left + 155f
            val cPartner = left + 250f
            val cHt = left + 395f
            val cTtc = left + 465f

            canvas.drawCleanText("Date", cDate, y + 14f, boldPaint)
            canvas.drawCleanText("Référence", cRef, y + 14f, boldPaint)
            canvas.drawCleanText("Document", cType, y + 14f, boldPaint)
            canvas.drawCleanText("Client / Fournisseur", cPartner, y + 14f, boldPaint)
            canvas.drawCleanText("Total HT", cHt, y + 14f, boldPaint)
            canvas.drawCleanText("Total TTC", cTtc, y + 14f, boldPaint)
            y += 20f

            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, sortedOps.size)

            for (i in startIndex until endIndex) {
                val item = sortedOps[i]
                val op = item.operation
                val rowH = 20f

                if (i % 2 == 1) {
                    fillPaint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(left, y, right, y + rowH, fillPaint)
                }

                val textY = y + 13.5f
                textPaint.textSize = 8f
                textPaint.color = Color.rgb(30, 41, 59)
                canvas.drawCleanText(DateUtils.formatDate(op.dateTime), cDate, textY, textPaint)
                boldPaint.textSize = 8f
                boldPaint.color = Color.rgb(15, 23, 42)
                canvas.drawCleanText(op.id.take(15), cRef, textY, boldPaint)
                canvas.drawCleanText(op.typeOperation.shortCode, cType, textY, textPaint)
                canvas.drawCleanText(op.clientFournisseur.take(24), cPartner, textY, textPaint)
                canvas.drawCleanText(CurrencyUtils.formatDaSmart(item.totalHt), cHt, textY, textPaint)
                boldPaint.color = primaryColor
                canvas.drawCleanText(CurrencyUtils.formatDaSmart(item.totalTtc), cTtc, textY, boldPaint)

                y += rowH
                canvas.drawLine(left, y, right, y, strokePaint)
            }

            grayPaint.textSize = 7.5f
            grayPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "${activeSettings.companyName} • Page ${pageIndex + 1} / $totalPages",
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 16f,
                grayPaint
            )
            grayPaint.textAlign = Paint.Align.LEFT

            pdfDocument.finishPage(page)
        }

        val docsDir = File(context.cacheDir, "documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val file = File(docsDir, "Journal_BL_Factures_${DateUtils.formatForExport(System.currentTimeMillis())}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    private fun wrapTextToWidth(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (measureCleanText(paint, candidate) <= maxWidth || currentLine.isEmpty()) {
                currentLine = candidate
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines.ifEmpty { listOf(text) }
    }
}
