package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockMovementType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Classe Room Database centrale de l'application Gestion Merélec.
 * Gère la persistance locale des Bons de Livraison (saisie_terrain & lignes_operation),
 * du Catalogue Produits (catalogue_produits), des Mouvements de Stock (mouvements_stock),
 * des Articles Éclatés (article_composition) et des Partenaires (partenaires).
 */
@Database(
    entities = [
        OperationEntity::class,
        LigneOperationEntity::class,
        PartnerEntity::class,
        ProductCatalogEntity::class,
        ArticleCompositionEntity::class,
        StockMovementEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saisie_terrain_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialSampleData(database.operationDao())
                    }
                }
            }
        }

        private suspend fun populateInitialSampleData(dao: OperationDao) {
            val now = System.currentTimeMillis()
            val hour = 3600 * 1000L

            // 1. Initial Sample Products Catalog with Stock & Alert Thresholds
            val p1 = ProductCatalogEntity(
                id = "prod-cable-15",
                name = "Câble Électrique 1.5mm (Rouleau 100m)",
                defaultUnitPrice = 3200.0,
                prixAchatMoyen = 2650.0,
                category = "Câblage",
                marque = "Elsewedy",
                unit = "Rouleau",
                stockActuel = 18.0,
                seuilAlerte = 5.0,
                reference = "CAB-15-100"
            )
            val p2 = ProductCatalogEntity(
                id = "prod-cable-25",
                name = "Câble Électrique 2.5mm (Rouleau 100m)",
                defaultUnitPrice = 4500.0,
                prixAchatMoyen = 3900.0,
                category = "Câblage",
                marque = "Elsewedy",
                unit = "Rouleau",
                stockActuel = 4.0, // Sous le seuil d'alerte (5.0)
                seuilAlerte = 5.0,
                reference = "CAB-25-100"
            )
            val p3 = ProductCatalogEntity(
                id = "prod-disj-16a",
                name = "Disjoncteur Bipolaire 16A Schneider",
                defaultUnitPrice = 680.0,
                prixAchatMoyen = 520.0,
                category = "Protection",
                marque = "Schneider",
                unit = "U",
                stockActuel = 35.0,
                seuilAlerte = 10.0,
                reference = "DISJ-16A-SCH"
            )
            val p4 = ProductCatalogEntity(
                id = "prod-disj-32a",
                name = "Disjoncteur Bipolaire 32A Hager",
                defaultUnitPrice = 920.0,
                prixAchatMoyen = 710.0,
                category = "Protection",
                marque = "Hager",
                unit = "U",
                stockActuel = 3.0, // Sous le seuil d'alerte (8.0)
                seuilAlerte = 8.0,
                reference = "DISJ-32A-HAG"
            )
            val p5 = ProductCatalogEntity(
                id = "prod-diff-40a",
                name = "Différentiel 40A 30mA",
                defaultUnitPrice = 3600.0,
                prixAchatMoyen = 2900.0,
                category = "Protection",
                marque = "Schneider",
                unit = "U",
                stockActuel = 12.0,
                seuilAlerte = 5.0,
                reference = "DIFF-40A-30"
            )
            val p6 = ProductCatalogEntity(
                id = "prod-tab-24m",
                name = "Tableau Électrique Encastrable 24 Modules",
                defaultUnitPrice = 4200.0,
                prixAchatMoyen = 3300.0,
                category = "Coffrets",
                marque = "Legrand",
                unit = "U",
                stockActuel = 2.0, // Sous le seuil d'alerte (5.0)
                seuilAlerte = 5.0,
                reference = "TAB-24M-ENC"
            )
            val p7 = ProductCatalogEntity(
                id = "prod-boite-ip65",
                name = "Boîte de Dérivation Étanche IP65",
                defaultUnitPrice = 250.0,
                prixAchatMoyen = 160.0,
                category = "Accessoires",
                marque = "Gewiss",
                unit = "U",
                stockActuel = 60.0,
                seuilAlerte = 15.0,
                reference = "BOITE-IP65"
            )
            val p8 = ProductCatalogEntity(
                id = "prod-proj-led50",
                name = "Projecteur LED 50W Étanche",
                defaultUnitPrice = 1850.0,
                prixAchatMoyen = 1350.0,
                category = "Éclairage",
                marque = "Philips",
                unit = "U",
                stockActuel = 0.0, // Rupture de stock
                seuilAlerte = 5.0,
                reference = "PROJ-LED-50W"
            )

            val initialProducts = listOf(p1, p2, p3, p4, p5, p6, p7, p8)
            dao.insertCatalogProducts(initialProducts)

            // 2. Initial Sample Operations (Bons de Livraison)
            val opSupplierId = "BLF-2026-001"
            val opSupplier = OperationEntity(
                id = opSupplierId,
                dateTime = now - 5 * hour,
                typeOperation = OperationType.BL_FOURNISSEUR,
                clientFournisseur = "Grossiste Câblerie & Matériel Blida",
                montantVerse = 50000.0,
                modeReglement = "Virement",
                notes = "Approvisionnement dépôt central"
            )
            val opSupplierLignes = listOf(
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = opSupplierId,
                    produit = p2.name,
                    quantite = 14.0,
                    prixUnitaire = 3900.0
                ),
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = opSupplierId,
                    produit = p3.name,
                    quantite = 60.0,
                    prixUnitaire = 520.0
                )
            )
            dao.saveOperationWithLignes(opSupplier, opSupplierLignes)

            val op1Id = "BLC-2026-001"
            val op1 = OperationEntity(
                id = op1Id,
                dateTime = now - 2 * hour,
                typeOperation = OperationType.BL_CLIENT,
                clientFournisseur = "SARL Etoile Distribution Alger",
                montantVerse = 15000.0,
                modeReglement = "Espèces",
                notes = "Livraison zone industrielle Rouiba, acompte versé au chauffeur"
            )
            val op1Lignes = listOf(
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = op1Id,
                    produit = "Câble Électrique 1.5mm (Rouleau 100m)",
                    quantite = 1.0,
                    prixUnitaire = 3200.0,
                    parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                ),
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = op1Id,
                    produit = "Câble Électrique 2.5mm (Rouleau 100m)",
                    quantite = 1.0,
                    prixUnitaire = 4500.0,
                    parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                ),
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = op1Id,
                    produit = "Boîte de Dérivation Étanche IP65",
                    quantite = 9.0,
                    prixUnitaire = 250.0,
                    parentArticle = "Lot Câblage & Dérivation Chantier (Article Éclaté)"
                ),
                LigneOperationEntity(
                    id = UUID.randomUUID().toString(),
                    operationId = op1Id,
                    produit = p3.name,
                    quantite = 25.0,
                    prixUnitaire = 680.0
                )
            )
            dao.saveOperationWithLignes(op1, op1Lignes)

            // 3. Initial Stock Movements linked to Bons de Livraison
            val initialMovements = listOf(
                StockMovementEntity(
                    productId = p2.id,
                    productName = p2.name,
                    type = StockMovementType.ENTREE,
                    quantity = 14.0,
                    previousStock = 0.0,
                    newStock = 14.0,
                    unit = p2.unit,
                    operationId = opSupplierId,
                    partnerName = opSupplier.clientFournisseur,
                    reason = "Réception BL Fournisseur $opSupplierId",
                    timestamp = now - 5 * hour
                ),
                StockMovementEntity(
                    productId = p3.id,
                    productName = p3.name,
                    type = StockMovementType.ENTREE,
                    quantity = 60.0,
                    previousStock = 0.0,
                    newStock = 60.0,
                    unit = p3.unit,
                    operationId = opSupplierId,
                    partnerName = opSupplier.clientFournisseur,
                    reason = "Réception BL Fournisseur $opSupplierId",
                    timestamp = now - 5 * hour
                ),
                StockMovementEntity(
                    productId = p2.id,
                    productName = p2.name,
                    type = StockMovementType.SORTIE,
                    quantity = 10.0,
                    previousStock = 14.0,
                    newStock = 4.0,
                    unit = p2.unit,
                    operationId = op1Id,
                    partnerName = op1.clientFournisseur,
                    reason = "Livraison BL Client $op1Id",
                    timestamp = now - 2 * hour
                ),
                StockMovementEntity(
                    productId = p3.id,
                    productName = p3.name,
                    type = StockMovementType.SORTIE,
                    quantity = 25.0,
                    previousStock = 60.0,
                    newStock = 35.0,
                    unit = p3.unit,
                    operationId = op1Id,
                    partnerName = op1.clientFournisseur,
                    reason = "Livraison BL Client $op1Id",
                    timestamp = now - 2 * hour
                )
            )
            dao.insertStockMovements(initialMovements)

            // 4. Initial Sample Clients
            val initialClients = listOf(
                PartnerEntity(name = "SARL Etoile Distribution Alger", type = PartnerType.CLIENT, phone = "0550 12 34 56", address = "Zone Industrielle Rouiba, Alger"),
                PartnerEntity(name = "Établissement Benali & Frères Oran", type = PartnerType.CLIENT, phone = "0561 98 76 54", address = "Zone des sièges, Oran"),
                PartnerEntity(name = "Entreprise Électricité Boumerdes", type = PartnerType.CLIENT, phone = "0770 44 55 66", address = "Centre Ville, Boumerdes"),
                PartnerEntity(name = "Chantier Résidence El Bahia", type = PartnerType.CLIENT, phone = "0662 33 22 11", address = "Chéraga, Alger")
            )
            dao.insertPartners(initialClients)

            // 5. Initial Sample Fournisseurs
            val initialSuppliers = listOf(
                PartnerEntity(name = "Grossiste Câblerie & Matériel Blida", type = PartnerType.FOURNISSEUR, phone = "025 30 11 22", address = "Route Principale, Blida"),
                PartnerEntity(name = "Import Quincaillerie & Matériaux Blida", type = PartnerType.FOURNISSEUR, phone = "025 40 50 60", address = "Zone d'activités, Blida"),
                PartnerEntity(name = "Manufacture Appareillage Électrique Sétif", type = PartnerType.FOURNISSEUR, phone = "036 80 90 00", address = "Zone Industrielle, Sétif")
            )
            dao.insertPartners(initialSuppliers)
        }
    }
}
