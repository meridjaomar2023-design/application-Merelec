package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import java.util.Locale

/**
 * Représentation hiérarchique des lignes d'un bon pour afficher :
 * - les articles standards sur 1 ligne,
 * - les Articles Éclatés avec le nom de l'Article Éclaté comme PARENT et ses composés comme FILS.
 */
sealed class GroupedOperationLine {
    abstract val displayIndex: Int

    data class StandardLine(
        override val displayIndex: Int,
        val ligne: LigneOperationEntity
    ) : GroupedOperationLine()

    data class ExplodedParentGroup(
        override val displayIndex: Int,
        val parentName: String,
        val children: List<LigneOperationEntity>
    ) : GroupedOperationLine() {
        val totalHt: Double
            get() = children.sumOf { it.totalLigne }

        val totalQuantite: Double
            get() = children.sumOf { it.quantite }
    }
}

fun groupLignesByParent(
    lignes: List<LigneOperationEntity>,
    eclateCompositionsMap: Map<String, List<ArticleCompositionEntity>> = emptyMap()
): List<GroupedOperationLine> {
    if (lignes.isEmpty()) return emptyList()
    val result = mutableListOf<GroupedOperationLine>()
    var i = 0
    var currentDisplayIndex = 1

    while (i < lignes.size) {
        val current = lignes[i]
        val parentName = current.parentArticle.trim()

        if (parentName.isNotEmpty()) {
            // Regrouper tous les composés (fils) consécutifs appartenant au même Article Éclaté parent
            val children = mutableListOf<LigneOperationEntity>()
            while (i < lignes.size && lignes[i].parentArticle.trim().equals(parentName, ignoreCase = true)) {
                children.add(lignes[i])
                i++
            }
            result.add(
                GroupedOperationLine.ExplodedParentGroup(
                    displayIndex = currentDisplayIndex++,
                    parentName = parentName,
                    children = children
                )
            )
        } else {
            // Vérifier si la ligne elle-même porte le nom d'un Article Éclaté connu dans le catalogue
            val matchedComps = eclateCompositionsMap[current.produit.trim().lowercase(Locale.FRENCH)]
            if (!matchedComps.isNullOrEmpty()) {
                val syntheticChildren = matchedComps.map { comp ->
                    LigneOperationEntity(
                        id = "${current.id}_${comp.id}",
                        operationId = current.operationId,
                        produit = comp.componentName,
                        quantite = comp.quantite * current.quantite,
                        prixUnitaire = comp.prixUnitaireEstime,
                        parentArticle = current.produit
                    )
                }
                result.add(
                    GroupedOperationLine.ExplodedParentGroup(
                        displayIndex = currentDisplayIndex++,
                        parentName = current.produit,
                        children = syntheticChildren
                    )
                )
            } else {
                result.add(
                    GroupedOperationLine.StandardLine(
                        displayIndex = currentDisplayIndex++,
                        ligne = current
                    )
                )
            }
            i++
        }
    }
    return result
}

data class OperationWithLignes(
    @Embedded
    val operation: OperationEntity,

    @Relation(
        parentColumn = "id_unique",
        entityColumn = "id_operation"
    )
    val lignes: List<LigneOperationEntity> = emptyList()
) {
    /**
     * Lignes regroupées avec hiérarchie Parent (Article Éclaté) -> Fils (Composés)
     */
    val groupedLignes: List<GroupedOperationLine>
        get() = groupLignesByParent(lignes)

    /**
     * Total Hors Taxes (HT):
     * SUM([Quantite] * [Prix_Unitaire])
     */
    val totalHt: Double
        get() = if (operation.typeOperation == OperationType.REGLEMENT) {
            0.0
        } else {
            lignes.sumOf { it.totalLigne }
        }

    val totalProduits: Double
        get() = totalHt

    /**
     * Montant de la TVA calculée
     */
    val montantTva: Double
        get() = if (operation.appliquerTva && operation.typeOperation != OperationType.REGLEMENT) {
            totalHt * (operation.tauxTva / 100.0)
        } else {
            0.0
        }

    /**
     * Total Toutes Taxes Comprises (TTC)
     */
    val totalTtc: Double
        get() = if (operation.typeOperation == OperationType.REGLEMENT) {
            operation.montantVerse
        } else {
            totalHt + montantTva
        }

    /**
     * Amount to display as principal depending on operation:
     * For Règlement: montantVerse
     * For BL Client / Facture / BL Fournisseur: totalTtc
     */
    val montantPrincipal: Double
        get() = if (operation.typeOperation == OperationType.REGLEMENT) {
            operation.montantVerse
        } else {
            totalTtc
        }

    /**
     * Reste à payer pour les factures et bons avec acompte ou versement partiel
     */
    val resteAPayer: Double
        get() = if (operation.typeOperation == OperationType.REGLEMENT) {
            0.0
        } else {
            (totalTtc - operation.montantVerse).coerceAtLeast(0.0)
        }

    /**
     * Nombre d'articles distincts
     */
    val nombreArticles: Int
        get() = lignes.size

    /**
     * Quantité totale cumulée
     */
    val quantiteTotale: Double
        get() = lignes.sumOf { it.quantite }
}
