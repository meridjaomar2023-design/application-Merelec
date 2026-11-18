package com.example.data.model

enum class OperationType(val label: String, val shortCode: String) {
    BL_CLIENT("BL Client", "BLC"),
    BL_FOURNISSEUR("BL Fournisseur", "BLF"),
    FACTURE("Facture", "FAC"),
    FACTURE_FOURNISSEUR("Facture Fournisseur", "FACF"),
    BON_COMMANDE("Bon de Commande", "BC"),
    DEVIS_CLIENT("Devis", "DEV"),
    AVOIR_CLIENT("Avoir Client", "AVR"),
    DEVIS_FOURNISSEUR("Devis Fournisseur", "DEVF"),
    AVOIR_FOURNISSEUR("Avoir Fournisseur", "AVRF"),
    REGLEMENT("Règlement", "REG");

    val isSalesDocument: Boolean
        get() = this == BL_CLIENT || this == FACTURE || this == BON_COMMANDE || this == DEVIS_CLIENT || this == AVOIR_CLIENT

    val isPurchaseDocument: Boolean
        get() = this == BL_FOURNISSEUR || this == FACTURE_FOURNISSEUR || this == DEVIS_FOURNISSEUR || this == AVOIR_FOURNISSEUR

    val hasProductLines: Boolean
        get() = this != REGLEMENT

    val isAvoir: Boolean
        get() = this == AVOIR_CLIENT || this == AVOIR_FOURNISSEUR

    val isDevis: Boolean
        get() = this == DEVIS_CLIENT || this == DEVIS_FOURNISSEUR

    companion object {
        fun fromLabel(label: String): OperationType {
            return entries.find {
                it.label.equals(label, ignoreCase = true) || it.shortCode.equals(label, ignoreCase = true)
            } ?: BL_CLIENT
        }
    }
}
