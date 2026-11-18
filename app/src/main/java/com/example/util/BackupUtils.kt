package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

data class BackupContent(
    val timestamp: Long,
    val dateFormatted: String,
    val operationsCount: Int,
    val clientsCount: Int,
    val suppliersCount: Int,
    val productsCount: Int,
    val operations: List<Pair<OperationEntity, List<LigneOperationEntity>>>,
    val partners: List<PartnerEntity>,
    val products: List<ProductCatalogEntity>,
    val compositions: List<ArticleCompositionEntity> = emptyList()
)

object BackupUtils {

    /**
     * Serialize the complete database into a clean JSON string
     */
    fun createBackupJson(
        operations: List<OperationWithLignes>,
        clients: List<PartnerEntity>,
        suppliers: List<PartnerEntity>,
        products: List<ProductCatalogEntity>,
        compositions: List<ArticleCompositionEntity> = emptyList()
    ): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()

        root.put("app_name", "Gestion Merelec")
        root.put("version", "2.0")
        root.put("backup_timestamp", now)
        root.put("backup_date", DateUtils.formatDateTime(now))

        // 1. Operations (BL Client, BL Fournisseur, Factures, BC, Devis, Avoirs, Règlements)
        val opsArray = JSONArray()
        for (item in operations) {
            val opObj = JSONObject()
            val op = item.operation
            opObj.put("id_unique", op.id)
            opObj.put("date_heure", op.dateTime)
            opObj.put("type_operation", op.typeOperation.name)
            opObj.put("client_fournisseur", op.clientFournisseur)
            opObj.put("montant_verse", op.montantVerse)
            opObj.put("mode_reglement", op.modeReglement)
            opObj.put("notes", op.notes)
            opObj.put("appliquer_tva", op.appliquerTva)
            opObj.put("taux_tva", op.tauxTva)
            opObj.put("statut", op.statut)

            // Lines
            val linesArray = JSONArray()
            for (ligne in item.lignes) {
                val lineObj = JSONObject()
                lineObj.put("id_ligne", ligne.id)
                lineObj.put("id_operation", ligne.operationId)
                lineObj.put("produit", ligne.produit)
                lineObj.put("quantite", ligne.quantite)
                if (ligne.prixUnitaire != null) {
                    lineObj.put("prix_unitaire", ligne.prixUnitaire)
                } else {
                    lineObj.put("prix_unitaire", JSONObject.NULL)
                }
                linesArray.put(lineObj)
            }
            opObj.put("lignes", linesArray)
            opsArray.put(opObj)
        }
        root.put("operations", opsArray)

