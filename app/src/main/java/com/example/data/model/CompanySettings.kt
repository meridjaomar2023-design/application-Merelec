package com.example.data.model

data class CompanySettings(
    val companyName: String = "GESTION MERÉLEC",
    val activitySlogan: String = "Équipements & Réalisations Électriques",
    val address: String = "Alger, Algérie",
    val phone: String = "0550 12 34 56",
    val phoneSecondary: String = "",
    val email: String = "contact@merelec-dz.com",
    val website: String = "",
    val nif: String = "000116012345678",
    val nis: String = "099012345678901",
    val rc: String = "16/00-1234567B16",
    val articleImposition: String = "16012345678",
    val nin: String = "109801600012345001",
    val bankName: String = "BNA / CPA",
    val bankRib: String = "001 00234 1234567890 12",
    val tvaEnabledByDefault: Boolean = true,
    val defaultTvaRate: Double = 19.0,
    val reducedTvaRate: Double = 9.0,
    val currency: String = "DA",
    val defaultStockAlertThreshold: Double = 5.0,
    val defaultMarginCoefficient: Double = 1.25, // Coefficient de marge par défaut (+25%) lors de la conversion Facture Fournisseur -> Facture Client
    val useCatalogSalePriceOnConvert: Boolean = false, // true = récupérer le prix de vente du catalogue, false = appliquer le coefficient sur le prix d'achat
    val prefixFacture: String = "FAC-",
    val prefixFactureFournisseur: String = "FACF-",
    val prefixBonCommande: String = "BC-",
    val prefixBonLivraison: String = "BL-",
    val prefixBonFournisseur: String = "BF-",
    val prefixDevis: String = "DEV-",
    val prefixAvoir: String = "AVR-",
    val prefixReglement: String = "REG-",
    // Options de mise en page, logo et réorganisation des coordonnées PDF
    val pdfShowLogo: Boolean = true,
    val pdfLogoPosition: String = "LEFT", // "LEFT", "CENTER", "RIGHT"
    val pdfLogoSize: Int = 52, // 38 (Petit), 52 (Moyen), 68 (Grand)
    val pdfHeaderBlocksOrder: String = "SLOGAN,CONTACT,FISCAL,BANQUE",
    val pdfShowSlogan: Boolean = true,
    val pdfShowAddress: Boolean = true,
    val pdfShowPhone: Boolean = true,
    val pdfShowEmailWebsite: Boolean = true,
    val pdfShowFiscalIds: Boolean = true,
    val pdfShowBankRib: Boolean = true,
    // Options de colonnes et sections pour Devis, Bons & Factures
    val pdfDocColumnsOrder: String = "NUMERO,DESIGNATION,QUANTITE,UNITE,PU_HT,TOTAL_HT",
    val pdfShowPartnerFiscal: Boolean = true,
    val pdfShowPaymentMethod: Boolean = true,
    val pdfShowKitComponents: Boolean = false,
    val pdfShowNotes: Boolean = true,
    val pdfShowConversionNote: Boolean = true, // Afficher ou masquer la mention "Document converti à partir de la facture N°..." en bas du PDF
    val pdfShowAmountInWords: Boolean = true,
    val pdfShowSignatures: Boolean = true,
    val pdfShowStampAndSignature: Boolean = true,
    val pdfStampScale: Int = 130, // 80..220 (% d'agrandissement du cachet humide)
    val pdfCustomFooter: String = "",
    // Options pour Créances PDF
    val pdfCreanceColumnsOrder: String = "NUMERO,CLIENT,TELEPHONE_VILLE,TOTAL_FACTURE,TOTAL_ENCAISSE,CREANCE_DUE,TAUX_RECOUVREMENT",
    val pdfCreanceShowSummary: Boolean = true,
    // Options pour Catalogue Articles PDF
    val pdfArticleColumnsOrder: String = "NUMERO,REFERENCE,DESIGNATION,FAMILLE,MARQUE,PRIX_VENTE,PRIX_ACHAT,STOCK",
    val pdfCatalogShowSummary: Boolean = true
)
