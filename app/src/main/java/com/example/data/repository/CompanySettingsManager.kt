package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CompanySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompanySettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("company_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<CompanySettings> = _settings

    fun getSettings(): CompanySettings = _settings.value

    fun updateSettings(newSettings: CompanySettings) {
        prefs.edit().apply {
            putString("company_name", newSettings.companyName)
            putString("activity_slogan", newSettings.activitySlogan)
            putString("address", newSettings.address)
            putString("phone", newSettings.phone)
            putString("phone_secondary", newSettings.phoneSecondary)
            putString("email", newSettings.email)
            putString("website", newSettings.website)
            putString("nif", newSettings.nif)
            putString("nis", newSettings.nis)
            putString("rc", newSettings.rc)
            putString("article_imposition", newSettings.articleImposition)
            putString("nin", newSettings.nin)
            putString("bank_name", newSettings.bankName)
            putString("bank_rib", newSettings.bankRib)
            putBoolean("tva_enabled_default", newSettings.tvaEnabledByDefault)
            putFloat("default_tva_rate", newSettings.defaultTvaRate.toFloat())
            putFloat("reduced_tva_rate", newSettings.reducedTvaRate.toFloat())
            putString("currency", newSettings.currency)
            putFloat("stock_alert_threshold", newSettings.defaultStockAlertThreshold.toFloat())
            putFloat("default_margin_coefficient", newSettings.defaultMarginCoefficient.toFloat())
            putBoolean("use_catalog_sale_price_on_convert", newSettings.useCatalogSalePriceOnConvert)
            putString("prefix_facture", newSettings.prefixFacture)
            putString("prefix_facture_fournisseur", newSettings.prefixFactureFournisseur)
            putString("prefix_bc", newSettings.prefixBonCommande)
            putString("prefix_bl", newSettings.prefixBonLivraison)
            putString("prefix_bf", newSettings.prefixBonFournisseur)
            putString("prefix_devis", newSettings.prefixDevis)
            putString("prefix_avoir", newSettings.prefixAvoir)
            putString("prefix_reg", newSettings.prefixReglement)
            // PDF Options
            putBoolean("pdf_show_logo", newSettings.pdfShowLogo)
            putString("pdf_logo_position", newSettings.pdfLogoPosition)
            putInt("pdf_logo_size", newSettings.pdfLogoSize)
            putString("pdf_header_blocks_order", newSettings.pdfHeaderBlocksOrder)
            putBoolean("pdf_show_slogan", newSettings.pdfShowSlogan)
            putBoolean("pdf_show_address", newSettings.pdfShowAddress)
            putBoolean("pdf_show_phone", newSettings.pdfShowPhone)
            putBoolean("pdf_show_email_website", newSettings.pdfShowEmailWebsite)
            putBoolean("pdf_show_fiscal_ids", newSettings.pdfShowFiscalIds)
            putBoolean("pdf_show_bank_rib", newSettings.pdfShowBankRib)
            putString("pdf_doc_columns_order", newSettings.pdfDocColumnsOrder)
            putBoolean("pdf_show_partner_fiscal", newSettings.pdfShowPartnerFiscal)
            putBoolean("pdf_show_payment_method", newSettings.pdfShowPaymentMethod)
            putBoolean("pdf_show_kit_components", newSettings.pdfShowKitComponents)
            putBoolean("pdf_show_notes", newSettings.pdfShowNotes)
            putBoolean("pdf_show_conversion_note", newSettings.pdfShowConversionNote)
            putBoolean("pdf_show_amount_in_words", newSettings.pdfShowAmountInWords)
            putBoolean("pdf_show_signatures", newSettings.pdfShowSignatures)
            putBoolean("pdf_show_stamp_signature", newSettings.pdfShowStampAndSignature)
            putInt("pdf_stamp_scale", newSettings.pdfStampScale)
            putString("pdf_custom_footer", newSettings.pdfCustomFooter)
            putString("pdf_creance_columns_order", newSettings.pdfCreanceColumnsOrder)
            putBoolean("pdf_creance_show_summary", newSettings.pdfCreanceShowSummary)
            putString("pdf_article_columns_order", newSettings.pdfArticleColumnsOrder)
            putBoolean("pdf_catalog_show_summary", newSettings.pdfCatalogShowSummary)
            apply()
        }
        _settings.value = newSettings
    }

    private fun loadSettings(): CompanySettings {
        return CompanySettings(
            companyName = prefs.getString("company_name", "GESTION MERÉLEC") ?: "GESTION MERÉLEC",
            activitySlogan = prefs.getString("activity_slogan", "Équipements & Réalisations Électriques") ?: "Équipements & Réalisations Électriques",
            address = prefs.getString("address", "Alger, Algérie") ?: "Alger, Algérie",
            phone = prefs.getString("phone", "0550 12 34 56") ?: "0550 12 34 56",
            phoneSecondary = prefs.getString("phone_secondary", "") ?: "",
            email = prefs.getString("email", "contact@merelec-dz.com") ?: "contact@merelec-dz.com",
            website = prefs.getString("website", "") ?: "",
            nif = prefs.getString("nif", "000116012345678") ?: "000116012345678",
            nis = prefs.getString("nis", "099012345678901") ?: "099012345678901",
            rc = prefs.getString("rc", "16/00-1234567B16") ?: "16/00-1234567B16",
            articleImposition = prefs.getString("article_imposition", "16012345678") ?: "16012345678",
            nin = prefs.getString("nin", "109801600012345001") ?: "109801600012345001",
            bankName = prefs.getString("bank_name", "BNA / CPA") ?: "BNA / CPA",
            bankRib = prefs.getString("bank_rib", "001 00234 1234567890 12") ?: "001 00234 1234567890 12",
            tvaEnabledByDefault = prefs.getBoolean("tva_enabled_default", true),
            defaultTvaRate = prefs.getFloat("default_tva_rate", 19.0f).toDouble(),
            reducedTvaRate = prefs.getFloat("reduced_tva_rate", 9.0f).toDouble(),
            currency = prefs.getString("currency", "DA") ?: "DA",
            defaultStockAlertThreshold = prefs.getFloat("stock_alert_threshold", 5.0f).toDouble(),
            defaultMarginCoefficient = prefs.getFloat("default_margin_coefficient", 1.25f).toDouble().coerceAtLeast(0.01),
            useCatalogSalePriceOnConvert = prefs.getBoolean("use_catalog_sale_price_on_convert", false),
            prefixFacture = prefs.getString("prefix_facture", "FAC-") ?: "FAC-",
            prefixFactureFournisseur = prefs.getString("prefix_facture_fournisseur", "FACF-") ?: "FACF-",
            prefixBonCommande = prefs.getString("prefix_bc", "BC-") ?: "BC-",
            prefixBonLivraison = prefs.getString("prefix_bl", "BL-") ?: "BL-",
            prefixBonFournisseur = prefs.getString("prefix_bf", "BF-") ?: "BF-",
            prefixDevis = prefs.getString("prefix_devis", "DEV-") ?: "DEV-",
            prefixAvoir = prefs.getString("prefix_avoir", "AVR-") ?: "AVR-",
            prefixReglement = prefs.getString("prefix_reg", "REG-") ?: "REG-",
            pdfShowLogo = prefs.getBoolean("pdf_show_logo", true),
            pdfLogoPosition = prefs.getString("pdf_logo_position", "LEFT") ?: "LEFT",
            pdfLogoSize = prefs.getInt("pdf_logo_size", 52),
            pdfHeaderBlocksOrder = prefs.getString("pdf_header_blocks_order", "SLOGAN,CONTACT,FISCAL,BANQUE") ?: "SLOGAN,CONTACT,FISCAL,BANQUE",
            pdfShowSlogan = prefs.getBoolean("pdf_show_slogan", true),
            pdfShowAddress = prefs.getBoolean("pdf_show_address", true),
            pdfShowPhone = prefs.getBoolean("pdf_show_phone", true),
            pdfShowEmailWebsite = prefs.getBoolean("pdf_show_email_website", true),
            pdfShowFiscalIds = prefs.getBoolean("pdf_show_fiscal_ids", true),
            pdfShowBankRib = prefs.getBoolean("pdf_show_bank_rib", true),
            pdfDocColumnsOrder = prefs.getString("pdf_doc_columns_order", "NUMERO,DESIGNATION,QUANTITE,UNITE,PU_HT,TOTAL_HT") ?: "NUMERO,DESIGNATION,QUANTITE,UNITE,PU_HT,TOTAL_HT",
            pdfShowPartnerFiscal = prefs.getBoolean("pdf_show_partner_fiscal", true),
            pdfShowPaymentMethod = prefs.getBoolean("pdf_show_payment_method", true),
            pdfShowKitComponents = prefs.getBoolean("pdf_show_kit_components", false),
            pdfShowNotes = prefs.getBoolean("pdf_show_notes", true),
            pdfShowConversionNote = prefs.getBoolean("pdf_show_conversion_note", true),
            pdfShowAmountInWords = prefs.getBoolean("pdf_show_amount_in_words", true),
            pdfShowSignatures = prefs.getBoolean("pdf_show_signatures", true),
            pdfShowStampAndSignature = prefs.getBoolean("pdf_show_stamp_signature", true),
            pdfStampScale = prefs.getInt("pdf_stamp_scale", 130),
            pdfCustomFooter = prefs.getString("pdf_custom_footer", "") ?: "",
            pdfCreanceColumnsOrder = prefs.getString("pdf_creance_columns_order", "NUMERO,CLIENT,TELEPHONE_VILLE,TOTAL_FACTURE,TOTAL_ENCAISSE,CREANCE_DUE,TAUX_RECOUVREMENT") ?: "NUMERO,CLIENT,TELEPHONE_VILLE,TOTAL_FACTURE,TOTAL_ENCAISSE,CREANCE_DUE,TAUX_RECOUVREMENT",
            pdfCreanceShowSummary = prefs.getBoolean("pdf_creance_show_summary", true),
            pdfArticleColumnsOrder = prefs.getString("pdf_article_columns_order", "NUMERO,REFERENCE,DESIGNATION,FAMILLE,MARQUE,PRIX_VENTE,PRIX_ACHAT,STOCK") ?: "NUMERO,REFERENCE,DESIGNATION,FAMILLE,MARQUE,PRIX_VENTE,PRIX_ACHAT,STOCK",
            pdfCatalogShowSummary = prefs.getBoolean("pdf_catalog_show_summary", true)
        )
    }
}
