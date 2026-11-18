package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class StockMovementType(val label: String, val isPositive: Boolean) {
    ENTREE("Entrée Marchandise", true),
    SORTIE("Sortie / Livraison", false),
    AJUSTEMENT("Ajustement Inventaire", true)
}

/**
 * Table Room: mouvements_stock
 * Trace toutes les entrées et sorties de marchandises (issues des Bons de Livraison
 * Clients/Fournisseurs, Factures, Avoirs ou saisies manuelles au dépôt).
 */
@Entity(
    tableName = "mouvements_stock",
    indices = [
        Index(value = ["id_produit"]),
        Index(value = ["id_bon_livraison"]),
        Index(value = ["date_mouvement"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_mouvement")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "id_produit")
    val productId: String,

    @ColumnInfo(name = "designation_produit")
    val productName: String,

    @ColumnInfo(name = "type_mouvement")
    val type: StockMovementType,

    @ColumnInfo(name = "quantite")
    val quantity: Double,

    @ColumnInfo(name = "stock_precedent")
    val previousStock: Double,

    @ColumnInfo(name = "nouveau_stock")
    val newStock: Double,

    @ColumnInfo(name = "unite")
    val unit: String = "U",

    @ColumnInfo(name = "id_bon_livraison")
    val operationId: String? = null,

    @ColumnInfo(name = "partenaire")
    val partnerName: String = "",

    @ColumnInfo(name = "motif_notes")
    val reason: String = "",

    @ColumnInfo(name = "date_mouvement")
    val timestamp: Long = System.currentTimeMillis()
)
