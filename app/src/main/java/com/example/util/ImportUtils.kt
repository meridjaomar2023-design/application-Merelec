package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class ParsedImportBatch<T>(
    val items: List<T>,
    val totalRowsRead: Int,
    val skippedRowsCount: Int,
    val fileFormat: String,
    val errorMessage: String? = null
)

data class FirestoreImportSummary(
    val entityLabel: String,
    val fileFormat: String,
    val totalRowsRead: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errorMessage: String? = null
) {
    val totalImported: Int
        get() = createdCount + updatedCount

    val isSuccess: Boolean
        get() = errorMessage == null && totalImported > 0
}

data class CreanceImportItem(
    val partnerName: String,
    val partnerType: PartnerType,
    val phone: String = "",
    val address: String = "",
    val rc: String = "",
    val nif: String = "",
    val ai: String = "",
    val nis: String = "",
    val nin: String = "",
    val rib: String = "",
    val referenceDoc: String = "",
    val montantFacture: Double = 0.0,
    val montantVerse: Double = 0.0,
    val creanceRestante: Double = 0.0,
    val motifNotes: String = ""
)

enum class ArticleImportField(
    val defaultHeader: String,
    val displayLabel: String,
    val sampleRow1: String,
    val sampleRow2: String
) {
    DESIGNATION("désignation", "Désignation de l'article *", "Câble Électrique 2.5mm Rouleau 100m", "Disjoncteur Bipolaire 16A"),
    REFERENCE("réf", "Réf.", "REF-CAB-25", "DISJ-16A-SCH"),
    CODE_BARRE("code_barre", "Code-barres", "613000100025", "613000123456"),
    FAMILLE("famille", "Famille", "Câblage", "Protection"),
    MARQUE("marque", "Marque", "Elsewedy", "Schneider"),
    PRIX_ACHAT("prix achat", "Prix d'achat (DA)", "3800", "490"),
    PRIX_VENTE("prix de vente", "Prix de vente (DA)", "4500", "680"),
    STOCK("stock", "Stock", "25", "120"),
    UNITE("unité", "Unité", "Rouleau", "U"),
    SEUIL_ALERTE("seuil_alerte", "Seuil alerte", "5", "10"),
    COLONNE_PLUS("colonne_plus", "Colonne en plus (personnalisée)", "Option 1", "Option 2");

    val displayName: String
        get() = displayLabel
}

data class ArticleColumnSpec(
    val field: ArticleImportField,
    val customHeader: String = field.defaultHeader,
    val enabled: Boolean = true,
    val isExtraColumn: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)

enum class PartnerImportField(
    val defaultHeader: String,
    val displayLabel: String,
    val sampleRow1: String,
    val sampleRow2: String
) {
    NOM("nom", "Nom de l'entreprise / Contact *", "SARL ElecPro Alger", "EURL Bâtiment Oran"),
    TELEPHONE("téléphone", "Numéro de téléphone", "0550 11 22 33", "0770 44 55 66"),
    ADRESSE("adresse", "Adresse / Ville / Zone", "Zone Industrielle Rouiba Alger", "Bd Millénium Oran"),
    RC("rc", "RC N°", "16/00-1234567B19", "31/00-7654321B20"),
    AI("ai", "N° article", "16151234567", "31017654321"),
    NIF("nif", "NIF N°", "001916123456789", "002031765432100"),
    NIS("nis", "NIS N°", "099816000123456", "099931000654321"),
    NIN("nin", "NIN N°", "109901600012345001", "109853100098765002"),
    RIB("rib", "RIB", "00500100123456789012", "00200310987654321034"),
    NOTES("notes", "Remarques / Notes", "Client Grands Comptes", "Promoteur"),
    SOLDE_INITIAL("solde_initial", "Créance / Solde initial (DA)", "25000", "0"),
    COLONNE_PLUS("colonne_plus", "Colonne en plus (personnalisée)", "Info 1", "Info 2");

    val displayName: String
        get() = displayLabel
}

data class PartnerColumnSpec(
    val field: PartnerImportField,
    val customHeader: String = field.defaultHeader,
    val enabled: Boolean = true,
    val isExtraColumn: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)

enum class CreanceImportField(
    val defaultHeader: String,
    val displayLabel: String,
    val sampleRow1: String,
    val sampleRow2: String
) {
    NOM("nom", "Nom de l'entreprise / Contact *", "SARL ElecPro Alger", "EURL Bâtiment Oran"),
    TELEPHONE("téléphone", "Numéro de téléphone", "0550 11 22 33", "0770 44 55 66"),
    ADRESSE("adresse", "Adresse / Ville / Zone", "Zone Industrielle Rouiba Alger", "Bd Millénium Oran"),
    RC("rc", "RC N°", "16/00-1234567B19", "31/00-7654321B20"),
    AI("ai", "N° article", "16151234567", "31017654321"),
    NIF("nif", "NIF N°", "001916123456789", "002031765432100"),
    NIS("nis", "NIS N°", "099816000123456", "099931000654321"),
    NIN("nin", "NIN N°", "109901600012345001", "109853100098765002"),
    RIB("rib", "RIB", "00500100123456789012", "00200310987654321034"),
    REFERENCE("reference", "N° Bon / Facture", "BL-2025-104", "FAC-2025-042"),
    MONTANT_FACTURE("montant_facture", "Montant Facture / Bon (DA)", "85000", "42000"),
    MONTANT_VERSE("montant_verse", "Montant Versé / Acompte (DA)", "25000", "0"),
    RESTE_A_PAYER("reste_a_payer", "Créance / Reste à payer (DA)", "60000", "42000"),
    NOTES("notes", "Remarques / Notes", "Solde chantier Rouiba", "Facture à crédit 30 jours"),
    COLONNE_PLUS("colonne_plus", "Colonne en plus (personnalisée)", "Info 1", "Info 2");

    val displayName: String
        get() = displayLabel
}

data class CreanceColumnSpec(
    val field: CreanceImportField,
    val customHeader: String = field.defaultHeader,
    val enabled: Boolean = true,
    val isExtraColumn: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)

object ImportUtils {

    fun defaultArticleColumns(): List<ArticleColumnSpec> = listOf(
        ArticleColumnSpec(ArticleImportField.DESIGNATION, "désignation", enabled = true),
        ArticleColumnSpec(ArticleImportField.REFERENCE, "réf", enabled = true),
        ArticleColumnSpec(ArticleImportField.CODE_BARRE, "code_barre", enabled = true),
        ArticleColumnSpec(ArticleImportField.FAMILLE, "famille", enabled = true),
        ArticleColumnSpec(ArticleImportField.MARQUE, "marque", enabled = true),
        ArticleColumnSpec(ArticleImportField.PRIX_ACHAT, "prix achat", enabled = true),
        ArticleColumnSpec(ArticleImportField.PRIX_VENTE, "prix de vente", enabled = true),
        ArticleColumnSpec(ArticleImportField.STOCK, "stock", enabled = true),
        ArticleColumnSpec(ArticleImportField.UNITE, "unité", enabled = true),
        ArticleColumnSpec(ArticleImportField.SEUIL_ALERTE, "seuil_alerte", enabled = true)
    )

    fun defaultPartnerColumns(): List<PartnerColumnSpec> = listOf(
        PartnerImportField.NOM to true,
        PartnerImportField.TELEPHONE to true,
        PartnerImportField.ADRESSE to true,
        PartnerImportField.RC to true,
        PartnerImportField.AI to true,
        PartnerImportField.NIF to true,
        PartnerImportField.NIS to true,
        PartnerImportField.NIN to true,
        PartnerImportField.RIB to true,
        PartnerImportField.NOTES to true,
        PartnerImportField.SOLDE_INITIAL to true
    ).map { (field, enabled) ->
        PartnerColumnSpec(field = field, customHeader = field.defaultHeader, enabled = enabled)
    }

    fun defaultCreanceColumns(): List<CreanceColumnSpec> = listOf(
        CreanceImportField.NOM to true,
        CreanceImportField.TELEPHONE to true,
        CreanceImportField.ADRESSE to false,
        CreanceImportField.RC to false,
        CreanceImportField.AI to false,
        CreanceImportField.NIF to false,
        CreanceImportField.NIS to false,
        CreanceImportField.NIN to false,
        CreanceImportField.RIB to false,
        CreanceImportField.REFERENCE to true,
        CreanceImportField.MONTANT_FACTURE to true,
        CreanceImportField.MONTANT_VERSE to true,
        CreanceImportField.RESTE_A_PAYER to true,
        CreanceImportField.NOTES to true
    ).map { (field, enabled) ->
        CreanceColumnSpec(field = field, customHeader = field.defaultHeader, enabled = enabled)
    }

