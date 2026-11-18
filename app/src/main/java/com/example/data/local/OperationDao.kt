package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ArticleCompositionEntity
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.example.data.model.ProductCatalogWithComposition
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockMovementType
import kotlinx.coroutines.flow.Flow

/**
 * Interface DAO Room gérant les opérations de lecture et d'écriture :
 * - Bons de livraison (BL Client, BL Fournisseur) et autres opérations commerciales
 * - Lignes d'opérations (articles livrés / réceptionnés)
 * - Catalogue Produits, Seuils d'alerte bas et Mouvements d'Entrées / Sorties de stock
 * - Répertoire Clients & Fournisseurs
 */
@Dao
interface OperationDao {

    // --- Bons de Livraison & Opérations de Saisie Terrain ---

    @Transaction
    @Query("SELECT * FROM saisie_terrain ORDER BY date_heure DESC")
    fun getAllOperationsWithLignes(): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE type_operation = :type ORDER BY date_heure DESC")
    fun getOperationsByType(type: OperationType): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE type_operation IN ('BL_CLIENT', 'BL_FOURNISSEUR') ORDER BY date_heure DESC")
    fun getAllBonsDeLivraison(): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE type_operation = 'BL_CLIENT' ORDER BY date_heure DESC")
    fun getBonsDeLivraisonClients(): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE type_operation = 'BL_FOURNISSEUR' ORDER BY date_heure DESC")
    fun getBonsDeLivraisonFournisseurs(): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE client_fournisseur LIKE '%' || :partnerQuery || '%' ORDER BY date_heure DESC")
    fun getOperationsByPartner(partnerQuery: String): Flow<List<OperationWithLignes>>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE id_unique = :id")
    fun getOperationWithLignesById(id: String): Flow<OperationWithLignes?>

