package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class NomenclatureType(val label: String, val badgeLabel: String) {
    STANDARD("Article Standard", "Standard"),
    ARTICLE_KIT("Article Kit (Nomenclature commerciale)", "🎁 Article Kit"),
    ARTICLE_ECLATE("Article Éclaté (Kit éclaté)", "⚡ Article Éclaté")
}

@Entity(tableName = "catalogue_produits")
data class ProductCatalogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_produit")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "designation")
    val name: String,

    @ColumnInfo(name = "prix_unitaire_defaut")
    val defaultUnitPrice: Double? = null,

    @ColumnInfo(name = "categorie")
    val category: String = "Général",

    @ColumnInfo(name = "unite")
    val unit: String = "U",

    @ColumnInfo(name = "stock_actuel")
    val stockActuel: Double = 0.0,

    @ColumnInfo(name = "seuil_alerte")
    val seuilAlerte: Double = 5.0,

    @ColumnInfo(name = "is_article_eclate")
    val isArticleEclate: Boolean = false, // Article Éclaté (ou Kit éclaté) : éclate immédiatement en composants à la saisie

    @ColumnInfo(name = "is_article_kit")
    val isArticleKit: Boolean = false, // Article Kit (Nomenclature commerciale) : 1 seule ligne parent forfaitaire + déstockage auto en arrière-plan

    @ColumnInfo(name = "prix_achat_moyen")
    val prixAchatMoyen: Double? = null,

    @ColumnInfo(name = "code_barre")
    val barcode: String = "",

    @ColumnInfo(name = "reference")
    val reference: String = "",

    @ColumnInfo(name = "marque")
    val marque: String = ""
) {
    val famille: String
        get() = category

    val prixAchat: Double?
        get() = prixAchatMoyen

    val prixVente: Double?
        get() = defaultUnitPrice

    val isComposite: Boolean
        get() = isArticleKit || isArticleEclate

    val nomenclatureType: NomenclatureType
        get() = when {
            isArticleKit -> NomenclatureType.ARTICLE_KIT
            isArticleEclate -> NomenclatureType.ARTICLE_ECLATE
            else -> NomenclatureType.STANDARD
        }
}