    /**
     * Détecte le nom du fichier à partir de son Uri
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (_: Exception) {
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment ?: "fichier_import"
        }
        return name
    }

    /**
     * Lit un fichier (.xlsx Excel ou .csv / .txt) depuis un Uri et retourne une liste de lignes (chaque ligne = liste de cellules)
     */
    fun readRowsFromUri(context: Context, uri: Uri): Result<Pair<String, List<List<String>>>> {
        return try {
            val fileName = getFileName(context, uri).lowercase(Locale.ROOT)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Impossible d'ouvrir le fichier sélectionné."))

            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Le fichier sélectionné est vide (0 octet)."))
            }

            // Détection signature ZIP (PK\x03\x04) typique des fichiers Excel .xlsx
            val isZipSignature = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()

            // Détection signature binaire OLE2 (D0 CF 11 E0) des anciens fichiers Excel 97-2003 .xls binaires
            val isOle2BinaryXls = bytes.size >= 4 &&
                    bytes[0] == 0xD0.toByte() &&
                    bytes[1] == 0xCF.toByte() &&
                    bytes[2] == 0x11.toByte() &&
                    bytes[3] == 0xE0.toByte()

            if (isZipSignature || fileName.endsWith(".xlsx")) {
                val rows = parseXlsxBytes(bytes)
                if (rows.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Aucune feuille ou donnée lisible trouvée dans ce fichier Excel (.xlsx).")
                    )
                }
                Result.success("Excel (.xlsx)" to rows)
            } else if (isOle2BinaryXls) {
                Result.failure(
                    IllegalArgumentException(
                        "Ce fichier est au format binaire Excel 97-2003 (.xls). Veuillez l'enregistrer sous « Excel (.xlsx) » ou « CSV (séparateur point-virgule) » depuis Excel."
                    )
                )
            } else {
                val text = decodeTextSmart(bytes)
                if (text.contains("<Workbook", ignoreCase = true) || text.contains("<Row", ignoreCase = true) && text.contains("<Cell", ignoreCase = true)) {
                    val xmlRows = parseXmlSpreadsheet2003(text)
                    if (xmlRows.isNotEmpty()) {
                        return Result.success("Excel XML (.xls)" to xmlRows)
                    }
                }
                val rows = parseCsvOrTextToRows(text)
                if (rows.isEmpty()) {
                    return Result.failure(IllegalArgumentException("Aucune ligne valide trouvée dans le fichier CSV/Excel."))
                }
                val formatLabel = if (fileName.endsWith(".xls")) "Excel (.xls)" else "CSV / Texte"
                Result.success(formatLabel to rows)
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(
                    "Format de fichier incorrect ou illisible : ${e.localizedMessage ?: "erreur de lecture"}"
                )
            )
        }
    }

    /**
     * Read full text content from a content Uri (kept for compatibility)
     */
    fun readTextFromUri(context: Context, uri: Uri): String {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
            val isZipSignature = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()
            if (isZipSignature) {
                val rows = parseXlsxBytes(bytes)
                rows.joinToString("\n") { row -> row.joinToString(";") }
            } else {
                val text = decodeTextSmart(bytes)
                if (text.contains("<Workbook", ignoreCase = true) || (text.contains("<Row", ignoreCase = true) && text.contains("<Cell", ignoreCase = true))) {
                    val xmlRows = parseXmlSpreadsheet2003(text)
                    if (xmlRows.isNotEmpty()) {
                        return xmlRows.joinToString("\n") { row -> row.joinToString(";") }
                    }
                }
                text
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Parse un fichier Excel XML Spreadsheet 2003 (<Workbook><Worksheet><Table><Row><Cell><Data>...)
     */
    fun parseXmlSpreadsheet2003(xmlContent: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(xmlContent.reader())

            var eventType = parser.eventType
            var currentRow: MutableList<String>? = null
            var inData = false
            val dataBuffer = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name.substringAfterLast(':')
                        when {
                            tag.equals("Row", ignoreCase = true) -> {
                                currentRow = mutableListOf()
                            }
                            tag.equals("Data", ignoreCase = true) && currentRow != null -> {
                                inData = true
                                dataBuffer.setLength(0)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inData) {
                            dataBuffer.append(parser.text ?: "")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.substringAfterLast(':')
                        when {
                            tag.equals("Data", ignoreCase = true) && inData -> {
                                currentRow?.add(dataBuffer.toString().trim())
                                inData = false
                            }
                            tag.equals("Row", ignoreCase = true) -> {
                                val r = currentRow
                                if (r != null && r.any { it.isNotBlank() }) {
                                    rows.add(r)
                                }
                                currentRow = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return rows
    }

    /**
     * Décodage intelligent UTF-8 (avec ou sans BOM) ou Windows-1252 (fichiers issus d'anciens logiciels Windows)
     */
    private fun decodeTextSmart(bytes: ByteArray): String {
        // Suppression BOM UTF-8 éventuel (EF BB BF)
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        val utf8 = String(bytes, StandardCharsets.UTF_8)
        // Si le décodage UTF-8 produit des caractères de remplacement '\uFFFD', c'est un fichier ANSI / Windows-1252
        return if (utf8.contains('\uFFFD')) {
            try {
                String(bytes, Charset.forName("windows-1252"))
            } catch (_: Exception) {
                String(bytes, StandardCharsets.ISO_8859_1)
            }
        } else {
            utf8
        }
    }

    /**
     * Lecture native d'un fichier Excel (.xlsx) sans dépendance lourde :
     * Extrait `xl/sharedStrings.xml` et la première feuille `xl/worksheets/sheet1.xml`
     */
    fun parseXlsxBytes(bytes: ByteArray): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null
        var fallbackSheetBytes: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                when {
                    entryName.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStringsXml(zis.readBytes()))
                    }
                    entryName.equals("xl/worksheets/sheet1.xml", ignoreCase = true) -> {
                        sheetXmlBytes = zis.readBytes()
                    }
                    entryName.startsWith("xl/worksheets/sheet", ignoreCase = true) &&
                            entryName.endsWith(".xml", ignoreCase = true) &&
                            fallbackSheetBytes == null -> {
                        fallbackSheetBytes = zis.readBytes()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val targetSheet = sheetXmlBytes ?: fallbackSheetBytes ?: return emptyList()
        return parseWorksheetXml(targetSheet, sharedStrings)
    }

    private fun parseSharedStringsXml(xmlBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        var inSi = false
        var inT = false
        val currentString = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            currentString.clear()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        currentString.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            inSi = false
                            result.add(currentString.toString().trim())
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        var currentRowMap = mutableMapOf<Int, String>()
        var maxColInRow = -1
        var currentCellCol = 0
        var currentCellType: String? = null
        var inValueOrInlineText = false
        val cellText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowMap = mutableMapOf()
                            maxColInRow = -1
                            currentCellCol = 0
                        }
                        "c" -> {
                            val cellRef = parser.getAttributeValue(null, "r") // Ex: "A1", "C12"
                            currentCellCol = if (!cellRef.isNullOrBlank()) {
                                excelColumnIndexFromRef(cellRef)
                            } else {
                                maxColInRow + 1
                            }
                            currentCellType = parser.getAttributeValue(null, "t")
                            cellText.clear()
                        }
                        "v", "t" -> {
                            inValueOrInlineText = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValueOrInlineText) {
                        cellText.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v", "t" -> {
                            inValueOrInlineText = false
                        }
                        "c" -> {
                            val raw = cellText.toString().trim()
                            val resolved = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                                }
                                else -> raw
                            }
                            currentRowMap[currentCellCol] = resolved
                            if (currentCellCol > maxColInRow) {
                                maxColInRow = currentCellCol
                            }
                        }
                        "row" -> {
                            if (maxColInRow >= 0) {
                                val rowList = (0..maxColInRow).map { colIdx ->
                                    currentRowMap[colIdx]?.trim() ?: ""
                                }
                                if (rowList.any { it.isNotBlank() }) {
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    /**
     * Convertit une référence de cellule Excel ("A1" -> 0, "B1" -> 1, "AA1" -> 26)
     */
    private fun excelColumnIndexFromRef(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    /**
     * Découpe un texte CSV / TSV en lignes et colonnes, en gérant `;`, `\t`, `,` et les guillemets
     */
    fun parseCsvOrTextToRows(text: String): List<List<String>> {
        val rawLines = text
            .replace("\uFEFF", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawLines.isEmpty()) return emptyList()

        // Détection du séparateur dominant sur la première ligne
        val firstLine = rawLines.first()
        val delimiter: Char? = when {
            firstLine.contains(';') -> ';'
            firstLine.contains('\t') -> '\t'
            firstLine.contains(',') -> ','
            else -> null
        }

        return rawLines.map { line ->
            val lineDelim = delimiter ?: when {
                line.contains(';') -> ';'
                line.contains('\t') -> '\t'
                line.contains(',') -> ','
                else -> null
            }
            if (lineDelim != null) {
                splitCsvLine(line, lineDelim)
            } else {
                listOf(line.trim())
            }
        }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim().removeSurrounding("\""))
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim().removeSurrounding("\""))
        return tokens
    }

    private fun normalizeHeader(raw: String): String {
        val noAccents = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
    }

    // =========================================================================
    // 1. IMPORTATION DES ARTICLES (STOCK) :
    // Colonnes supportées : réf, désignation, famille, marque, prix achat, prix de vente, stock [unité, code_barre]
    // =========================================================================

    fun parseProductsFromUri(
        context: Context,
        uri: Uri,
        customColumns: List<ArticleColumnSpec>? = null
    ): ParsedImportBatch<ProductCatalogEntity> {
        val readRes = readRowsFromUri(context, uri)
        if (readRes.isFailure) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = "Inconnu",
                errorMessage = readRes.exceptionOrNull()?.localizedMessage ?: "Fichier illisible"
            )
        }
        val (formatName, rows) = readRes.getOrThrow()
        return parseProductsFromRows(rows, formatName, customColumns)
    }

    fun parseProducts(
        text: String,
        customColumns: List<ArticleColumnSpec>? = null
    ): List<ProductCatalogEntity> {
        val rows = parseCsvOrTextToRows(text)
        return parseProductsFromRows(rows, "CSV / Texte", customColumns).items
    }

    fun parseProductsFromRows(
        rows: List<List<String>>,
        fileFormat: String = "Excel / CSV",
        customColumns: List<ArticleColumnSpec>? = null
    ): ParsedImportBatch<ProductCatalogEntity> {
        if (rows.isEmpty()) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = fileFormat,
                errorMessage = "Le fichier ne contient aucune ligne."
            )
        }

        val enabledSpecs = customColumns?.filter { it.enabled }.orEmpty()
        val customNormalizedHeaders = enabledSpecs
            .map { normalizeHeader(it.customHeader) }
            .filter { it.isNotBlank() }
            .toSet()

        val firstRowNormalized = rows.first().map { normalizeHeader(it) }
        val isHeaderRow = firstRowNormalized.any { h ->
            h in setOf(
                "reference", "ref", "code", "code_barre", "code_barres", "barcode", "ean",
                "nom", "designation", "article", "produit", "libelle", "description",
                "categorie", "famille", "marque", "brand", "fabricant",
                "prix_achat", "prix_d_achat", "pa", "prix_vente", "prix_de_vente", "pv", "prix", "prix_unitaire", "prix_unitaire_da",
                "stock_actuel", "stock", "quantite", "qte", "unite"
            ) || h in customNormalizedHeaders
        }

        var idxRef = -1
        var idxBarcode = -1
        var idxNom = -1
        var idxFamille = -1
        var idxMarque = -1
        var idxPrixAchat = -1
        var idxPrixVente = -1
        var idxStock = -1
        var idxUnite = -1
        var idxSeuil = -1

        if (isHeaderRow) {
            // 1. D'abord chercher selon les noms personnalisés définis par l'utilisateur
            enabledSpecs.forEach { spec ->
                val targetHeader = normalizeHeader(spec.customHeader)
                if (targetHeader.isNotBlank()) {
                    val foundIdx = firstRowNormalized.indexOfFirst { it == targetHeader }
                    if (foundIdx >= 0) {
                        when (spec.field) {
                            ArticleImportField.REFERENCE -> if (idxRef == -1) idxRef = foundIdx
                            ArticleImportField.DESIGNATION -> if (idxNom == -1) idxNom = foundIdx
                            ArticleImportField.FAMILLE -> if (idxFamille == -1) idxFamille = foundIdx
                            ArticleImportField.MARQUE -> if (idxMarque == -1) idxMarque = foundIdx
                            ArticleImportField.PRIX_ACHAT -> if (idxPrixAchat == -1) idxPrixAchat = foundIdx
                            ArticleImportField.PRIX_VENTE -> if (idxPrixVente == -1) idxPrixVente = foundIdx
                            ArticleImportField.STOCK -> if (idxStock == -1) idxStock = foundIdx
                            ArticleImportField.UNITE -> if (idxUnite == -1) idxUnite = foundIdx
                            ArticleImportField.CODE_BARRE -> if (idxBarcode == -1) idxBarcode = foundIdx
                            ArticleImportField.SEUIL_ALERTE -> if (idxSeuil == -1) idxSeuil = foundIdx
                            ArticleImportField.COLONNE_PLUS -> Unit
                        }
                    }
                }
            }

            // 2. Compléter avec la reconnaissance automatique des en-têtes standards
            firstRowNormalized.forEachIndexed { idx, h ->
                when {
                    idxRef == -1 && (h == "reference" || h == "ref" || h == "code" || h == "code_article" || h.startsWith("ref_")) -> idxRef = idx
                    idxBarcode == -1 && (h == "code_barre" || h == "code_barres" || h == "barcode" || h == "ean" || h == "gencod") -> idxBarcode = idx
                    idxNom == -1 && (h == "nom" || h == "designation" || h == "article" || h == "produit" || h == "libelle" || h == "nom_article" || h.contains("designation")) -> idxNom = idx
                    idxFamille == -1 && (h == "famille" || h == "categorie" || h == "rayon" || h == "groupe" || h.contains("famille")) -> idxFamille = idx
                    idxMarque == -1 && (h == "marque" || h == "brand" || h == "fabricant" || h == "constructeur" || h.contains("marque")) -> idxMarque = idx
                    idxPrixAchat == -1 && (h == "prix_achat" || h == "prix_d_achat" || h == "pa" || h == "cout" || h == "prix_achat_ht" || h == "prix_achat_moyen" || h.contains("achat")) -> idxPrixAchat = idx
                    idxPrixVente == -1 && (h == "prix_vente" || h == "prix_de_vente" || h == "pv" || h == "prix" || h == "prix_unitaire" || h == "prix_unitaire_da" || h == "pu" || h == "tarif" || h.contains("vente")) -> idxPrixVente = idx
                    idxStock == -1 && (h == "stock_actuel" || h == "stock" || h == "quantite" || h == "qte" || h == "qte_stock" || h.contains("stock") || h.contains("quantite")) -> idxStock = idx
                    idxUnite == -1 && (h == "unite" || h == "u" || h == "conditionnement") -> idxUnite = idx
                    idxSeuil == -1 && (h == "seuil_alerte" || h == "seuil" || h == "stock_min" || h == "alerte") -> idxSeuil = idx
                }
            }
            if (idxRef == -1 && idxBarcode != -1) {
                idxRef = idxBarcode
            }
            if (idxNom == -1) {
                val customNomPos = enabledSpecs.indexOfFirst { it.field == ArticleImportField.DESIGNATION }
                idxNom = if (customNomPos >= 0) customNomPos else if (idxRef == 0 && firstRowNormalized.size > 1) 1 else 0
            }
        } else if (enabledSpecs.isNotEmpty()) {
            // Pas d'en-tête détecté : appliquer l'ordre exact des colonnes choisies par l'utilisateur
            enabledSpecs.forEachIndexed { pos, spec ->
                when (spec.field) {
                    ArticleImportField.REFERENCE -> idxRef = pos
                    ArticleImportField.DESIGNATION -> idxNom = pos
                    ArticleImportField.FAMILLE -> idxFamille = pos
                    ArticleImportField.MARQUE -> idxMarque = pos
                    ArticleImportField.PRIX_ACHAT -> idxPrixAchat = pos
                    ArticleImportField.PRIX_VENTE -> idxPrixVente = pos
                    ArticleImportField.STOCK -> idxStock = pos
                    ArticleImportField.UNITE -> idxUnite = pos
                    ArticleImportField.CODE_BARRE -> idxBarcode = pos
                    ArticleImportField.SEUIL_ALERTE -> idxSeuil = pos
                    ArticleImportField.COLONNE_PLUS -> Unit
                }
            }
        }

        val startIndex = if (isHeaderRow) 1 else 0
        val dataRowsCount = (rows.size - startIndex).coerceAtLeast(0)
        val parsedItems = mutableListOf<ProductCatalogEntity>()
        var skipped = 0

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue

            if (isHeaderRow || enabledSpecs.isNotEmpty()) {
                val rawName = if (idxNom >= 0) row.getOrNull(idxNom)?.trim() ?: "" else ""
                val rawRef = if (idxRef >= 0) row.getOrNull(idxRef)?.trim() ?: "" else ""
                val rawBarcode = if (idxBarcode >= 0) row.getOrNull(idxBarcode)?.trim() ?: "" else rawRef

                val effectiveName = rawName.ifBlank { rawRef }
                if (effectiveName.isBlank()) {
                    skipped++
                    continue
                }

                val famille = (if (idxFamille >= 0) row.getOrNull(idxFamille)?.trim() else null)
                    ?.ifBlank { "Général" } ?: "Général"
                val marque = if (idxMarque >= 0) row.getOrNull(idxMarque)?.trim() ?: "" else ""
                val prixAchatText = if (idxPrixAchat >= 0) row.getOrNull(idxPrixAchat)?.trim() ?: "" else ""
                val prixVenteText = if (idxPrixVente >= 0) row.getOrNull(idxPrixVente)?.trim() ?: "" else ""
                val stockText = if (idxStock >= 0) row.getOrNull(idxStock)?.trim() ?: "" else ""
                val unit = (if (idxUnite >= 0) row.getOrNull(idxUnite)?.trim() else null)
                    ?.ifBlank { "U" } ?: "U"
                val seuilText = if (idxSeuil >= 0) row.getOrNull(idxSeuil)?.trim() ?: "" else ""

                val prixAchat = if (prixAchatText.isNotBlank()) CurrencyUtils.parseAmount(prixAchatText).coerceAtLeast(0.0) else null
                val prixVente = if (prixVenteText.isNotBlank()) CurrencyUtils.parseAmount(prixVenteText).coerceAtLeast(0.0) else null
                val stock = if (stockText.isNotBlank()) CurrencyUtils.parseAmount(stockText) else 0.0
                val seuil = if (seuilText.isNotBlank()) CurrencyUtils.parseAmount(seuilText).coerceAtLeast(0.0) else 5.0

                parsedItems.add(
                    ProductCatalogEntity(
                        id = UUID.randomUUID().toString(),
                        name = effectiveName,
                        reference = rawRef,
                        barcode = rawBarcode,
                        category = famille,
                        marque = marque,
                        unit = unit,
                        prixAchatMoyen = prixAchat,
                        defaultUnitPrice = prixVente,
                        stockActuel = stock,
                        seuilAlerte = seuil
                    )
                )
            } else {
                // Sans ligne d'en-tête et sans configuration explicite : détection selon le nombre de colonnes
                when {
                    row.size >= 7 -> {
                        // Ordre standard 7 colonnes : réf ; désignation ; famille ; marque ; prix achat ; prix de vente ; stock [; unité]
                        val ref = row.getOrNull(0)?.trim() ?: ""
                        val nom = (row.getOrNull(1)?.trim() ?: "").ifBlank { ref }
                        if (nom.isBlank()) {
                            skipped++
                            continue
                        }
                        val famille = row.getOrNull(2)?.trim()?.ifBlank { "Général" } ?: "Général"
                        val marque = row.getOrNull(3)?.trim() ?: ""
                        val paRaw = row.getOrNull(4)?.trim() ?: ""
                        val prixAchat = if (paRaw.isNotBlank()) CurrencyUtils.parseAmount(paRaw).coerceAtLeast(0.0) else null
                        val pvRaw = row.getOrNull(5)?.trim() ?: ""
                        val prixVente = if (pvRaw.isNotBlank()) CurrencyUtils.parseAmount(pvRaw).coerceAtLeast(0.0) else null
                        val stock = CurrencyUtils.parseAmount(row.getOrNull(6) ?: "")
                        val unit = row.getOrNull(7)?.trim()?.ifBlank { "U" } ?: "U"

                        parsedItems.add(
                            ProductCatalogEntity(
                                id = UUID.randomUUID().toString(),
                                name = nom,
                                reference = ref,
                                barcode = ref,
                                category = famille,
                                marque = marque,
                                unit = unit,
                                prixAchatMoyen = prixAchat,
                                defaultUnitPrice = prixVente,
                                stockActuel = stock
                            )
                        )
                    }
                    row.size == 6 -> {
                        // Format 6 colonnes : réf ; désignation ; famille ; prix achat ; prix de vente ; stock
                        val ref = row.getOrNull(0)?.trim() ?: ""
                        val nom = (row.getOrNull(1)?.trim() ?: "").ifBlank { ref }
                        if (nom.isBlank()) {
                            skipped++
                            continue
                        }
                        val famille = row.getOrNull(2)?.trim()?.ifBlank { "Général" } ?: "Général"
                        val paRaw = row.getOrNull(3)?.trim() ?: ""
                        val prixAchat = if (paRaw.isNotBlank()) CurrencyUtils.parseAmount(paRaw).coerceAtLeast(0.0) else null
                        val pvRaw = row.getOrNull(4)?.trim() ?: ""
                        val prixVente = if (pvRaw.isNotBlank()) CurrencyUtils.parseAmount(pvRaw).coerceAtLeast(0.0) else null
                        val stock = CurrencyUtils.parseAmount(row.getOrNull(5) ?: "")

                        parsedItems.add(
                            ProductCatalogEntity(
                                id = UUID.randomUUID().toString(),
                                name = nom,
                                reference = ref,
                                barcode = ref,
                                category = famille,
                                unit = "U",
                                prixAchatMoyen = prixAchat,
                                defaultUnitPrice = prixVente,
                                stockActuel = stock
                            )
                        )
                    }
                    row.size in 2..5 -> {
                        // Format court : Désignation ; Prix de vente ; Famille ; Marque ; Stock
                        val nom = row.getOrNull(0)?.trim() ?: ""
                        if (nom.isBlank()) {
                            skipped++
                            continue
                        }
                        val pvRaw = row.getOrNull(1)?.trim() ?: ""
                        val prixVente = if (pvRaw.isNotBlank()) CurrencyUtils.parseAmount(pvRaw).coerceAtLeast(0.0) else null
                        val famille = row.getOrNull(2)?.trim()?.ifBlank { "Général" } ?: "Général"
                        val marque = row.getOrNull(3)?.trim() ?: ""
                        val stock = if (row.size >= 5) CurrencyUtils.parseAmount(row.getOrNull(4) ?: "") else 0.0

                        parsedItems.add(
                            ProductCatalogEntity(
                                id = UUID.randomUUID().toString(),
                                name = nom,
                                defaultUnitPrice = prixVente,
                                category = famille,
                                marque = marque,
                                unit = "U",
                                stockActuel = stock
                            )
                        )
                    }
                    else -> {
                        val nom = row.firstOrNull()?.trim() ?: ""
                        if (nom.isNotBlank()) {
                            parsedItems.add(
                                ProductCatalogEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = nom,
                                    defaultUnitPrice = null,
                                    category = "Général",
                                    unit = "U"
                                )
                            )
                        } else {
                            skipped++
                        }
                    }
                }
            }
        }

        val err = if (parsedItems.isEmpty()) {
            "Aucun article valide détecté. Vérifiez les colonnes : réf, désignation, famille, marque, prix achat, prix de vente, stock."
        } else null

        return ParsedImportBatch(
            items = parsedItems,
            totalRowsRead = dataRowsCount,
            skippedRowsCount = skipped,
            fileFormat = fileFormat,
            errorMessage = err
        )
    }

    /**
     * Détecte si les lignes lues depuis un fichier CSV / Excel correspondent clairement à un fichier d'ARTICLES
     * (réf ; désignation ; famille ; marque ; prix achat ; prix de vente ; stock) afin d'éviter tout mélange avec les Clients.
     */
    fun isClearlyArticlesRows(rows: List<List<String>>): Boolean {
        val nonEmptyRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmptyRows.isEmpty()) return false

        val firstRowNormalized = nonEmptyRows.first().map { normalizeHeader(it) }
        val unmistakableArticleHeaders = setOf(
            "designation", "famille", "categorie", "marque", "brand", "fabricant",
            "prix_achat", "prix_d_achat", "pa", "prix_vente", "prix_de_vente", "pv",
            "prix", "prix_unitaire", "prix_unitaire_da", "pu", "tarif",
            "stock", "stock_actuel", "quantite", "qte", "code_barre", "code_barres",
            "barcode", "ean", "unite", "seuil_alerte", "seuil"
        )
        val unmistakablePartnerHeaders = setOf(
            "telephone", "tel", "mobile", "gsm", "phone", "adresse", "wilaya",
            "rc", "nif", "nis", "nin", "rib", "solde_initial", "raison_sociale"
        )

        val articleHeaderHits = firstRowNormalized.count { h ->
            h in unmistakableArticleHeaders ||
                h.contains("designation") ||
                h.contains("famille") ||
                h.contains("marque") ||
                h.contains("achat") ||
                h.contains("vente") ||
                h.contains("stock")
        }
        val partnerHeaderHits = firstRowNormalized.count { h -> h in unmistakablePartnerHeaders }

        if (articleHeaderHits >= 1 && articleHeaderHits > partnerHeaderHits) {
            return true
        }
        if ((firstRowNormalized.contains("ref") || firstRowNormalized.contains("reference") || firstRowNormalized.contains("code")) &&
            partnerHeaderHits == 0
        ) {
            return true
        }

        // Vérification heuristique sur la première ligne de données (sans en-tête) :
        // Dans un fichier Articles (réf ; désignation ; famille ; marque ; prix achat ; prix de vente ; stock),
        // la colonne 1 (désignation) contient des lettres (et n'est pas un numéro de téléphone), et les colonnes 4/5/6 sont numériques.
        val sampleRow = if (articleHeaderHits > 0 || partnerHeaderHits > 0) {
            nonEmptyRows.getOrNull(1)
        } else {
            nonEmptyRows.firstOrNull()
        } ?: return false

        if (sampleRow.size >= 5 && partnerHeaderHits == 0) {
            val col1 = sampleRow.getOrNull(1)?.trim().orEmpty()
            val col1HasLetters = col1.any { it.isLetter() }
            val col4Num = sampleRow.getOrNull(4)?.trim()?.replace(" ", "")?.replace(",", ".")?.toDoubleOrNull()
            val col5Num = sampleRow.getOrNull(5)?.trim()?.replace(" ", "")?.replace(",", ".")?.toDoubleOrNull()
            if (col1HasLetters && (col4Num != null || col5Num != null)) {
                return true
            }
        }
        return false
    }

    /**
     * Détecte si les lignes lues correspondent clairement à un fichier de TIERS (Clients / Fournisseurs)
     */
    fun isClearlyPartnersRows(rows: List<List<String>>): Boolean {
        val nonEmptyRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmptyRows.isEmpty()) return false
        if (isClearlyArticlesRows(nonEmptyRows)) return false

        val firstRowNormalized = nonEmptyRows.first().map { normalizeHeader(it) }
        val partnerHeaders = setOf(
            "raison_sociale", "client", "fournisseur", "tiers",
            "telephone", "tel", "mobile", "phone", "adresse", "ville", "wilaya",
            "rc", "nif", "nis", "nin", "rib", "solde_initial"
        )
        return firstRowNormalized.count { it in partnerHeaders } >= 2
    }

    // =========================================================================
    // 2. IMPORTATION DES TIERS (CLIENTS / FOURNISSEURS) :
    // Colonnes supportées : nom (raison sociale), téléphone, adresse, rc, nif, ai, nin, solde_initial
    // =========================================================================

    fun parsePartnersFromUri(
        context: Context,
        uri: Uri,
        targetType: PartnerType,
        customColumns: List<PartnerColumnSpec>? = null
    ): ParsedImportBatch<PartnerEntity> {
        val readRes = readRowsFromUri(context, uri)
        if (readRes.isFailure) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = "Inconnu",
                errorMessage = readRes.exceptionOrNull()?.localizedMessage ?: "Fichier illisible"
            )
        }
        val (formatName, rows) = readRes.getOrThrow()
        return parsePartnersFromRows(rows, targetType, formatName, customColumns)
    }

    fun parsePartners(
        text: String,
        targetType: PartnerType,
        customColumns: List<PartnerColumnSpec>? = null
    ): List<PartnerEntity> {
        val rows = parseCsvOrTextToRows(text)
        return parsePartnersFromRows(rows, targetType, "CSV / Texte", customColumns).items
    }

    fun parsePartnersFromRows(
        rows: List<List<String>>,
        targetType: PartnerType,
        fileFormat: String = "Excel / CSV",
        customColumns: List<PartnerColumnSpec>? = null
    ): ParsedImportBatch<PartnerEntity> {
        if (rows.isEmpty()) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = fileFormat,
                errorMessage = "Le fichier ne contient aucune ligne."
            )
        }

        val enabledSpecs = customColumns?.filter { it.enabled }.orEmpty()
        val customNormalizedHeaders = enabledSpecs
            .map { normalizeHeader(it.customHeader) }
            .filter { it.isNotBlank() }
            .toSet()

        val firstRowNormalized = rows.first().map { normalizeHeader(it) }
        val isHeaderRow = firstRowNormalized.any { h ->
            h in setOf(
                "nom", "raison_sociale", "client", "fournisseur", "tiers", "entreprise", "societe",
                "telephone", "tel", "mobile", "phone", "adresse", "ville", "wilaya",
                "rc", "nif", "ai", "article_imposition", "n_article", "nis", "nin", "rib",
                "solde_initial", "solde", "creance", "dette"
            ) || h in customNormalizedHeaders
        }

        var idxNom = -1
        var idxPhone = -1
        var idxAddress = -1
        var idxRc = -1
        var idxNif = -1
        var idxAi = -1
        var idxNis = -1
        var idxNin = -1
        var idxRib = -1
        var idxSoldeInitial = -1
        var idxNotes = -1

        if (isHeaderRow) {
            enabledSpecs.forEach { spec ->
                val targetHeader = normalizeHeader(spec.customHeader)
                if (targetHeader.isNotBlank()) {
                    val foundIdx = firstRowNormalized.indexOfFirst { it == targetHeader }
                    if (foundIdx >= 0) {
                        when (spec.field) {
                            PartnerImportField.NOM -> if (idxNom == -1) idxNom = foundIdx
                            PartnerImportField.TELEPHONE -> if (idxPhone == -1) idxPhone = foundIdx
                            PartnerImportField.ADRESSE -> if (idxAddress == -1) idxAddress = foundIdx
                            PartnerImportField.RC -> if (idxRc == -1) idxRc = foundIdx
                            PartnerImportField.NIF -> if (idxNif == -1) idxNif = foundIdx
                            PartnerImportField.AI -> if (idxAi == -1) idxAi = foundIdx
                            PartnerImportField.NIS -> if (idxNis == -1) idxNis = foundIdx
                            PartnerImportField.NIN -> if (idxNin == -1) idxNin = foundIdx
                            PartnerImportField.RIB -> if (idxRib == -1) idxRib = foundIdx
                            PartnerImportField.SOLDE_INITIAL -> if (idxSoldeInitial == -1) idxSoldeInitial = foundIdx
                            PartnerImportField.NOTES -> if (idxNotes == -1) idxNotes = foundIdx
                            PartnerImportField.COLONNE_PLUS -> Unit
                        }
                    }
                }
            }

            firstRowNormalized.forEachIndexed { idx, h ->
                when {
                    idxNom == -1 && (h == "nom" || h == "raison_sociale" || h == "client" || h == "fournisseur" || h == "tiers" || h == "societe" || h == "entreprise" || h.contains("nom") || h.contains("raison")) -> idxNom = idx
                    idxPhone == -1 && (h == "telephone" || h == "tel" || h == "mobile" || h == "gsm" || h == "phone" || h == "contact" || h.contains("tel")) -> idxPhone = idx
                    idxAddress == -1 && (h == "adresse" || h == "ville" || h == "wilaya" || h == "commune" || h == "address" || h.contains("adresse")) -> idxAddress = idx
                    idxRc == -1 && (h == "rc" || h == "n_rc" || h == "rc_n" || h == "registre_commerce" || h.startsWith("rc")) -> idxRc = idx
                    idxNif == -1 && (h == "nif" || h == "n_nif" || h == "nif_n" || h == "identifiant_fiscal" || h == "matricule_fiscal" || h.startsWith("nif")) -> idxNif = idx
                    idxAi == -1 && (h == "ai" || h == "article_imposition" || h == "n_article" || h == "ai_n") -> idxAi = idx
                    idxNis == -1 && (h == "nis" || h == "n_nis" || h == "nis_n") -> idxNis = idx
                    idxNin == -1 && (h == "nin" || h == "n_nin" || h == "nin_n" || h == "numero_identification_national" || h.startsWith("nin")) -> idxNin = idx
                    idxRib == -1 && (h == "rib" || h == "rip" || h == "compte_bancaire") -> idxRib = idx
                    idxSoldeInitial == -1 && (h == "solde_initial" || h == "solde" || h == "creance" || h == "dette" || h == "ancien_solde" || h == "reste_a_payer" || h.contains("solde")) -> idxSoldeInitial = idx
                    idxNotes == -1 && (h == "notes" || h == "remarques" || h == "observation" || h == "observations" || h == "commentaire") -> idxNotes = idx
                }
            }
            if (idxNom == -1) {
                val customNomPos = enabledSpecs.indexOfFirst { it.field == PartnerImportField.NOM }
                idxNom = if (customNomPos >= 0) customNomPos else 0
            }
        } else if (enabledSpecs.isNotEmpty()) {
            enabledSpecs.forEachIndexed { pos, spec ->
                when (spec.field) {
                    PartnerImportField.NOM -> idxNom = pos
                    PartnerImportField.TELEPHONE -> idxPhone = pos
                    PartnerImportField.ADRESSE -> idxAddress = pos
                    PartnerImportField.RC -> idxRc = pos
                    PartnerImportField.NIF -> idxNif = pos
                    PartnerImportField.AI -> idxAi = pos
                    PartnerImportField.NIS -> idxNis = pos
                    PartnerImportField.NIN -> idxNin = pos
                    PartnerImportField.RIB -> idxRib = pos
                    PartnerImportField.SOLDE_INITIAL -> idxSoldeInitial = pos
                    PartnerImportField.NOTES -> idxNotes = pos
                    PartnerImportField.COLONNE_PLUS -> Unit
                }
            }
        }

        val startIndex = if (isHeaderRow) 1 else 0
        val dataRowsCount = (rows.size - startIndex).coerceAtLeast(0)
        val parsedItems = mutableListOf<PartnerEntity>()
        var skipped = 0

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue

            if (isHeaderRow || enabledSpecs.isNotEmpty()) {
                val name = if (idxNom >= 0) row.getOrNull(idxNom)?.trim() ?: "" else ""
                val normName = normalizeHeader(name)
                if (name.isBlank() || normName in setOf("ref", "reference", "designation", "famille", "marque", "nom", "client", "fournisseur")) {
                    skipped++
                    continue
                }
                val phone = if (idxPhone >= 0) row.getOrNull(idxPhone)?.trim() ?: "" else ""
                val address = if (idxAddress >= 0) row.getOrNull(idxAddress)?.trim() ?: "" else ""
                val rc = if (idxRc >= 0) row.getOrNull(idxRc)?.trim() ?: "" else ""
                val nif = if (idxNif >= 0) row.getOrNull(idxNif)?.trim() ?: "" else ""
                val ai = if (idxAi >= 0) row.getOrNull(idxAi)?.trim() ?: "" else ""
                val nis = if (idxNis >= 0) row.getOrNull(idxNis)?.trim() ?: "" else ""
                val nin = if (idxNin >= 0) row.getOrNull(idxNin)?.trim() ?: "" else ""
                val rib = if (idxRib >= 0) row.getOrNull(idxRib)?.trim() ?: "" else ""
                val soldeText = if (idxSoldeInitial >= 0) row.getOrNull(idxSoldeInitial)?.trim() ?: "" else ""
                val soldeInitial = if (soldeText.isNotBlank()) CurrencyUtils.parseAmount(soldeText) else 0.0
                val notes = if (idxNotes >= 0) row.getOrNull(idxNotes)?.trim() ?: "" else ""

                parsedItems.add(
                    PartnerEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = targetType,
                        phone = phone,
                        address = address,
                        rc = rc,
                        nif = nif,
                        articleImposition = ai,
                        nis = nis,
                        nin = nin,
                        rib = rib,
                        solde = soldeInitial,
                        notes = notes
                    )
                )
            } else {
                // Sans en-tête : détection selon le nombre de colonnes
                when {
                    row.size >= 6 -> {
                        val name = row.getOrNull(0)?.trim() ?: ""
                        if (name.isBlank()) {
                            skipped++
                            continue
                        }
                        val phone = row.getOrNull(1)?.trim() ?: ""
                        val address = row.getOrNull(2)?.trim() ?: ""
                        val rc = row.getOrNull(3)?.trim() ?: ""
                        val nif = row.getOrNull(4)?.trim() ?: ""
                        val ai = row.getOrNull(5)?.trim() ?: ""
                        val soldeInitial = CurrencyUtils.parseAmount(row.getOrNull(6) ?: "")
                        val notes = row.getOrNull(7)?.trim() ?: ""

                        parsedItems.add(
                            PartnerEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = targetType,
                                phone = phone,
                                address = address,
                                rc = rc,
                                nif = nif,
                                articleImposition = ai,
                                solde = soldeInitial,
                                notes = notes
                            )
                        )
                    }
                    row.size in 2..5 -> {
                        val name = row.getOrNull(0)?.trim() ?: ""
                        if (name.isBlank()) {
                            skipped++
                            continue
                        }
                        val phone = row.getOrNull(1)?.trim() ?: ""
                        val address = row.getOrNull(2)?.trim() ?: ""
                        val col3 = row.getOrNull(3)?.trim() ?: ""
                        val col3AsNumber = col3.replace(" ", "").replace(",", ".").toDoubleOrNull()
                        val soldeInitial = col3AsNumber ?: 0.0
                        val notes = if (col3AsNumber == null) col3 else (row.getOrNull(4)?.trim() ?: "")

                        parsedItems.add(
                            PartnerEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = targetType,
                                phone = phone,
                                address = address,
                                solde = soldeInitial,
                                notes = notes
                            )
                        )
                    }
                    else -> {
                        val name = row.firstOrNull()?.trim() ?: ""
                        if (name.isNotBlank()) {
                            parsedItems.add(
                                PartnerEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    type = targetType
                                )
                            )
                        } else {
                            skipped++
                        }
                    }
                }
            }
        }

        val err = if (parsedItems.isEmpty()) {
            "Aucun tiers valide détecté. Vérifiez que votre fichier contient les colonnes : nom, téléphone, adresse, rc, nif, ai, nin, solde_initial."
        } else null

        return ParsedImportBatch(
            items = parsedItems,
            totalRowsRead = dataRowsCount,
            skippedRowsCount = skipped,
            fileFormat = fileFormat,
            errorMessage = err
        )
    }

    // =========================================================================
    // 3. IMPORTATION DES CRÉANCES & DETTES (CLIENTS / FOURNISSEURS) :
    // Colonnes supportées : nom (client/fournisseur), type, telephone, reference, montant_facture, montant_verse, reste_a_payer (ou creance/solde), notes
    // =========================================================================

    fun parseCreancesFromUri(
        context: Context,
        uri: Uri,
        defaultType: PartnerType = PartnerType.CLIENT,
        customColumns: List<CreanceColumnSpec>? = null
    ): ParsedImportBatch<CreanceImportItem> {
        val readRes = readRowsFromUri(context, uri)
        if (readRes.isFailure) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = "Inconnu",
                errorMessage = readRes.exceptionOrNull()?.localizedMessage ?: "Fichier illisible"
            )
        }
        val (formatName, rows) = readRes.getOrThrow()
        return parseCreancesFromRows(rows, defaultType, formatName, customColumns)
    }

    fun parseCreances(
        text: String,
        defaultType: PartnerType = PartnerType.CLIENT,
        customColumns: List<CreanceColumnSpec>? = null
    ): List<CreanceImportItem> {
        val rows = parseCsvOrTextToRows(text)
        return parseCreancesFromRows(rows, defaultType, "CSV / Texte", customColumns).items
    }

    fun parseCreancesFromRows(
        rows: List<List<String>>,
        defaultType: PartnerType = PartnerType.CLIENT,
        fileFormat: String = "Excel / CSV",
        customColumns: List<CreanceColumnSpec>? = null
    ): ParsedImportBatch<CreanceImportItem> {
        if (rows.isEmpty()) {
            return ParsedImportBatch(
                items = emptyList(),
                totalRowsRead = 0,
                skippedRowsCount = 0,
                fileFormat = fileFormat,
                errorMessage = "Le fichier ne contient aucune ligne."
            )
        }

        val enabledSpecs = customColumns?.filter { it.enabled }.orEmpty()
        val customNormalizedHeaders = enabledSpecs
            .map { normalizeHeader(it.customHeader) }
            .filter { it.isNotBlank() }
            .toSet()

        val firstRowNormalized = rows.first().map { normalizeHeader(it) }
        val isHeaderRow = firstRowNormalized.any { h ->
            h in setOf(
                "nom", "client", "fournisseur", "tiers", "raison_sociale", "partenaire", "entreprise", "societe",
                "creance", "creances", "dette", "dettes", "reste_a_payer", "reste", "solde", "solde_initial",
                "solde_restant", "montant_facture", "total_facture", "total_bl", "montant_verse", "montant_paye",
                "versement", "reference", "num_bon", "num_facture", "montant"
            ) || h in customNormalizedHeaders
        }

        var idxNom = -1
        var idxType = -1
        var idxPhone = -1
        var idxAddress = -1
        var idxRc = -1
        var idxNif = -1
        var idxAi = -1
        var idxNis = -1
        var idxNin = -1
        var idxRib = -1
        var idxRef = -1
        var idxMontantFacture = -1
        var idxMontantVerse = -1
        var idxCreance = -1
        var idxNotes = -1

        if (isHeaderRow) {
            enabledSpecs.forEach { spec ->
                val targetHeader = normalizeHeader(spec.customHeader)
                if (targetHeader.isNotBlank()) {
                    val foundIdx = firstRowNormalized.indexOfFirst { it == targetHeader }
                    if (foundIdx >= 0) {
                        when (spec.field) {
                            CreanceImportField.NOM -> if (idxNom == -1) idxNom = foundIdx
                            CreanceImportField.TELEPHONE -> if (idxPhone == -1) idxPhone = foundIdx
                            CreanceImportField.ADRESSE -> if (idxAddress == -1) idxAddress = foundIdx
                            CreanceImportField.RC -> if (idxRc == -1) idxRc = foundIdx
                            CreanceImportField.AI -> if (idxAi == -1) idxAi = foundIdx
                            CreanceImportField.NIF -> if (idxNif == -1) idxNif = foundIdx
                            CreanceImportField.NIS -> if (idxNis == -1) idxNis = foundIdx
                            CreanceImportField.NIN -> if (idxNin == -1) idxNin = foundIdx
                            CreanceImportField.RIB -> if (idxRib == -1) idxRib = foundIdx
                            CreanceImportField.REFERENCE -> if (idxRef == -1) idxRef = foundIdx
                            CreanceImportField.MONTANT_FACTURE -> if (idxMontantFacture == -1) idxMontantFacture = foundIdx
                            CreanceImportField.MONTANT_VERSE -> if (idxMontantVerse == -1) idxMontantVerse = foundIdx
                            CreanceImportField.RESTE_A_PAYER -> if (idxCreance == -1) idxCreance = foundIdx
                            CreanceImportField.NOTES -> if (idxNotes == -1) idxNotes = foundIdx
                            CreanceImportField.COLONNE_PLUS -> Unit
                        }
                    }
                }
            }

            firstRowNormalized.forEachIndexed { idx, h ->
                when {
                    idxNom == -1 && (h == "nom" || h == "client" || h == "fournisseur" || h == "tiers" || h == "raison_sociale" || h == "partenaire" || h == "entreprise" || h == "societe" || h.contains("client") || h.contains("fournisseur") || h.contains("nom")) -> idxNom = idx
                    idxType == -1 && (h == "type" || h == "type_tiers" || h == "categorie" || h == "nature") -> idxType = idx
                    idxPhone == -1 && (h == "telephone" || h == "tel" || h == "mobile" || h == "gsm" || h == "phone" || h.contains("tel")) -> idxPhone = idx
                    idxAddress == -1 && (h == "adresse" || h == "ville" || h == "wilaya" || h == "address" || h.contains("adresse")) -> idxAddress = idx
                    idxRc == -1 && (h == "rc" || h == "n_rc" || h.startsWith("rc")) -> idxRc = idx
                    idxNif == -1 && (h == "nif" || h == "n_nif" || h.startsWith("nif")) -> idxNif = idx
                    idxAi == -1 && (h == "ai" || h == "article_imposition" || h == "n_article") -> idxAi = idx
                    idxNis == -1 && (h == "nis" || h == "n_nis") -> idxNis = idx
                    idxNin == -1 && (h == "nin" || h == "n_nin" || h.startsWith("nin")) -> idxNin = idx
                    idxRib == -1 && (h == "rib" || h == "rip") -> idxRib = idx
                    idxRef == -1 && (h == "reference" || h == "ref" || h == "num_bon" || h == "num_facture" || h == "n_bon" || h == "n_facture" || h == "piece" || h == "document") -> idxRef = idx
                    idxMontantFacture == -1 && (h == "montant_facture" || h == "total_facture" || h == "total_bl" || h == "montant_total" || h == "total_ttc" || h == "debit" || h == "total_achats" || h == "total_ventes") -> idxMontantFacture = idx
                    idxMontantVerse == -1 && (h == "montant_verse" || h == "montant_paye" || h == "versement" || h == "paye" || h == "encaisse" || h == "regle" || h == "acompte" || h == "credit") -> idxMontantVerse = idx
                    idxCreance == -1 && (h == "creance" || h == "creances" || h == "dette" || h == "dettes" || h == "reste_a_payer" || h == "reste" || h == "solde" || h == "solde_initial" || h == "solde_restant" || h == "impaye" || h == "montant" || h.contains("creance") || h.contains("dette") || h.contains("reste") || h.contains("solde")) -> idxCreance = idx
                    idxNotes == -1 && (h == "notes" || h == "note" || h == "motif" || h == "observation" || h == "observations" || h == "remarque" || h == "designation" || h == "libelle") -> idxNotes = idx
                }
            }
            if (idxNom == -1) {
                val customNomPos = enabledSpecs.indexOfFirst { it.field == CreanceImportField.NOM }
                idxNom = if (customNomPos >= 0) customNomPos else 0
            }
        } else if (enabledSpecs.isNotEmpty()) {
            enabledSpecs.forEachIndexed { pos, spec ->
                when (spec.field) {
                    CreanceImportField.NOM -> idxNom = pos
                    CreanceImportField.TELEPHONE -> idxPhone = pos
                    CreanceImportField.ADRESSE -> idxAddress = pos
                    CreanceImportField.RC -> idxRc = pos
                    CreanceImportField.AI -> idxAi = pos
                    CreanceImportField.NIF -> idxNif = pos
                    CreanceImportField.NIS -> idxNis = pos
                    CreanceImportField.NIN -> idxNin = pos
                    CreanceImportField.RIB -> idxRib = pos
                    CreanceImportField.REFERENCE -> idxRef = pos
                    CreanceImportField.MONTANT_FACTURE -> idxMontantFacture = pos
                    CreanceImportField.MONTANT_VERSE -> idxMontantVerse = pos
                    CreanceImportField.RESTE_A_PAYER -> idxCreance = pos
                    CreanceImportField.NOTES -> idxNotes = pos
                    CreanceImportField.COLONNE_PLUS -> Unit
                }
            }
        }

        val startIndex = if (isHeaderRow) 1 else 0
        val dataRowsCount = (rows.size - startIndex).coerceAtLeast(0)
        val parsedItems = mutableListOf<CreanceImportItem>()
        var skipped = 0

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue

            if (isHeaderRow || enabledSpecs.isNotEmpty()) {
                val name = if (idxNom >= 0) row.getOrNull(idxNom)?.trim() ?: "" else ""
                if (name.isBlank()) {
                    skipped++
                    continue
                }
                val rawType = if (idxType >= 0) row.getOrNull(idxType)?.trim()?.lowercase(Locale.ROOT) ?: "" else ""
                val resolvedType = when {
                    rawType.contains("fourn") || rawType.contains("ach") || rawType == "f" -> PartnerType.FOURNISSEUR
                    rawType.contains("cli") || rawType.contains("ven") || rawType == "c" -> PartnerType.CLIENT
                    else -> defaultType
                }
                val phone = if (idxPhone >= 0) row.getOrNull(idxPhone)?.trim() ?: "" else ""
                val address = if (idxAddress >= 0) row.getOrNull(idxAddress)?.trim() ?: "" else ""
                val rc = if (idxRc >= 0) row.getOrNull(idxRc)?.trim() ?: "" else ""
                val nif = if (idxNif >= 0) row.getOrNull(idxNif)?.trim() ?: "" else ""
                val ai = if (idxAi >= 0) row.getOrNull(idxAi)?.trim() ?: "" else ""
                val nis = if (idxNis >= 0) row.getOrNull(idxNis)?.trim() ?: "" else ""
                val nin = if (idxNin >= 0) row.getOrNull(idxNin)?.trim() ?: "" else ""
                val rib = if (idxRib >= 0) row.getOrNull(idxRib)?.trim() ?: "" else ""
                val refDoc = if (idxRef >= 0) row.getOrNull(idxRef)?.trim() ?: "" else ""
                val notes = if (idxNotes >= 0) row.getOrNull(idxNotes)?.trim() ?: "" else ""

                val rawFacture = if (idxMontantFacture >= 0) CurrencyUtils.parseAmount(row.getOrNull(idxMontantFacture) ?: "").coerceAtLeast(0.0) else 0.0
                val rawVerse = if (idxMontantVerse >= 0) CurrencyUtils.parseAmount(row.getOrNull(idxMontantVerse) ?: "").coerceAtLeast(0.0) else 0.0
                val rawCreance = if (idxCreance >= 0) CurrencyUtils.parseAmount(row.getOrNull(idxCreance) ?: "").coerceAtLeast(0.0) else 0.0

                val creanceRestante = when {
                    rawCreance > 0.0 -> rawCreance
                    rawFacture > 0.0 -> (rawFacture - rawVerse).coerceAtLeast(0.0)
                    else -> 0.0
                }
                val montantFacture = when {
                    rawFacture >= creanceRestante && rawFacture > 0.0 -> rawFacture
                    else -> creanceRestante + rawVerse
                }

                parsedItems.add(
                    CreanceImportItem(
                        partnerName = name,
                        partnerType = resolvedType,
                        phone = phone,
                        address = address,
                        rc = rc,
                        nif = nif,
                        ai = ai,
                        nis = nis,
                        nin = nin,
                        rib = rib,
                        referenceDoc = refDoc,
                        montantFacture = montantFacture,
                        montantVerse = rawVerse,
                        creanceRestante = creanceRestante,
                        motifNotes = notes
                    )
                )
            } else {
                // Sans en-tête : détection intelligente
                val name = row.getOrNull(0)?.trim() ?: ""
                if (name.isBlank()) {
                    skipped++
                    continue
                }
                when {
                    row.size >= 5 -> {
                        // nom ; telephone ; reference ; montant_facture ; montant_verse ; [reste_a_payer] ; [notes]
                        val phone = row.getOrNull(1)?.trim() ?: ""
                        val refDoc = row.getOrNull(2)?.trim() ?: ""
                        val col3 = CurrencyUtils.parseAmount(row.getOrNull(3) ?: "").coerceAtLeast(0.0)
                        val col4 = CurrencyUtils.parseAmount(row.getOrNull(4) ?: "").coerceAtLeast(0.0)
                        val col5 = CurrencyUtils.parseAmount(row.getOrNull(5) ?: "").coerceAtLeast(0.0)
                        val creanceRestante = if (col5 > 0.0) col5 else (col3 - col4).coerceAtLeast(0.0)
                        val montantFacture = if (col3 >= creanceRestante && col3 > 0.0) col3 else (creanceRestante + col4)
                        val notes = row.getOrNull(6)?.trim() ?: ""
                        parsedItems.add(
                            CreanceImportItem(
                                partnerName = name,
                                partnerType = defaultType,
                                phone = phone,
                                referenceDoc = refDoc,
                                montantFacture = montantFacture,
                                montantVerse = col4,
                                creanceRestante = creanceRestante,
                                motifNotes = notes
                            )
                        )
                    }
                    row.size == 4 -> {
                        // nom ; telephone ; creance ; notes  OU  nom ; reference ; montant_facture ; montant_verse
                        val col1 = row.getOrNull(1)?.trim() ?: ""
                        val col2Num = CurrencyUtils.parseAmount(row.getOrNull(2) ?: "").coerceAtLeast(0.0)
                        val col3Raw = row.getOrNull(3)?.trim() ?: ""
                        val col3Num = col3Raw.replace(" ", "").replace(",", ".").toDoubleOrNull()
                        if (col3Num != null && col2Num >= col3Num) {
                            val reste = (col2Num - col3Num).coerceAtLeast(0.0)
                            parsedItems.add(
                                CreanceImportItem(
                                    partnerName = name,
                                    partnerType = defaultType,
                                    referenceDoc = col1,
                                    montantFacture = col2Num,
                                    montantVerse = col3Num,
                                    creanceRestante = reste
                                )
                            )
                        } else {
                            parsedItems.add(
                                CreanceImportItem(
                                    partnerName = name,
                                    partnerType = defaultType,
                                    phone = col1,
                                    montantFacture = col2Num,
                                    montantVerse = 0.0,
                                    creanceRestante = col2Num,
                                    motifNotes = col3Raw
                                )
                            )
                        }
                    }
                    row.size == 3 -> {
                        // nom ; telephone ; creance  OU  nom ; creance ; notes
                        val col1 = row.getOrNull(1)?.trim() ?: ""
                        val col2 = row.getOrNull(2)?.trim() ?: ""
                        val col2AsNum = col2.replace(" ", "").replace(",", ".").replace("DA", "", ignoreCase = true).trim().toDoubleOrNull()
                        if (col2AsNum != null) {
                            parsedItems.add(
                                CreanceImportItem(
                                    partnerName = name,
                                    partnerType = defaultType,
                                    phone = col1,
                                    montantFacture = col2AsNum.coerceAtLeast(0.0),
                                    montantVerse = 0.0,
                                    creanceRestante = col2AsNum.coerceAtLeast(0.0)
                                )
                            )
                        } else {
                            val creance = CurrencyUtils.parseAmount(col1).coerceAtLeast(0.0)
                            parsedItems.add(
                                CreanceImportItem(
                                    partnerName = name,
                                    partnerType = defaultType,
                                    montantFacture = creance,
                                    montantVerse = 0.0,
                                    creanceRestante = creance,
                                    motifNotes = col2
                                )
                            )
                        }
                    }
                    row.size == 2 -> {
                        val creance = CurrencyUtils.parseAmount(row.getOrNull(1) ?: "").coerceAtLeast(0.0)
                        parsedItems.add(
                            CreanceImportItem(
                                partnerName = name,
                                partnerType = defaultType,
                                montantFacture = creance,
                                montantVerse = 0.0,
                                creanceRestante = creance
                            )
                        )
                    }
                    else -> {
                        skipped++
                    }
                }
            }
        }

        val err = if (parsedItems.isEmpty()) {
            "Aucune créance valide détectée. Vérifiez que votre fichier contient les colonnes : nom, telephone, reference, montant_facture, montant_verse, reste_a_payer."
        } else null

        return ParsedImportBatch(
            items = parsedItems,
            totalRowsRead = dataRowsCount,
            skippedRowsCount = skipped,
            fileFormat = fileFormat,
            errorMessage = err
        )
    }

    /**
     * Génère un modèle CSV pour les Tiers (Clients / Fournisseurs) selon les colonnes, noms et ordre choisis
     */
    fun generatePartnersTemplateCsv(
        type: PartnerType,
        customColumns: List<PartnerColumnSpec>? = null
    ): String {
        val activeCols = (customColumns ?: defaultPartnerColumns()).filter { it.enabled }
            .ifEmpty { defaultPartnerColumns().filter { it.enabled } }
        val title = if (type == PartnerType.CLIENT) "Client" else "Fournisseur"
        return buildString {
            append('\uFEFF') // UTF-8 BOM pour ouverture directe dans Excel sous Windows
            append(activeCols.joinToString(";") { it.customHeader.ifBlank { it.field.defaultHeader } })
            append("\n")
            append(
                activeCols.joinToString(";") { col ->
                    when (col.field) {
                        PartnerImportField.NOM -> "SARL $title Alger"
                        else -> col.field.sampleRow1
                    }
                }
            )
            append("\n")
            append(
                activeCols.joinToString(";") { col ->
                    when (col.field) {
                        PartnerImportField.NOM -> "EURL $title Oran"
                        else -> col.field.sampleRow2
                    }
                }
            )
            append("\n")
        }
    }

    /**
     * Génère un modèle CSV pour les Articles (Stock) selon les colonnes, noms et ordre choisis :
     * par défaut : réf ; désignation ; famille ; marque ; prix achat ; prix de vente ; stock
     */
    fun generateProductsTemplateCsv(
        customColumns: List<ArticleColumnSpec>? = null
    ): String {
        val activeCols = (customColumns ?: defaultArticleColumns()).filter { it.enabled }
            .ifEmpty { defaultArticleColumns().filter { it.enabled } }
        return buildString {
            append('\uFEFF') // UTF-8 BOM pour ouverture directe dans Excel sous Windows
            append(activeCols.joinToString(";") { it.customHeader.ifBlank { it.field.defaultHeader } })
            append("\n")
            append(activeCols.joinToString(";") { it.field.sampleRow1 })
            append("\n")
            append(activeCols.joinToString(";") { it.field.sampleRow2 })
            append("\n")
        }
    }

    /**
     * Génère un modèle CSV pour les Créances Clients & Dettes Fournisseurs selon les colonnes choisies
     */
    fun generateCreancesTemplateCsv(
        defaultType: PartnerType = PartnerType.CLIENT,
        customColumns: List<CreanceColumnSpec>? = null
    ): String {
        val activeCols = (customColumns ?: defaultCreanceColumns()).filter { it.enabled }
            .ifEmpty { defaultCreanceColumns().filter { it.enabled } }
        val isClient = defaultType == PartnerType.CLIENT
        val title = if (isClient) "Client" else "Fournisseur"
        return buildString {
            append('\uFEFF') // UTF-8 BOM pour ouverture directe dans Excel sous Windows
            append(activeCols.joinToString(";") { it.customHeader.ifBlank { it.field.defaultHeader } })
            append("\n")
            append(
                activeCols.joinToString(";") { col ->
                    when (col.field) {
                        CreanceImportField.NOM -> if (isClient) "SARL ElecPro Algérie" else "Schneider Distribution DZ"
                        CreanceImportField.REFERENCE -> if (isClient) "BL-2025-104" else "BLF-2025-088"
                        else -> col.field.sampleRow1
                    }
                }
            )
            append("\n")
            append(
                activeCols.joinToString(";") { col ->
                    when (col.field) {
                        CreanceImportField.NOM -> "EURL $title Moderne"
                        CreanceImportField.REFERENCE -> if (isClient) "FAC-2025-042" else "FAC-FOU-019"
                        else -> col.field.sampleRow2
                    }
                }
            )
            append("\n")
        }
    }
}
