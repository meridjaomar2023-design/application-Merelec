package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ProductCatalogWithComposition(
    @Embedded
    val product: ProductCatalogEntity,

    @Relation(
        parentColumn = "id_produit",
        entityColumn = "id_article_parent"
    )
    val composantes: List<ArticleCompositionEntity> = emptyList()
)
