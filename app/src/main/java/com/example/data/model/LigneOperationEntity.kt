package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lignes_operation",
    foreignKeys = [
        ForeignKey(
            entity = OperationEntity::class,
            parentColumns = ["id_unique"],
            childColumns = ["id_operation"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_operation"])
    ]
)
data class LigneOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_ligne")
    val id: String = "", // ID_Ligne (Key)

    @ColumnInfo(name = "id_operation")
    val operationId: String = "", // ID_Operation (Ref to 1_Saisie_Terrain)

    @ColumnInfo(name = "produit")
    val produit: String = "", // Produit (Text / Enum)

    @ColumnInfo(name = "quantite")
    val quantite: Double = 1.0, // Quantite (Number)

    @ColumnInfo(name = "prix_unitaire")
    val prixUnitaire: Double? = null, // Prix_Unitaire (Price / Decimal, facultatif)

    @ColumnInfo(name = "parent_article", defaultValue = "")
    val parentArticle: String = "" // Nom de l'Article Éclaté parent lorsque cette ligne est un composé (fils)
) {
    /**
     * Total calculated for this line: Quantité * (Prix_Unitaire ?: 0.0)
     */
    val totalLigne: Double
        get() = quantite * (prixUnitaire ?: 0.0)
}