        // 2. Clients
        val clientsArray = JSONArray()
        for (c in clients) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("nom", c.name)
            cObj.put("telephone", c.phone)
            cObj.put("adresse", c.address)
            cObj.put("notes", c.notes)
            cObj.put("rc", c.rc)
            cObj.put("article_imposition", c.articleImposition)
            cObj.put("nif", c.nif)
            cObj.put("nis", c.nis)
            cObj.put("nin", c.nin)
            cObj.put("rib", c.rib)
            clientsArray.put(cObj)
        }
        root.put("clients", clientsArray)

        // 3. Fournisseurs
        val suppliersArray = JSONArray()
        for (s in suppliers) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("nom", s.name)
            sObj.put("telephone", s.phone)
            sObj.put("adresse", s.address)
            sObj.put("notes", s.notes)
            sObj.put("rc", s.rc)
            sObj.put("article_imposition", s.articleImposition)
            sObj.put("nif", s.nif)
            sObj.put("nis", s.nis)
            sObj.put("nin", s.nin)
            sObj.put("rib", s.rib)
            suppliersArray.put(sObj)
        }
        root.put("fournisseurs", suppliersArray)

        // 4. Catalogue Produits
        val prodsArray = JSONArray()
        for (p in products) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("designation", p.name)
            if (p.defaultUnitPrice != null) {
                pObj.put("prix_unitaire", p.defaultUnitPrice)
                pObj.put("prix_vente", p.defaultUnitPrice)
            } else {
                pObj.put("prix_unitaire", JSONObject.NULL)
                pObj.put("prix_vente", JSONObject.NULL)
            }
            pObj.put("categorie", p.category)
            pObj.put("famille", p.category)
            pObj.put("marque", p.marque)
            pObj.put("unite", p.unit)
            pObj.put("stock_actuel", p.stockActuel)
            pObj.put("seuil_alerte", p.seuilAlerte)
            pObj.put("is_article_eclate", p.isArticleEclate)
            if (p.prixAchatMoyen != null) {
                pObj.put("prix_achat_moyen", p.prixAchatMoyen)
            } else {
                pObj.put("prix_achat_moyen", JSONObject.NULL)
            }
            pObj.put("code_barre", p.barcode)
            pObj.put("reference", p.reference)
            prodsArray.put(pObj)
        }
        root.put("catalogue_produits", prodsArray)

        // 5. Compositions (Articles éclatés)
        val compsArray = JSONArray()
        for (comp in compositions) {
            val compObj = JSONObject()
            compObj.put("id", comp.id)
            compObj.put("id_article_parent", comp.parentProductId)
            compObj.put("designation_composant", comp.componentName)
            compObj.put("quantite", comp.quantite)
            if (comp.prixUnitaireEstime != null) {
                compObj.put("prix_unitaire_estime", comp.prixUnitaireEstime)
            } else {
                compObj.put("prix_unitaire_estime", JSONObject.NULL)
            }
            compObj.put("unite", comp.unit)
            compsArray.put(compObj)
        }
        root.put("compositions_articles", compsArray)

        return root.toString(2)
    }

    /**
     * Parse backup JSON text into structured BackupContent
     */
    fun parseBackupJson(jsonString: String): BackupContent? {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("operations") && !root.has("clients") && !root.has("catalogue_produits")) {
                return null
            }
            val timestamp = root.optLong("backup_timestamp", System.currentTimeMillis())
            val dateFormatted = root.optString("backup_date", DateUtils.formatDateTime(timestamp))

            // Operations
            val opsList = mutableListOf<Pair<OperationEntity, List<LigneOperationEntity>>>()
            val opsArray = root.optJSONArray("operations") ?: JSONArray()
            for (i in 0 until opsArray.length()) {
                val opObj = opsArray.getJSONObject(i)
                val typeStr = opObj.optString("type_operation", OperationType.BL_CLIENT.name)
                val type = try {
                    OperationType.valueOf(typeStr)
                } catch (e: Exception) {
                    OperationType.BL_CLIENT
                }

                val op = OperationEntity(
                    id = opObj.getString("id_unique"),
                    dateTime = opObj.optLong("date_heure", System.currentTimeMillis()),
                    typeOperation = type,
                    clientFournisseur = opObj.optString("client_fournisseur", ""),
                    montantVerse = opObj.optDouble("montant_verse", 0.0),
                    modeReglement = opObj.optString("mode_reglement", "Espèces"),
                    notes = opObj.optString("notes", ""),
                    appliquerTva = opObj.optBoolean("appliquer_tva", false),
                    tauxTva = opObj.optDouble("taux_tva", 19.0),
                    statut = opObj.optString("statut", "Validé")
                )

                val linesList = mutableListOf<LigneOperationEntity>()
                val linesArray = opObj.optJSONArray("lignes") ?: JSONArray()
                for (j in 0 until linesArray.length()) {
                    val lineObj = linesArray.getJSONObject(j)
                    val pu = if (lineObj.isNull("prix_unitaire")) null else lineObj.optDouble("prix_unitaire")
                    linesList.add(
                        LigneOperationEntity(
                            id = lineObj.optString("id_ligne", UUID.randomUUID().toString()),
                            operationId = op.id,
                            produit = lineObj.optString("produit", ""),
                            quantite = lineObj.optDouble("quantite", 1.0),
                            prixUnitaire = pu
                        )
                    )
                }

                opsList.add(Pair(op, linesList))
            }

            // Partners (Clients & Fournisseurs)
            val partnersList = mutableListOf<PartnerEntity>()
            val clientsArray = root.optJSONArray("clients") ?: JSONArray()
            for (i in 0 until clientsArray.length()) {
                val cObj = clientsArray.getJSONObject(i)
                partnersList.add(
                    PartnerEntity(
                        id = cObj.optString("id", UUID.randomUUID().toString()),
                        name = cObj.optString("nom", ""),
                        type = PartnerType.CLIENT,
                        phone = cObj.optString("telephone", ""),
                        address = cObj.optString("adresse", ""),
                        notes = cObj.optString("notes", ""),
                        rc = cObj.optString("rc", ""),
                        articleImposition = cObj.optString("article_imposition", ""),
                        nif = cObj.optString("nif", ""),
                        nis = cObj.optString("nis", ""),
                        nin = cObj.optString("nin", ""),
                        rib = cObj.optString("rib", "")
                    )
                )
            }

            val suppliersArray = root.optJSONArray("fournisseurs") ?: JSONArray()
            for (i in 0 until suppliersArray.length()) {
                val sObj = suppliersArray.getJSONObject(i)
                partnersList.add(
                    PartnerEntity(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        name = sObj.optString("nom", ""),
                        type = PartnerType.FOURNISSEUR,
                        phone = sObj.optString("telephone", ""),
                        address = sObj.optString("adresse", ""),
                        notes = sObj.optString("notes", ""),
                        rc = sObj.optString("rc", ""),
                        articleImposition = sObj.optString("article_imposition", ""),
                        nif = sObj.optString("nif", ""),
                        nis = sObj.optString("nis", ""),
                        nin = sObj.optString("nin", ""),
                        rib = sObj.optString("rib", "")
                    )
                )
            }

            // Products
            val productsList = mutableListOf<ProductCatalogEntity>()
            val prodsArray = root.optJSONArray("catalogue_produits") ?: JSONArray()
            for (i in 0 until prodsArray.length()) {
                val pObj = prodsArray.getJSONObject(i)
                val pu = when {
                    !pObj.isNull("prix_vente") -> pObj.optDouble("prix_vente")
                    !pObj.isNull("prix_unitaire") -> pObj.optDouble("prix_unitaire")
                    else -> null
                }
                val pam = if (pObj.isNull("prix_achat_moyen")) null else pObj.optDouble("prix_achat_moyen")
                val familleValue = pObj.optString("famille", pObj.optString("categorie", "Général"))
                productsList.add(
                    ProductCatalogEntity(
                        id = pObj.optString("id", UUID.randomUUID().toString()),
                        name = pObj.optString("designation", ""),
                        defaultUnitPrice = pu,
                        category = familleValue.ifBlank { "Général" },
                        marque = pObj.optString("marque", ""),
                        unit = pObj.optString("unite", "U"),
                        stockActuel = pObj.optDouble("stock_actuel", 0.0),
                        seuilAlerte = pObj.optDouble("seuil_alerte", 5.0),
                        isArticleEclate = pObj.optBoolean("is_article_eclate", false),
                        prixAchatMoyen = pam,
                        barcode = pObj.optString("code_barre", ""),
                        reference = pObj.optString("reference", "")
                    )
                )
            }

            // Compositions
            val compositionsList = mutableListOf<ArticleCompositionEntity>()
            val compsArray = root.optJSONArray("compositions_articles") ?: JSONArray()
            for (i in 0 until compsArray.length()) {
                val compObj = compsArray.getJSONObject(i)
                val pue = if (compObj.isNull("prix_unitaire_estime")) null else compObj.optDouble("prix_unitaire_estime")
                compositionsList.add(
                    ArticleCompositionEntity(
                        id = compObj.optString("id", UUID.randomUUID().toString()),
                        parentProductId = compObj.optString("id_article_parent", ""),
                        componentName = compObj.optString("designation_composant", ""),
                        quantite = compObj.optDouble("quantite", 1.0),
                        prixUnitaireEstime = pue,
                        unit = compObj.optString("unite", "U")
                    )
                )
            }

            BackupContent(
                timestamp = timestamp,
                dateFormatted = dateFormatted,
                operationsCount = opsList.size,
                clientsCount = clientsArray.length(),
                suppliersCount = suppliersArray.length(),
                productsCount = productsList.size,
                operations = opsList,
                partners = partnersList,
                products = productsList,
                compositions = compositionsList
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write backup data to an output stream (used by Google Drive document picker)
     */
    fun writeBackupToStream(outputStream: OutputStream, jsonContent: String) {
        outputStream.use { os ->
            os.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
            os.flush()
        }
    }

    /**
     * Read backup text from a document Uri
     */
    fun readBackupFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }

    /**
     * Send backup file directly to Google Drive app or Android Sharesheet
     */
    fun shareBackupToDriveApp(context: Context, jsonContent: String) {
        try {
            val exportDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timestamp = DateUtils.formatForExport(System.currentTimeMillis())
            val file = File(exportDir, "Gestion_Merelec_Sauvegarde_$timestamp.json")

            FileOutputStream(file).use { fos ->
                fos.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gestion Merelec - Sauvegarde Google Drive")
                putExtra(Intent.EXTRA_TITLE, file.name)
                setPackage("com.google.android.apps.docs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Gestion Merelec - Sauvegarde Google Drive")
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Sauvegarder dans Google Drive ou partager")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            ExportUtils.shareText(context, "Sauvegarde_Gestion_Merelec.json", jsonContent)
        }
    }
}
