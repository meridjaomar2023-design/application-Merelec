package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "article_composition")
data class ArticleCompositionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_composition")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "id_article_parent")
    val parentProductId: String, // Référence de l'article éclaté (kit parent)

    @ColumnInfo(name = "designation_composant")
    val componentName: String, // Nom du composant

    @ColumnInfo(name = "quantite")
    val quantite: Double = 1.0, // Quantité nécessaire dans le kit

    @ColumnInfo(name = "prix_unitaire_estime")
    val prixUnitaireEstime: Double? = null,

    @ColumnInfo(name = "unite")
    val unit: String = "U"
)
