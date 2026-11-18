package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.CompanySettings
import com.example.data.model.EmployeeEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.StockMovementEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExportUtils {
    const val WEB_PC_DIRECT_URL = "https://ais-pre-ryazjanf7vlm4lwe7ne2h7-931669780539.europe-west2.run.app"
    const val WEB_PC_DEV_URL = "https://ais-dev-ryazjanf7vlm4lwe7ne2h7-931669780539.europe-west2.run.app"

    /**
     * Generate multi-sheet XML Spreadsheet 2003 (.xls) that opens natively in Microsoft Excel
     * with formatted headers, typed numeric values, and multiple tabs.
     */
    fun generateExcelXml(operations: List<OperationWithLignes>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")

        // Styles
        sb.append("  <Styles>\n")
        sb.append("    <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Segoe UI\" ss:Size=\"11\"/></Style>\n")
        sb.append("    <Style ss:ID=\"HeaderStyle\">\n")
        sb.append("      <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
        sb.append("      <Interior ss:Color=\"#0284C7\" ss:Pattern=\"Solid\"/>\n")
        sb.append("      <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        sb.append("    </Style>\n")
        sb.append("    <Style ss:ID=\"HeaderRegStyle\">\n")
        sb.append("      <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
        sb.append("      <Interior ss:Color=\"#059669\" ss:Pattern=\"Solid\"/>\n")
        sb.append("      <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        sb.append("    </Style>\n")
        sb.append("    <Style ss:ID=\"CurrencyStyle\">\n")
        sb.append("      <NumberFormat ss:Format=\"#,##0.00\\ &quot;DA&quot;\"/>\n")
        sb.append("      <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
        sb.append("    </Style>\n")
        sb.append("    <Style ss:ID=\"BoldText\">\n")
        sb.append("      <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Bold=\"1\"/>\n")
        sb.append("    </Style>\n")
        sb.append("  </Styles>\n")

        // Sheet 1 : Synthèse des Opérations
        sb.append("  <Worksheet ss:Name=\"Opérations Terrain\">\n")
        sb.append("    <Table>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"100\"/>\n")
        sb.append("      <Column ss:Width=\"220\"/>\n")
        sb.append("      <Column ss:Width=\"90\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"100\"/>\n")
        sb.append("      <Column ss:Width=\"200\"/>\n")

        // Header Row
        sb.append("      <Row ss:Height=\"24\" ss:StyleID=\"HeaderStyle\">\n")
        listOf(
            "ID Unique", "Date & Heure", "Type Opération", "Client / Fournisseur",
            "Nbr Articles", "Total Produits (DA)", "Montant Versé (DA)", "Reste Dû (DA)",
            "Mode Paiement", "Observations"
        ).forEach { header ->
            sb.append("        <Cell><Data ss:Type=\"String\">$header</Data></Cell>\n")
        }
        sb.append("      </Row>\n")

        for (item in operations) {
            val op = item.operation
            sb.append("      <Row ss:Height=\"20\">\n")
            sb.append("        <Cell ss:StyleID=\"BoldText\"><Data ss:Type=\"String\">${escapeXml(op.id)}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(DateUtils.formatDateTime(op.dateTime))}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.typeOperation.label)}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.clientFournisseur)}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"Number\">${item.nombreArticles}</Data></Cell>\n")
            sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">${item.totalProduits}</Data></Cell>\n")
            sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">${op.montantVerse}</Data></Cell>\n")
            sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">${item.resteAPayer}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.modeReglement)}</Data></Cell>\n")
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.notes)}</Data></Cell>\n")
            sb.append("      </Row>\n")
        }
        sb.append("    </Table>\n")
        sb.append("  </Worksheet>\n")

        // Sheet 2 : Détail des Lignes Articles
        sb.append("  <Worksheet ss:Name=\"Détails Lignes Produits\">\n")
        sb.append("    <Table>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"110\"/>\n")
        sb.append("      <Column ss:Width=\"200\"/>\n")
        sb.append("      <Column ss:Width=\"240\"/>\n")
        sb.append("      <Column ss:Width=\"80\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"130\"/>\n")

        sb.append("      <Row ss:Height=\"24\" ss:StyleID=\"HeaderStyle\">\n")
        listOf(
            "Réf Bon", "Date", "Partenaire", "Désignation Article",
            "Quantité", "Prix Unitaire (DA)", "Sous-Total Ligne (DA)"
        ).forEach { header ->
            sb.append("        <Cell><Data ss:Type=\"String\">$header</Data></Cell>\n")
        }
        sb.append("      </Row>\n")

        for (item in operations) {
            val op = item.operation
            if (op.typeOperation != OperationType.REGLEMENT) {
                for (ligne in item.lignes) {
                    sb.append("      <Row ss:Height=\"20\">\n")
                    sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.id)}</Data></Cell>\n")
                    sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(DateUtils.formatDate(op.dateTime))}</Data></Cell>\n")
                    sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.clientFournisseur)}</Data></Cell>\n")
                    sb.append("        <Cell ss:StyleID=\"BoldText\"><Data ss:Type=\"String\">${escapeXml(ligne.produit)}</Data></Cell>\n")
                    sb.append("        <Cell><Data ss:Type=\"Number\">${ligne.quantite}</Data></Cell>\n")
                    val pu = ligne.prixUnitaire ?: 0.0
                    sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">$pu</Data></Cell>\n")
                    sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">${ligne.totalLigne}</Data></Cell>\n")
                    sb.append("      </Row>\n")
                }
            }
        }
        sb.append("    </Table>\n")
        sb.append("  </Worksheet>\n")

        // Sheet 3 : Règlements
        sb.append("  <Worksheet ss:Name=\"Règlements et Paiements\">\n")
        sb.append("    <Table>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"120\"/>\n")
        sb.append("      <Column ss:Width=\"220\"/>\n")
        sb.append("      <Column ss:Width=\"140\"/>\n")
        sb.append("      <Column ss:Width=\"110\"/>\n")
        sb.append("      <Column ss:Width=\"240\"/>\n")

        sb.append("      <Row ss:Height=\"24\" ss:StyleID=\"HeaderRegStyle\">\n")
        listOf("Réf Règlement", "Date & Heure", "Client / Fournisseur", "Montant Versé (DA)", "Mode Paiement", "Observations").forEach { header ->
            sb.append("        <Cell><Data ss:Type=\"String\">$header</Data></Cell>\n")
        }
        sb.append("      </Row>\n")

        for (item in operations) {
            val op = item.operation
            if (op.typeOperation == OperationType.REGLEMENT || op.montantVerse > 0) {
                sb.append("      <Row ss:Height=\"20\">\n")
                sb.append("        <Cell ss:StyleID=\"BoldText\"><Data ss:Type=\"String\">${escapeXml(op.id)}</Data></Cell>\n")
                sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(DateUtils.formatDateTime(op.dateTime))}</Data></Cell>\n")
                sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.clientFournisseur)}</Data></Cell>\n")
                sb.append("        <Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">${op.montantVerse}</Data></Cell>\n")
                sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.modeReglement)}</Data></Cell>\n")
                sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(op.notes)}</Data></Cell>\n")
                sb.append("      </Row>\n")
            }
        }
        sb.append("    </Table>\n")
        sb.append("  </Worksheet>\n")

        sb.append("</Workbook>\n")
        return sb.toString()
    }

    /**
     * Generate Excel-compatible CSV with UTF-8 BOM and semicolon separator for Operations & Lignes
     */
    fun generateOperationsCsv(operations: List<OperationWithLignes>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID_Unique;Date_Heure;Type_Operation;Client_Fournisseur;Statut;Appliquer_TVA;Taux_TVA;Total_HT_DA;Montant_TVA_DA;Total_TTC_DA;Montant_Verse_DA;Reste_A_Payer_DA;Mode_Reglement;Produit;Quantite;Prix_Unitaire_DA;Total_Ligne_DA;Notes\n")

        for (opWithLignes in operations) {
            val op = opWithLignes.operation
            val dateStr = DateUtils.formatDateTime(op.dateTime)
            val typeStr = op.typeOperation.label
            val clientStr = escapeCsv(op.clientFournisseur)
            val statutStr = escapeCsv(op.statut)
            val tvaEnabledStr = if (op.appliquerTva) "OUI" else "NON"
            val tauxTvaStr = CurrencyUtils.formatQty(op.tauxTva)
            val totalHtStr = CurrencyUtils.formatDa(opWithLignes.totalHt)
            val montantTvaStr = CurrencyUtils.formatDa(opWithLignes.montantTva)
            val totalTtcStr = CurrencyUtils.formatDa(opWithLignes.totalTtc)
            val montantVerseStr = CurrencyUtils.formatDa(op.montantVerse)
            val resteStr = CurrencyUtils.formatDa(opWithLignes.resteAPayer)
            val modeStr = escapeCsv(op.modeReglement)
            val notesStr = escapeCsv(op.notes)

            if (op.typeOperation == OperationType.REGLEMENT || opWithLignes.lignes.isEmpty()) {
                sb.append("${op.id};$dateStr;$typeStr;$clientStr;$statutStr;$tvaEnabledStr;$tauxTvaStr;$totalHtStr;$montantTvaStr;$totalTtcStr;$montantVerseStr;$resteStr;$modeStr;;;;;$notesStr\n")
            } else {
                for (ligne in opWithLignes.lignes) {
                    val prodStr = escapeCsv(ligne.produit)
                    val qteStr = CurrencyUtils.formatQty(ligne.quantite)
                    val puStr = ligne.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "0"
                    val totLigneStr = CurrencyUtils.formatDa(ligne.totalLigne)
                    sb.append("${op.id};$dateStr;$typeStr;$clientStr;$statutStr;$tvaEnabledStr;$tauxTvaStr;$totalHtStr;$montantTvaStr;$totalTtcStr;$montantVerseStr;$resteStr;$modeStr;$prodStr;$qteStr;$puStr;$totLigneStr;$notesStr\n")
                }
            }
        }
        return sb.toString()
    }

    /**
     * Génère le CSV de la table Room `partners_directory` (Clients & Fournisseurs)
     */
    fun generatePartnersCsv(partners: List<PartnerEntity>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID_Partenaire;Type_Partenaire;Nom_Raison_Sociale;Telephone;Adresse_Ville;RC;Article_Imposition;NIF;NIS;NIN;RIB;Notes\n")
        for (p in partners) {
            sb.append(
                listOf(
                    escapeCsv(p.id),
                    escapeCsv(p.type.name),
                    escapeCsv(p.name),
                    escapeCsv(p.phone),
                    escapeCsv(p.address),
                    escapeCsv(p.rc),
                    escapeCsv(p.articleImposition),
                    escapeCsv(p.nif),
                    escapeCsv(p.nis),
                    escapeCsv(p.nin),
                    escapeCsv(p.rib),
                    escapeCsv(p.notes)
                ).joinToString(";") + "\n"
            )
        }
        return sb.toString()
    }

    /**
     * Génère le CSV de la table Room `products_catalog` (Articles, Prix & Seuils de Stock)
     */
    fun generateCatalogProductsCsv(products: List<ProductCatalogEntity>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID_Article;Code_Barres;Reference;Designation;Categorie;Unite;Prix_Unitaire_Defaut_DA;Stock_Actuel;Seuil_Alerte;Est_Kit_Eclate\n")
        for (prod in products) {
            sb.append(
                listOf(
                    escapeCsv(prod.id),
                    escapeCsv(prod.barcode),
                    escapeCsv(prod.reference),
                    escapeCsv(prod.name),
                    escapeCsv(prod.category),
                    escapeCsv(prod.unit),
                    prod.defaultUnitPrice?.let { CurrencyUtils.formatQty(it) } ?: "0",
                    CurrencyUtils.formatQty(prod.stockActuel),
                    CurrencyUtils.formatQty(prod.seuilAlerte),
                    if (prod.isArticleEclate) "OUI" else "NON"
                ).joinToString(";") + "\n"
            )
        }
        return sb.toString()
    }

    /**
     * Génère le CSV de la table Room `stock_movements` (Historique des Entrées / Sorties de Stock)
     */
    fun generateStockMovementsCsv(movements: List<StockMovementEntity>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID_Mouvement;Date_Heure;Type_Mouvement;ID_Article;Designation_Article;Quantite;Unite;Stock_Avant;Stock_Apres;ID_Bon_Livraison;Client_Fournisseur;Motif_Notes\n")
        for (m in movements) {
            sb.append(
                listOf(
                    escapeCsv(m.id),
                    DateUtils.formatDateTime(m.timestamp),
                    escapeCsv(m.type.label),
                    escapeCsv(m.productId),
                    escapeCsv(m.productName),
                    CurrencyUtils.formatQty(m.quantity),
                    escapeCsv(m.unit),
                    CurrencyUtils.formatQty(m.previousStock),
                    CurrencyUtils.formatQty(m.newStock),
                    escapeCsv(m.operationId ?: ""),
                    escapeCsv(m.partnerName),
                    escapeCsv(m.reason)
                ).joinToString(";") + "\n"
            )
        }
        return sb.toString()
    }

    /**
     * Génère un fichier CSV intégral contenant TOUTES les tables de la base de données Room :
     * 1. Opérations & Bons de Livraison / Factures (`operations_terrain` + `lignes_operation`)
     * 2. Répertoire Clients & Fournisseurs (`partners_directory`)
     * 3. Catalogue Articles & Stocks (`products_catalog`)
     * 4. Compositions de Kits Éclatés (`articles_composition`)
     * 5. Journal des Mouvements de Stock (`stock_movements`)
     */
    fun generateFullRoomDatabaseCsv(
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        compositions: List<ArticleCompositionEntity> = emptyList(),
        stockMovements: List<StockMovementEntity> = emptyList()
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM pour ouverture directe dans Excel / LibreOffice avec accents préservés
        sb.append('\uFEFF')
        sb.append("# SAUVEGARDE INTEGRALE DE LA BASE DE DONNEES ROOM - GESTION MERELEC\n")
        sb.append("# Date d'exportation;${DateUtils.formatDateTime(System.currentTimeMillis())}\n")
        sb.append("# Total Operations;${operations.size};Total Partenaires;${partners.size};Total Articles;${products.size};Total Mouvements Stock;${stockMovements.size}\n\n")

        // SECTION 1 : OPERATIONS & LIGNES (operations_terrain + lignes_operation)
        sb.append("=== TABLE ROOM 1 : OPERATIONS_TERRAIN & LIGNES_OPERATION (${operations.size} documents) ===\n")
        sb.append("Table_Room;ID_Operation;Date_Heure;Type_Operation;Client_Fournisseur;Statut;Appliquer_TVA;Taux_TVA;Total_HT_DA;Montant_TVA_DA;Total_TTC_DA;Montant_Verse_DA;Reste_A_Payer_DA;Mode_Reglement;ID_Ligne;Produit;Quantite;Prix_Unitaire_DA;Total_Ligne_DA;Notes\n")
        for (opWithLignes in operations) {
            val op = opWithLignes.operation
            val commonCols = listOf(
                escapeCsv(op.id),
                DateUtils.formatDateTime(op.dateTime),
                escapeCsv(op.typeOperation.name),
                escapeCsv(op.clientFournisseur),
                escapeCsv(op.statut),
                if (op.appliquerTva) "1" else "0",
                CurrencyUtils.formatQty(op.tauxTva),
                CurrencyUtils.formatQty(opWithLignes.totalHt),
                CurrencyUtils.formatQty(opWithLignes.montantTva),
                CurrencyUtils.formatQty(opWithLignes.totalTtc),
                CurrencyUtils.formatQty(op.montantVerse),
                CurrencyUtils.formatQty(opWithLignes.resteAPayer),
                escapeCsv(op.modeReglement)
            ).joinToString(";")

            if (opWithLignes.lignes.isEmpty()) {
                sb.append("OPERATIONS_TERRAIN;$commonCols;;;;;;${escapeCsv(op.notes)}\n")
            } else {
                for (ligne in opWithLignes.lignes) {
                    val ligneCols = listOf(
                        escapeCsv(ligne.id),
                        escapeCsv(ligne.produit),
                        CurrencyUtils.formatQty(ligne.quantite),
                        ligne.prixUnitaire?.let { CurrencyUtils.formatQty(it) } ?: "0",
                        CurrencyUtils.formatQty(ligne.totalLigne),
                        escapeCsv(op.notes)
                    ).joinToString(";")
                    sb.append("OPERATIONS_ET_LIGNES;$commonCols;$ligneCols\n")
                }
            }
        }
        sb.append("\n")

        // SECTION 2 : PARTENAIRES CLIENTS & FOURNISSEURS (partners_directory)
        sb.append("=== TABLE ROOM 2 : PARTNERS_DIRECTORY (${partners.size} partenaires) ===\n")
        sb.append("Table_Room;ID_Partenaire;Type_Partenaire;Nom_Raison_Sociale;Telephone;Adresse_Ville;RC;Article_Imposition;NIF;NIS;NIN;RIB;Notes\n")
        for (p in partners) {
            sb.append(
                listOf(
                    "PARTNERS_DIRECTORY",
                    escapeCsv(p.id),
                    escapeCsv(p.type.name),
                    escapeCsv(p.name),
                    escapeCsv(p.phone),
                    escapeCsv(p.address),
                    escapeCsv(p.rc),
                    escapeCsv(p.articleImposition),
                    escapeCsv(p.nif),
                    escapeCsv(p.nis),
                    escapeCsv(p.nin),
                    escapeCsv(p.rib),
                    escapeCsv(p.notes)
                ).joinToString(";") + "\n"
            )
        }
        sb.append("\n")

        // SECTION 3 : CATALOGUE ARTICLES & STOCKS (products_catalog)
        sb.append("=== TABLE ROOM 3 : PRODUCTS_CATALOG (${products.size} articles) ===\n")
        sb.append("Table_Room;ID_Article;Code_Barres;Reference;Designation;Categorie;Unite;Prix_Unitaire_Defaut_DA;Stock_Actuel;Seuil_Alerte;Est_Kit_Eclate\n")
        for (prod in products) {
            sb.append(
                listOf(
                    "PRODUCTS_CATALOG",
                    escapeCsv(prod.id),
                    escapeCsv(prod.barcode),
                    escapeCsv(prod.reference),
                    escapeCsv(prod.name),
                    escapeCsv(prod.category),
                    escapeCsv(prod.unit),
                    prod.defaultUnitPrice?.let { CurrencyUtils.formatQty(it) } ?: "0",
                    CurrencyUtils.formatQty(prod.stockActuel),
                    CurrencyUtils.formatQty(prod.seuilAlerte),
                    if (prod.isArticleEclate) "1" else "0"
                ).joinToString(";") + "\n"
            )
        }
        sb.append("\n")

        // SECTION 4 : COMPOSITIONS DE KITS (article_composition)
        sb.append("=== TABLE ROOM 4 : ARTICLE_COMPOSITION (${compositions.size} composants) ===\n")
        sb.append("Table_Room;ID_Composition;ID_Article_Parent;Designation_Composant;Quantite_Requise;Prix_Unitaire_Estime_DA;Unite\n")
        for (comp in compositions) {
            sb.append(
                listOf(
                    "ARTICLE_COMPOSITION",
                    escapeCsv(comp.id),
                    escapeCsv(comp.parentProductId),
                    escapeCsv(comp.componentName),
                    CurrencyUtils.formatQty(comp.quantite),
                    comp.prixUnitaireEstime?.let { CurrencyUtils.formatQty(it) } ?: "0",
                    escapeCsv(comp.unit)
                ).joinToString(";") + "\n"
            )
        }
        sb.append("\n")

        // SECTION 5 : MOUVEMENTS DE STOCK (mouvements_stock)
        sb.append("=== TABLE ROOM 5 : MOUVEMENTS_STOCK (${stockMovements.size} mouvements) ===\n")
        sb.append("Table_Room;ID_Mouvement;Date_Heure;Type_Mouvement;ID_Article;Designation_Article;Quantite;Unite;Stock_Avant;Stock_Apres;ID_Bon_Livraison;Client_Fournisseur;Motif_Notes\n")
        for (m in stockMovements) {
            sb.append(
                listOf(
                    "MOUVEMENTS_STOCK",
                    escapeCsv(m.id),
                    DateUtils.formatDateTime(m.timestamp),
                    escapeCsv(m.type.name),
                    escapeCsv(m.productId),
                    escapeCsv(m.productName),
                    CurrencyUtils.formatQty(m.quantity),
                    escapeCsv(m.unit),
                    CurrencyUtils.formatQty(m.previousStock),
                    CurrencyUtils.formatQty(m.newStock),
                    escapeCsv(m.operationId ?: ""),
                    escapeCsv(m.partnerName),
                    escapeCsv(m.reason)
                ).joinToString(";") + "\n"
            )
        }

        return sb.toString()
    }

    /**
     * Crée un fichier .csv physique dans le cache et retourne l'objet File prêt à être partagé ou enregistré.
     */
    fun createCsvFile(context: Context, fileNamePrefix: String, csvContent: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateUtils.formatForExport(System.currentTimeMillis())
        val file = File(exportDir, "${fileNamePrefix}_$timestamp.csv")
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(csvContent)
            }
        }
        return file
    }

    /**
     * Partage un fichier .csv via FileProvider (Email, WhatsApp, Drive, etc.) pour sauvegarde externe.
     */
    fun shareCsvFile(context: Context, file: File, subject: String = "Sauvegarde Base de Données Room (CSV)") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TITLE, file.name)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Veuillez trouver ci-joint l'exportation CSV de la base de données Room (${file.name})."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Exporter / Partager la sauvegarde CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            shareText(context, file.name, file.readText(StandardCharsets.UTF_8))
        }
    }

    /**
     * Enregistre le contenu CSV vers un Uri choisi par l'utilisateur via le sélecteur de documents Android.
     */
    fun saveCsvToUri(context: Context, csvContent: String, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export to real Excel file (.xls) and open Android sharesheet targeting Microsoft Excel / Sheets / Drive
     */
    fun exportToExcelFile(context: Context, operations: List<OperationWithLignes>) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = DateUtils.formatForExport(System.currentTimeMillis())
            val file = File(exportDir, "Gestion_Merelec_Export_$timestamp.xls")

            val xmlContent = generateExcelXml(operations)
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(xmlContent)
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gestion Merelec - Export Excel")
                putExtra(Intent.EXTRA_TITLE, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Ouvrir ou Partager avec Excel")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text sharing if file creation encounters permission edge case
            val csv = generateOperationsCsv(operations)
            shareText(context, "Gestion_Merelec_Export.csv", csv)
        }
    }

    /**
     * Send Excel file directly by email client (Gmail, Outlook, etc.)
     */
    fun sendExcelByEmail(context: Context, operations: List<OperationWithLignes>, recipientEmail: String? = null) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = DateUtils.formatForExport(System.currentTimeMillis())
            val file = File(exportDir, "Gestion_Merelec_Export_$timestamp.xls")

            val xmlContent = generateExcelXml(operations)
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(xmlContent)
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                if (!recipientEmail.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, "Gestion Merelec - Export des Opérations Excel ($timestamp)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Bonjour,\n\nVeuillez trouver ci-joint le classeur Excel récapitulatif des bons de livraison, achats et règlements généré depuis l'application Gestion Merelec.\n\nCordialement,\nService Gestion Merelec"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(emailIntent, "Envoyer le fichier Excel par email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            val csv = generateOperationsCsv(operations)
            shareText(context, "Gestion_Merelec_Export.csv", csv)
        }
    }

    /**
     * Generate formatted printable text slip for a single operation
     */
    fun generateSlipText(opWithLignes: OperationWithLignes): String {
        val op = opWithLignes.operation
        val sb = StringBuilder()
        val border = "========================================\n"
        val divider = "----------------------------------------\n"

        sb.append(border)
        sb.append("           GESTION MERELEC              \n")
        sb.append("   Équipements & Réalisations Élec     \n")
        sb.append("      BON & RÈGLEMENT DE TERRAIN       \n")
        sb.append(border)
        sb.append("RÉFÉRENCE   : ${op.id}\n")
        sb.append("OPÉRATION   : ${op.typeOperation.label.uppercase()}\n")
        sb.append("DATE & HEURE: ${DateUtils.formatDateTime(op.dateTime)}\n")
        val role = if (op.typeOperation == OperationType.BL_FOURNISSEUR) "FOURNISSEUR" else "CLIENT"
        sb.append("$role     : ${op.clientFournisseur}\n")
        if (op.modeReglement.isNotBlank()) {
            sb.append("MODE PAIEM. : ${op.modeReglement}\n")
        }
        sb.append(divider)

        if (op.typeOperation != OperationType.REGLEMENT && opWithLignes.lignes.isNotEmpty()) {
            sb.append("DÉTAIL DES ARTICLES :\n")
            sb.append(divider)
            opWithLignes.lignes.forEachIndexed { idx, ligne ->
                val puText = ligne.prixUnitaire?.let { CurrencyUtils.formatDa(it) } ?: "Facultatif"
                sb.append("${idx + 1}. ${ligne.produit}\n")
                sb.append("   Qté: ${CurrencyUtils.formatQty(ligne.quantite)} x $puText\n")
                sb.append("   Total ligne: ${CurrencyUtils.formatDa(ligne.totalLigne)}\n")
            }
            sb.append(divider)
            sb.append("TOTAL ARTICLES : ${opWithLignes.nombreArticles} article(s)\n")
            if (op.appliquerTva) {
                sb.append("TOTAL HT       : ${CurrencyUtils.formatDa(opWithLignes.totalHt)}\n")
                sb.append("TVA (${CurrencyUtils.formatQty(op.tauxTva)}%)     : ${CurrencyUtils.formatDa(opWithLignes.montantTva)}\n")
                sb.append("TOTAL TTC      : ${CurrencyUtils.formatDa(opWithLignes.totalTtc)}\n")
            } else {
                sb.append("TOTAL PRODUITS : ${CurrencyUtils.formatDa(opWithLignes.totalProduits)}\n")
            }
            if (op.montantVerse > 0) {
                sb.append("MONTANT VERSÉ  : ${CurrencyUtils.formatDa(op.montantVerse)}\n")
                sb.append("RESTE À PAYER  : ${CurrencyUtils.formatDa(opWithLignes.resteAPayer)}\n")
            }
            sb.append(divider)
            sb.append("${CurrencyUtils.formatArretePrefix(op.typeOperation)}\n")
            sb.append("${CurrencyUtils.amountToFrenchWordsDa(opWithLignes.totalTtc)}\n")
        } else {
            sb.append("MONTANT DU RÈGLEMENT : ${CurrencyUtils.formatDa(op.montantVerse)}\n")
            sb.append(divider)
            sb.append("${CurrencyUtils.formatArretePrefix(op.typeOperation)}\n")
            sb.append("${CurrencyUtils.amountToFrenchWordsDa(op.montantVerse)}\n")
        }

        if (op.notes.isNotBlank()) {
            sb.append(divider)
            sb.append("OBSERVATIONS / NOTES :\n${op.notes}\n")
        }
        sb.append(border)
        sb.append("Document généré par Gestion Merelec\n")
        sb.append("Montants exprimés en Dinars Algériens (DA)\n")

        return sb.toString()
    }

    fun shareText(context: Context, title: String, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    fun defaultFullRoomBackupFileName(): String {
        val timestamp = DateUtils.formatForExport(System.currentTimeMillis())
        return "Sauvegarde_Integrale_Room_Merelec_$timestamp.csv"
    }

    fun writeFullRoomDatabaseCsvToUri(
        context: Context,
        targetUri: Uri,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        compositions: List<ArticleCompositionEntity> = emptyList(),
        stockMovements: List<StockMovementEntity> = emptyList()
    ): Boolean {
        val csvContent = generateFullRoomDatabaseCsv(
            operations = operations,
            partners = partners,
            products = products,
            compositions = compositions,
            stockMovements = stockMovements
        )
        return saveCsvToUri(context, csvContent, targetUri)
    }

    fun exportAndShareFullRoomDatabaseCsv(
        context: Context,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        compositions: List<ArticleCompositionEntity> = emptyList(),
        stockMovements: List<StockMovementEntity> = emptyList()
    ) {
        val csvContent = generateFullRoomDatabaseCsv(
            operations = operations,
            partners = partners,
            products = products,
            compositions = compositions,
            stockMovements = stockMovements
        )
        val file = createCsvFile(context, "Sauvegarde_Integrale_Room_Merelec", csvContent)
        shareCsvFile(context, file, "Sauvegarde Intégrale Base de Données Room (CSV)")
    }

    fun exportAndShareRoomTablesSeparateCsvs(
        context: Context,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        compositions: List<ArticleCompositionEntity> = emptyList(),
        stockMovements: List<StockMovementEntity> = emptyList()
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = DateUtils.formatForExport(System.currentTimeMillis())

            // 1. operations_terrain
            val opsHeader = "\uFEFFID_Operation;Date_Heure;Type_Operation;Client_Fournisseur;Statut;Appliquer_TVA;Taux_TVA;Total_HT_DA;Montant_TVA_DA;Total_TTC_DA;Montant_Verse_DA;Reste_A_Payer_DA;Mode_Reglement;Notes\n"
            val opsBody = operations.joinToString("\n") { item ->
                val op = item.operation
                listOf(
                    escapeCsv(op.id),
                    DateUtils.formatDateTime(op.dateTime),
                    escapeCsv(op.typeOperation.name),
                    escapeCsv(op.clientFournisseur),
                    escapeCsv(op.statut),
                    if (op.appliquerTva) "1" else "0",
                    CurrencyUtils.formatQty(op.tauxTva),
                    CurrencyUtils.formatQty(item.totalHt),
                    CurrencyUtils.formatQty(item.montantTva),
                    CurrencyUtils.formatQty(item.totalTtc),
                    CurrencyUtils.formatQty(op.montantVerse),
                    CurrencyUtils.formatQty(item.resteAPayer),
                    escapeCsv(op.modeReglement),
                    escapeCsv(op.notes)
                ).joinToString(";")
            }
            val file1 = File(exportDir, "1_operations_terrain_$timestamp.csv").apply {
                writeText(opsHeader + opsBody, StandardCharsets.UTF_8)
            }

            // 2. lignes_operation
            val lignesHeader = "\uFEFFID_Ligne;ID_Operation;Produit;Quantite;Prix_Unitaire_DA;Total_Ligne_DA\n"
            val lignesBody = operations.flatMap { it.lignes }.joinToString("\n") { l ->
                listOf(
                    escapeCsv(l.id),
                    escapeCsv(l.operationId),
                    escapeCsv(l.produit),
                    CurrencyUtils.formatQty(l.quantite),
                    l.prixUnitaire?.let { CurrencyUtils.formatQty(it) } ?: "0",
                    CurrencyUtils.formatQty(l.totalLigne)
                ).joinToString(";")
            }
            val file2 = File(exportDir, "2_lignes_operation_$timestamp.csv").apply {
                writeText(lignesHeader + lignesBody, StandardCharsets.UTF_8)
            }

            // 3. partners_directory
            val file3 = File(exportDir, "3_partners_directory_$timestamp.csv").apply {
                writeText(generatePartnersCsv(partners), StandardCharsets.UTF_8)
            }

            // 4. products_catalog
            val file4 = File(exportDir, "4_products_catalog_$timestamp.csv").apply {
                writeText(generateCatalogProductsCsv(products), StandardCharsets.UTF_8)
            }

            // 5. article_composition
            val compHeader = "\uFEFFID_Composition;ID_Article_Parent;Designation_Composant;Quantite_Requise;Prix_Unitaire_Estime_DA;Unite\n"
            val compBody = compositions.joinToString("\n") { c ->
                listOf(
                    escapeCsv(c.id),
                    escapeCsv(c.parentProductId),
                    escapeCsv(c.componentName),
                    CurrencyUtils.formatQty(c.quantite),
                    c.prixUnitaireEstime?.let { CurrencyUtils.formatQty(it) } ?: "0",
                    escapeCsv(c.unit)
                ).joinToString(";")
            }
            val file5 = File(exportDir, "5_article_composition_$timestamp.csv").apply {
                writeText(compHeader + compBody, StandardCharsets.UTF_8)
            }

            // 6. mouvements_stock
            val file6 = File(exportDir, "6_mouvements_stock_$timestamp.csv").apply {
                writeText(generateStockMovementsCsv(stockMovements), StandardCharsets.UTF_8)
            }

            val uris = ArrayList<Uri>(
                listOf(file1, file2, file3, file4, file5, file6).map { f ->
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                }
            )

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Sauvegarde Externe Room - 6 Tables CSV ($timestamp)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Veuillez trouver ci-joint les 6 fichiers CSV des tables de la base de données Room Gestion Merélec."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Exporter les 6 tables Room (CSV)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            exportAndShareFullRoomDatabaseCsv(context, operations, partners, products, compositions, stockMovements)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Génère le fichier autonome HTML pour PC Windows (Chrome / Edge / Firefox)
     * pré-chargé avec toutes les données actuelles (Bons, Factures, Clients, Fournisseurs, Articles, Employés, Coordonnées)
     * et connecté en temps réel à Firebase Cloud Firestore + Scanner OCR Intelligent.
     */
    fun buildStandaloneWebPcHtmlBytes(
        context: Context,
        settings: CompanySettings,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        employees: List<EmployeeEntity>
    ): ByteArray {
        val rawHtml = try {
            context.assets.open("index.html").use { it.readBytes().toString(StandardCharsets.UTF_8) }
        } catch (e: Exception) {
            ""
        }
        if (rawHtml.isBlank()) return ByteArray(0)

        return try {
            val rootJson = JSONObject()

            val settingsJson = JSONObject().apply {
                put("companyName", settings.companyName)
                put("address", settings.address)
                put("phone", listOf(settings.phone, settings.phoneSecondary).filter { it.isNotBlank() }.joinToString(" / "))
                put("email", settings.email)
                put("rc", settings.rc)
                put("ai", settings.articleImposition)
                put("nif", settings.nif)
                put("nis", settings.nis)
                put("nin", settings.nin)
                put("rib", listOf(settings.bankName, settings.bankRib).filter { it.isNotBlank() }.joinToString(" • "))
                put("pdfShowStampAndSignature", settings.pdfShowStampAndSignature)
                put("pdfStampScale", settings.pdfStampScale)
            }
            rootJson.put("settings", settingsJson)

            val partnersArr = JSONArray()
            partners.forEach { p ->
                partnersArr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("type", p.type.name)
                        put("phone", p.phone)
                        put("address", p.address)
                        put("rc", p.rc)
                        put("nif", p.nif)
                        put("solde", p.solde)
                    }
                )
            }
            rootJson.put("partners", partnersArr)

            val productsArr = JSONArray()
            products.forEach { prod ->
                productsArr.put(
                    JSONObject().apply {
                        put("id", prod.id)
                        put("reference", prod.reference)
                        put("name", prod.name)
                        put("category", prod.category)
                        put("unit", prod.unit)
                        put("defaultUnitPrice", prod.defaultUnitPrice ?: 0.0)
                        put("prixAchatMoyen", prod.prixAchatMoyen)
                        put("stockActuel", prod.stockActuel)
                    }
                )
            }
            rootJson.put("products", productsArr)

            val opsArr = JSONArray()
            operations.forEach { item ->
                val op = item.operation
                val linesArr = JSONArray()
                item.lignes.forEach { l ->
                    linesArr.put(
                        JSONObject().apply {
                            put("id", l.id)
                            put("produit", l.produit)
                            put("quantite", l.quantite)
                            put("prixUnitaire", l.prixUnitaire ?: 0.0)
                        }
                    )
                }
                opsArr.put(
                    JSONObject().apply {
                        put("id", op.id)
                        put("dateTime", op.dateTime)
                        put("typeOperation", op.typeOperation.name)
                        put("clientFournisseur", op.clientFournisseur)
                        put("montantVerse", op.montantVerse)
                        put("modeReglement", op.modeReglement)
                        put("appliquerTva", op.appliquerTva)
                        put("tauxTva", op.tauxTva)
                        put("notes", op.notes)
                        put("lignes", linesArr)
                    }
                )
            }
            rootJson.put("operations", opsArr)

            val empArr = JSONArray()
            employees.forEach { emp ->
                empArr.put(
                    JSONObject().apply {
                        put("id", emp.id)
                        put("name", emp.name)
                        put("email", emp.email)
                        put("password", emp.password)
                        put("role", emp.role.name)
                        put("canModifyBon", emp.canModifyBon)
                        put("canDeleteBon", emp.canDeleteBon)
                        put("canModifyFacture", emp.canModifyFacture)
                        put("canDeleteFacture", emp.canDeleteFacture)
                        put("canModifyCreance", emp.canModifyCreance)
                        put("canDeleteCreance", emp.canDeleteCreance)
                        put("canManageEmployees", emp.canManageEmployees)
                    }
                )
            }
            rootJson.put("employees", empArr)

            val preloadScript = "\n  <script>\n    window.__PRELOADED_ANDROID_STATE__ = ${rootJson.toString()};\n  </script>\n</head>"
            val enrichedHtml = if (rawHtml.contains("</head>")) {
                rawHtml.replaceFirst("</head>", preloadScript)
            } else {
                rawHtml
            }
            enrichedHtml.toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            rawHtml.toByteArray(StandardCharsets.UTF_8)
        }
    }

    /**
     * Partage le fichier autonome Merelec_Web_PC_Windows.html (avec lien direct PC) vers WhatsApp, Email ou Google Drive.
     */
    fun shareStandaloneWebPcHtml(
        context: Context,
        settings: CompanySettings,
        operations: List<OperationWithLignes>,
        partners: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        employees: List<EmployeeEntity>
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "Merelec_Web_PC_Windows.html")
            val bytes = buildStandaloneWebPcHtmlBytes(
                context = context,
                settings = settings,
                operations = operations,
                partners = partners,
                products = products,
                employees = employees
            )
            FileOutputStream(file).use { it.write(bytes) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Application Web PC Windows — ${settings.companyName}")
                putExtra(Intent.EXTRA_TITLE, "Merelec_Web_PC_Windows.html")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🖥️ Version Web pour PC Windows (${settings.companyName}) :\n\n" +
                        "1️⃣ Lien Web direct à ouvrir dans Chrome ou Edge sur votre PC Windows :\n" +
                        "$WEB_PC_DIRECT_URL\n\n" +
                        "2️⃣ Ou ouvrez directement le fichier joint « Merelec_Web_PC_Windows.html » par double-clic sur votre PC Windows (vos ${operations.size} bons/factures, ${partners.size} partenaires et ${products.size} articles y sont déjà intégrés + synchronisation temps réel Firebase + Scanner OCR Intelligent)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Envoyer la Version Web vers mon PC Windows").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            shareText(
                context,
                "Lien Version Web PC Windows",
                "Ouvrez ce lien sur votre PC Windows (Chrome / Edge) : $WEB_PC_DIRECT_URL"
            )
        }
    }
}