    @Transaction
    @Query("SELECT * FROM saisie_terrain WHERE id_unique = :id")
    suspend fun getOperationWithLignesByIdOnce(id: String): OperationWithLignes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationEntity)

    @Update
    suspend fun updateOperation(operation: OperationEntity)

    @Delete
    suspend fun deleteOperation(operation: OperationEntity)

    @Query("DELETE FROM saisie_terrain WHERE id_unique = :id")
    suspend fun deleteOperationById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLigne(ligne: LigneOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLignes(lignes: List<LigneOperationEntity>)

    @Update
    suspend fun updateLigne(ligne: LigneOperationEntity)

    @Delete
    suspend fun deleteLigne(ligne: LigneOperationEntity)

    @Query("DELETE FROM lignes_operation WHERE id_operation = :operationId")
    suspend fun deleteLignesByOperationId(operationId: String)

    @Query("DELETE FROM lignes_operation WHERE id_ligne = :idLigne")
    suspend fun deleteLigneById(idLigne: String)

    @Query("SELECT DISTINCT client_fournisseur FROM saisie_terrain WHERE client_fournisseur != '' ORDER BY date_heure DESC LIMIT 20")
    fun getRecentPartners(): Flow<List<String>>

    @Query("SELECT DISTINCT produit FROM lignes_operation WHERE produit != '' ORDER BY id_ligne DESC LIMIT 30")
    fun getRecentProducts(): Flow<List<String>>

    @Transaction
    suspend fun saveOperationWithLignes(operation: OperationEntity, lignes: List<LigneOperationEntity>) {
        insertOperation(operation)
        deleteLignesByOperationId(operation.id)
        if (operation.typeOperation != OperationType.REGLEMENT && lignes.isNotEmpty()) {
            insertLignes(lignes)
        }
    }

    // --- Partenaires (Clients & Fournisseurs importés) ---

    @Query("SELECT * FROM partenaires ORDER BY nom ASC")
    fun getAllPartners(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partenaires WHERE type = :type ORDER BY nom ASC")
    fun getPartnersByType(type: PartnerType): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partenaires WHERE id_partenaire = :id")
    fun getPartnerById(id: String): Flow<PartnerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity)

    @Update
    suspend fun updatePartner(partner: PartnerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartners(partners: List<PartnerEntity>)

    @Delete
    suspend fun deletePartner(partner: PartnerEntity)

    @Query("DELETE FROM partenaires WHERE id_partenaire = :id")
    suspend fun deletePartnerById(id: String)

    @Query("DELETE FROM partenaires WHERE type = :type")
    suspend fun clearPartnersByType(type: PartnerType)

    // --- Catalogue Produits (Articles importés) ---

    @Query("SELECT * FROM catalogue_produits ORDER BY designation ASC")
    fun getAllCatalogProducts(): Flow<List<ProductCatalogEntity>>

    @Query("SELECT * FROM catalogue_produits WHERE id_produit = :id")
    fun getCatalogProductById(id: String): Flow<ProductCatalogEntity?>

    @Query("SELECT * FROM catalogue_produits WHERE LOWER(TRIM(designation)) = LOWER(TRIM(:designation)) LIMIT 1")
    suspend fun getCatalogProductByNameOnce(designation: String): ProductCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogProduct(product: ProductCatalogEntity)

    @Update
    suspend fun updateCatalogProduct(product: ProductCatalogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogProducts(products: List<ProductCatalogEntity>)

    @Delete
    suspend fun deleteCatalogProduct(product: ProductCatalogEntity)

    @Query("DELETE FROM catalogue_produits WHERE id_produit = :id")
    suspend fun deleteCatalogProductById(id: String)

    @Query("DELETE FROM catalogue_produits")
    suspend fun clearAllCatalogProducts()

    // --- Gestion du Stock & Alertes de Seuil Bas ---

    @Query("UPDATE catalogue_produits SET stock_actuel = :newStock WHERE id_produit = :id")
    suspend fun updateStock(id: String, newStock: Double)

    @Query("UPDATE catalogue_produits SET seuil_alerte = :newThreshold WHERE id_produit = :id")
    suspend fun updateSeuilAlerte(id: String, newThreshold: Double)

    @Query("UPDATE catalogue_produits SET stock_actuel = MAX(0.0, stock_actuel + :delta) WHERE LOWER(TRIM(designation)) = LOWER(TRIM(:designation))")
    suspend fun adjustStockByName(designation: String, delta: Double)

    @Query("SELECT * FROM catalogue_produits WHERE stock_actuel <= seuil_alerte ORDER BY stock_actuel ASC")
    fun getLowStockProducts(): Flow<List<ProductCatalogEntity>>

    @Query("SELECT * FROM catalogue_produits WHERE stock_actuel <= 0 ORDER BY designation ASC")
    fun getOutOfStockProducts(): Flow<List<ProductCatalogEntity>>

    // --- Mouvements de Stock (Entrées / Sorties / Inventaire) ---

    @Query("SELECT * FROM mouvements_stock ORDER BY date_mouvement DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM mouvements_stock WHERE id_produit = :productId ORDER BY date_mouvement DESC")
    fun getStockMovementsByProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM mouvements_stock WHERE type_mouvement = :type ORDER BY date_mouvement DESC")
    fun getStockMovementsByType(type: StockMovementType): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM mouvements_stock WHERE id_bon_livraison = :operationId ORDER BY date_mouvement DESC")
    fun getStockMovementsForOperation(operationId: String): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM mouvements_stock WHERE id_mouvement = :id")
    suspend fun deleteStockMovementById(id: String)

    @Query("DELETE FROM mouvements_stock")
    suspend fun clearAllStockMovements()

    @Transaction
    suspend fun recordStockMovementAndApply(movement: StockMovementEntity) {
        updateStock(movement.productId, movement.newStock)
        insertStockMovement(movement)
    }

    // --- Articles Éclatés & Nomenclatures (Compositions) ---

    @Query("SELECT * FROM article_composition WHERE id_article_parent = :parentId")
    fun getCompositionsForProduct(parentId: String): Flow<List<ArticleCompositionEntity>>

    @Query("SELECT * FROM article_composition WHERE id_article_parent = :parentId")
    suspend fun getCompositionsForProductOnce(parentId: String): List<ArticleCompositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComposition(composition: ArticleCompositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompositions(compositions: List<ArticleCompositionEntity>)

    @Query("DELETE FROM article_composition WHERE id_article_parent = :parentId")
    suspend fun deleteCompositionsForProduct(parentId: String)

    @Query("DELETE FROM article_composition WHERE id_composition = :id")
    suspend fun deleteCompositionById(id: String)

    @Query("DELETE FROM article_composition")
    suspend fun clearAllCompositions()

    @Query("DELETE FROM partenaires")
    suspend fun clearAllPartners()

    @Query("DELETE FROM lignes_operation")
    suspend fun clearAllOperationLines()

    @Query("DELETE FROM saisie_terrain")
    suspend fun clearAllOperations()

    @Transaction
    suspend fun resetEntireLocalDatabase() {
        clearAllOperationLines()
        clearAllOperations()
        clearAllStockMovements()
        clearAllCompositions()
        clearAllCatalogProducts()
        clearAllPartners()
    }

    @Transaction
    @Query("SELECT * FROM catalogue_produits WHERE is_article_eclate = 1 OR is_article_kit = 1 ORDER BY designation ASC")
    fun getAllArticlesEclatesWithComposition(): Flow<List<ProductCatalogWithComposition>>

    @Transaction
    suspend fun saveArticleEclateWithComposition(product: ProductCatalogEntity, compositions: List<ArticleCompositionEntity>) {
        val sumOfComponents = compositions.sumOf { (it.prixUnitaireEstime ?: 0.0) * it.quantite }
        val updatedProduct = when {
            product.isArticleEclate -> product.copy(
                isArticleEclate = true,
                isArticleKit = false,
                defaultUnitPrice = if (sumOfComponents > 0.0) sumOfComponents else product.defaultUnitPrice
            )
            product.isArticleKit -> product.copy(
                isArticleKit = true,
                isArticleEclate = false
            )
            else -> product.copy(isArticleEclate = true)
        }
        insertCatalogProduct(updatedProduct)
        deleteCompositionsForProduct(updatedProduct.id)
        if (compositions.isNotEmpty()) {
            insertCompositions(compositions.map { it.copy(parentProductId = updatedProduct.id) })
        }
    }
}
