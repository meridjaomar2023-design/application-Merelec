package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceOcrHelperTest {

    @Test
    fun testResolveQtyAndPrice_standardInvoiceOrder_QtéBeforePU() {
        // Quantité = 5, PU = 650.0, Total = 3250.0
        val (qty, price) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(5.0, 650.0, 3250.0),
            headerHasQteBeforePu = true
        )
        assertEquals(5.0, qty, 0.001)
        assertEquals(650.0, price, 0.001)
    }

    @Test
    fun testResolveQtyAndPrice_invertedInvoiceOrder_PUBeforeQté() {
        // Sur facture où la colonne Prix est avant Quantité : PU = 1200.0, Qté = 3, Total = 3600.0
        val (qty, price) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(1200.0, 3.0, 3600.0),
            headerHasQteBeforePu = false
        )
        assertEquals(3.0, qty, 0.001)
        assertEquals(1200.0, price, 0.001)
    }

    @Test
    fun testResolveQtyAndPrice_noHeader_largePriceSmallQuantity() {
        // Sans ordre d'en-tête, le grand nombre en Dinars (4500 DA) doit être le prix et le petit (2) la quantité
        val (qty1, price1) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(4500.0, 2.0, 9000.0),
            headerHasQteBeforePu = null
        )
        assertEquals(2.0, qty1, 0.001)
        assertEquals(4500.0, price1, 0.001)

        val (qty2, price2) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(2.0, 4500.0, 9000.0),
            headerHasQteBeforePu = null
        )
        assertEquals(2.0, qty2, 0.001)
        assertEquals(4500.0, price2, 0.001)
    }

    @Test
    fun testResolveQtyAndPrice_twoNumbers_priceAndQuantity() {
        val (qty1, price1) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(850.0, 4.0),
            headerHasQteBeforePu = null
        )
        assertEquals(4.0, qty1, 0.001)
        assertEquals(850.0, price1, 0.001)

        val (qty2, price2) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(4.0, 850.0),
            headerHasQteBeforePu = null
        )
        assertEquals(4.0, qty2, 0.001)
        assertEquals(850.0, price2, 0.001)
    }

    @Test
    fun testTryParseProductRow_leadingQuantity() {
        // 1. Facture avec quantité en première colonne
        val line = "5   Disjoncteur 16A Schneider   650.00   3250.00"
        val parsed = InvoiceOcrHelper.tryParseProductRow(
            row = line,
            catalogProducts = emptyList()
        )
        assertNotNull(parsed)
        assertEquals(5.0, parsed!!.quantite, 0.001)
        assertEquals(650.0, parsed.prixUnitaire, 0.001)
        assertEquals("Disjoncteur 16A Schneider", parsed.designation)
    }

    @Test
    fun testTryParseProductRow_referenceInFirstColumn() {
        // 2. Facture avec référence en première colonne (CAB-125 | Câble électrique 3G2.5 | 100 | 250 | 25000)
        val line = "CAB-125   Câble électrique 3G2.5   100   250.00   25000.00"
        val parsed = InvoiceOcrHelper.tryParseProductRow(
            row = line,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsed)
        assertEquals("CAB-125", parsed!!.reference)
        assertEquals("Câble électrique 3G2.5", parsed.designation)
        assertEquals(100.0, parsed.quantite, 0.001)
        assertEquals(250.0, parsed.prixUnitaire, 0.001)
    }

    @Test
    fun testTryParseProductRow_referenceBeforeQuantityBeforeDesignation() {
        // 3. Facture avec Référence avant Quantité avant Désignation
        val line = "DIS-020   10   Disjoncteur modulaire 20A   450.00   4500.00"
        val parsed = InvoiceOcrHelper.tryParseProductRow(
            row = line,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsed)
        assertEquals("DIS-020", parsed!!.reference)
        assertEquals("Disjoncteur modulaire 20A", parsed.designation)
        assertEquals(10.0, parsed.quantite, 0.001)
        assertEquals(450.0, parsed.prixUnitaire, 0.001)
    }

    @Test
    fun testTryParseProductRow_designationBeforeReference() {
        // 4. Facture avec Désignation avant Référence
        val line = "Contacteur tripolaire 25A   REF-CT25   4   1800.00   7200.00"
        val parsed = InvoiceOcrHelper.tryParseProductRow(
            row = line,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsed)
        assertEquals("REF-CT25", parsed!!.reference)
        assertEquals("Contacteur tripolaire 25A", parsed.designation)
        assertEquals(4.0, parsed.quantite, 0.001)
        assertEquals(1800.0, parsed.prixUnitaire, 0.001)
    }

    @Test
    fun testParseRawInvoiceText_multiLineDesignationsAndFiscalMetadata() {
        // 5. Désignations sur plusieurs lignes + extraction NIF/RC/NIS/AI + abréviations commerciales
        val rawInvoice = """
            SARL ELEC DISTRIBUTION ALGERIE
            Adresse : Zone Industrielle Rouiba, Alger
            Tél : 0550 12 34 56
            RC : 16/00-1234567B19
            NIF : 001916123456789
            NIS : 199916100123456
            AI : 16123456789
            Facture N° FAC-2026-089
            Date : 14/03/2026
            Bon de livraison N° BL-884
            Réf. Commande : BC-77
            Mode de paiement : Chèque
            Réf   Article   Qts   P.U.H.T   Montant HT
            CAB-125   Câble électrique souple 3G2.5   100   250.00   25000.00
            Norme NF C 32-201 Cuivre Rouge Isolé
            DIS-020   Disjoncteur différentiel 20A   10   450.00   4500.00
            Courbe C 30mA Type AC
            TOTAL HT : 29500.00 DA
            TVA 19% : 5605.00 DA
            TOTAL TTC : 35105.00 DA
        """.trimIndent()

        val result = InvoiceOcrHelper.parseRawInvoiceText(
            rawText = rawInvoice,
            reconstructedRows = rawInvoice.lines(),
            knownPartners = emptyList(),
            catalogProducts = emptyList(),
            ownCompanyName = "MERELEC"
        )

        assertEquals("SARL ELEC DISTRIBUTION ALGERIE", result.detectedPartnerName)
        assertEquals("FAC-2026-089", result.detectedInvoiceNumber)
        assertEquals("BL-884", result.detectedBonNumber)
        assertEquals("BC-77", result.detectedOrderRef)
        assertEquals("001916123456789", result.detectedPartnerNif)
        assertEquals("16/00-1234567B19", result.detectedPartnerRc)
        assertEquals(2, result.extractedLines.size)

        // Vérification de la fusion multi-lignes et du non-décalage des colonnes
        val first = result.extractedLines[0]
        assertEquals("CAB-125", first.reference)
        assertTrue(first.designation.contains("Câble électrique souple 3G2.5"))
        assertTrue(first.designation.contains("Norme NF C 32-201 Cuivre Rouge Isolé"))
        assertEquals(100.0, first.quantite, 0.001)
        assertEquals(250.0, first.prixUnitaire, 0.001)
        assertEquals(25000.0, first.totalHt, 0.01)

        val second = result.extractedLines[1]
        assertEquals("DIS-020", second.reference)
        assertTrue(second.designation.contains("Disjoncteur différentiel 20A"))
        assertTrue(second.designation.contains("Courbe C 30mA Type AC"))
        assertEquals(10.0, second.quantite, 0.001)
        assertEquals(450.0, second.prixUnitaire, 0.001)
        assertEquals(4500.0, second.totalHt, 0.01)
    }

    @Test
    fun testMajorationFormula_andMultiRateVatCalculation() {
        // 6 & 7. Formule : Prix de vente HT = Prix d'achat HT × (1 + Pourcentage / 100)
        // Exemple cahier des charges : Prix fournisseur 250 DA HT, Majoration 20% -> Prix client 300 DA HT
        val line1 = OcrExtractedLine(
            reference = "CAB-125",
            designation = "Câble électrique 3G2.5",
            quantite = 100.0,
            prixUnitaire = 250.0,
            majorationPct = 20.0,
            applyMajoration = true,
            remisePct = 0.0,
            tauxTva = 19.0
        )
        assertEquals(300.0, line1.puHtAfterMajoration, 0.001)
        assertEquals(300.0, line1.puHtNet, 0.001)
        assertEquals(30000.0, line1.totalHt, 0.001)
        assertEquals(5700.0, line1.montantTva, 0.001)
        assertEquals(35700.0, line1.totalTtc, 0.001)

        // Deuxième ligne avec taux de TVA réduit (9%) et remise 10%
        val line2 = OcrExtractedLine(
            reference = "SOL-09",
            designation = "Panneau solaire 400W",
            quantite = 2.0,
            prixUnitaire = 10000.0,
            majorationPct = 20.0,
            applyMajoration = true,
            remisePct = 10.0,
            tauxTva = 9.0
        )
        // 10000 * 1.20 = 12000 -> -10% remise = 10800 PU HT Net -> x2 = 21600 Total HT
        assertEquals(12000.0, line2.puHtAfterMajoration, 0.001)
        assertEquals(10800.0, line2.puHtNet, 0.001)
        assertEquals(21600.0, line2.totalHt, 0.001)
        assertEquals(1944.0, line2.montantTva, 0.001)
    }

    @Test
    fun testDetectCalculationMismatchOnLine() {
        // Détection automatique d'erreur de calcul sur une ligne
        val mismatchLine = OcrExtractedLine(
            reference = "ERR-01",
            designation = "Projecteur LED 100W",
            quantite = 5.0,
            prixUnitaire = 1200.0,
            extractedMontantHt = 7500.0 // Devrait être 6000.0
        )
        assertTrue(mismatchLine.hasCalculationMismatch)

        val validLine = OcrExtractedLine(
            reference = "OK-01",
            designation = "Projecteur LED 100W",
            quantite = 5.0,
            prixUnitaire = 1200.0,
            extractedMontantHt = 6000.0
        )
        assertFalse(validLine.hasCalculationMismatch)
    }

    @Test
    fun testDetectHeaderColumnOrder() {
        val header1 = "Réf   Désignation   Qté   P.U. HT   Montant HT"
        assertEquals(true, InvoiceOcrHelper.detectHeaderColumnOrder(listOf(header1)))

        val header2 = "Code   Article   P.U. (DA)   Quantité   Total"
        assertEquals(false, InvoiceOcrHelper.detectHeaderColumnOrder(listOf(header2)))
    }

    @Test
    fun testExtractProductLines_twoLineHeaderAndTiltedSplitRows() {
        // Cas réel : en-tête sur 2 lignes (avec "Montant HT" seul sur la 2e ligne)
        // + photo légèrement inclinée où la désignation et les montants sont sur 2 lignes consécutives
        val rows = listOf(
            "EURL MATERIEL ELECTRIQUE",
            "Facture N° 2026/105",
            "Réf   Désignation   Qté   P.U HT",
            "Montant HT",
            "CAB-125   Câble électrique 3G2.5",
            "100   250.00   25000.00",
            "DIS-020   Disjoncteur 20A   10   450.00   4500.00",
            "Total HT   29500.00"
        )

        val extracted = InvoiceOcrHelper.extractProductLinesFromRows(rows)
        assertEquals(2, extracted.size)
        assertEquals("CAB-125", extracted[0].reference)
        assertEquals("Câble électrique 3G2.5", extracted[0].designation)
        assertEquals(100.0, extracted[0].quantite, 0.001)
        assertEquals(250.0, extracted[0].prixUnitaire, 0.001)

        assertEquals("DIS-020", extracted[1].reference)
        assertEquals("Disjoncteur 20A", extracted[1].designation)
        assertEquals(10.0, extracted[1].quantite, 0.001)
        assertEquals(450.0, extracted[1].prixUnitaire, 0.001)
    }

    @Test
    fun testExtractProductLines_singleNumberDeliveryNoteFallback() {
        // Cas d'un Bon de Livraison ou tarif comportant un seul nombre par ligne
        val rows = listOf(
            "BON DE LIVRAISON N° BL-402",
            "Désignation   Prix Unitaire",
            "Armoire électrique étanche IP65   4800.00",
            "Goulotte PVC 40x60   350.00"
        )
        val extracted = InvoiceOcrHelper.extractProductLinesFromRows(rows)
        assertEquals(2, extracted.size)
        assertEquals("Armoire électrique étanche IP65", extracted[0].designation)
        assertEquals(4800.0, extracted[0].prixUnitaire, 0.001)
    }

    @Test
    fun testHublot24WRound_withGluedTvaAndFourColumns() {
        // Cas réel signalé par l'utilisateur :
        // Article : "Hublot 24 W Round", Quantité = 309, PU HT = 361,13, TVA = 19%, Montant HT = 111 589,17
        // 1. Cas où l'OCR colle la virgule de 361,13 avec la colonne TVA 19 -> "361,1319"
        val rowGluedTva = "Hublot 24 W Round   309   361,1319   111 589,17"
        val parsedGlued = InvoiceOcrHelper.tryParseProductRow(
            row = rowGluedTva,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsedGlued)
        assertEquals("Hublot 24 W Round", parsedGlued!!.designation)
        assertEquals(309.0, parsedGlued.quantite, 0.001)
        assertEquals(361.13, parsedGlued.prixUnitaire, 0.01)

        // 2. Cas où l'OCR sépare la partie entière 361 et colle les centimes 13 avec la TVA 19 -> "361   1319"
        val rowSplitDecimalGluedTva = "Hublot 24 W Round   309   361   1319   111 589,17"
        val parsedSplit = InvoiceOcrHelper.tryParseProductRow(
            row = rowSplitDecimalGluedTva,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsedSplit)
        assertEquals("Hublot 24 W Round", parsedSplit!!.designation)
        assertEquals(309.0, parsedSplit.quantite, 0.001)
        assertEquals(361.13, parsedSplit.prixUnitaire, 0.01)

        // 3. Cas avec 4 colonnes numériques distinctes (Qté | PU HT | TVA | Montant HT) et un seul espace
        val rowSingleSpace = "Hublot 24 W Round 309 361,13 19 111 589,17"
        val parsedSingleSpace = InvoiceOcrHelper.tryParseProductRow(
            row = rowSingleSpace,
            catalogProducts = emptyList(),
            headerHasQteBeforePu = true
        )
        assertNotNull(parsedSingleSpace)
        assertEquals("Hublot 24 W Round", parsedSingleSpace!!.designation)
        assertEquals(309.0, parsedSingleSpace.quantite, 0.001)
        assertEquals(361.13, parsedSingleSpace.prixUnitaire, 0.01)

        // 4. Cas où l'OCR a omis le séparateur de colonne entre 309 et 361,13 ou n'a lu que [361, 1319, 111589.17]
        val (recoveredQty, recoveredPrice) = InvoiceOcrHelper.resolveQtyAndPrice(
            candidateNumbers = listOf(361.0, 1319.0, 111589.17),
            headerHasQteBeforePu = true
        )
        assertEquals(309.0, recoveredQty, 0.001)
        assertEquals(361.13, recoveredPrice, 0.01)
    }

    @Test
    fun testCustomColumnOrder_QuantiteRefDesignationPuHt() {
        // Test de l'option demandée : Quantité / Réf / Désignation / P.U.HT
        val config = OcrColumnConfig.fromPreset(OcrColumnPreset.QTE_REF_DES_PU)
        assertFalse(config.isAuto)
        assertTrue(config.includeReference)
        assertEquals(true, config.qteBeforeRef)
        assertEquals(true, config.qteBeforeDesignation)
        assertEquals(true, config.refBeforeDesignation)
        assertEquals(true, config.qteBeforePu)

        // 1. Avec Quantité en 1re colonne, Réf en 2e, Désignation en 3e, P.U.HT en 4e (et Total HT en 5e)
        val rowWithTotal = "15   CAB-3G25   Câble électrique souple 3G2.5   250,00   3750,00"
        val parsed1 = InvoiceOcrHelper.tryParseProductRow(
            row = rowWithTotal,
            catalogProducts = emptyList(),
            columnConfig = config
        )
        assertNotNull(parsed1)
        assertEquals(15.0, parsed1!!.quantite, 0.001)
        assertEquals("CAB-3G25", parsed1.reference)
        assertEquals("Câble électrique souple 3G2.5", parsed1.designation)
        assertEquals(250.0, parsed1.prixUnitaire, 0.001)

        // 2. Même ordre sans colonne Total HT (uniquement Quantité / Réf / Désignation / P.U.HT)
        val rowWithoutTotal = "309   HUB-24R   Hublot 24 W Round   361,13"
        val parsed2 = InvoiceOcrHelper.tryParseProductRow(
            row = rowWithoutTotal,
            catalogProducts = emptyList(),
            columnConfig = config
        )
        assertNotNull(parsed2)
        assertEquals(309.0, parsed2!!.quantite, 0.001)
        assertEquals("HUB-24R", parsed2.reference)
        assertEquals("Hublot 24 W Round", parsed2.designation)
        assertEquals(361.13, parsed2.prixUnitaire, 0.01)
    }

    @Test
    fun testCustomColumnOrder_WithoutRef_andReparseLive() {
        // Test du choix des colonnes (désactivation de la colonne Réf + changement d'ordre en direct)
        val rows = listOf(
            "FOURNISSEUR TEST",
            "10   CAB-99   Projecteur LED 50W   1200.00   12000.00"
        )
        val initialResult = InvoiceOcrHelper.parseRawInvoiceText(
            rawText = rows.joinToString("\n"),
            reconstructedRows = rows,
            columnConfig = OcrColumnConfig.fromPreset(OcrColumnPreset.QTE_REF_DES_PU)
        )
        assertEquals(1, initialResult.extractedLines.size)
        assertEquals(10.0, initialResult.extractedLines[0].quantite, 0.001)
        assertEquals("CAB-99", initialResult.extractedLines[0].reference)
        assertEquals("Projecteur LED 50W", initialResult.extractedLines[0].designation)
        assertEquals(1200.0, initialResult.extractedLines[0].prixUnitaire, 0.001)

        // Si l'utilisateur désactive la colonne Réf (Quantité / Désignation / P.U.HT)
        val noRefConfig = OcrColumnConfig.fromPreset(OcrColumnPreset.QTE_DES_PU)
        val reparsed = InvoiceOcrHelper.reparseResultWithColumnConfig(
            existingResult = initialResult,
            columnConfig = noRefConfig
        )
        assertEquals(1, reparsed.extractedLines.size)
        assertEquals("", reparsed.extractedLines[0].reference)
        assertTrue(reparsed.extractedLines[0].designation.contains("CAB-99"))
        assertEquals(10.0, reparsed.extractedLines[0].quantite, 0.001)
        assertEquals(1200.0, reparsed.extractedLines[0].prixUnitaire, 0.001)
    }

    @Test
    fun testToggleFieldInOrder_QtsFirstDesignationSecondPuThird() {
        // Test de la sélection séquentielle directe : 1er = Qts, 2e = Désignation, 3e = P.U.HT
        var config = OcrColumnConfig()
        assertTrue(config.isAuto)

        // Clic 1 : Qts en 1er
        config = config.toggleFieldInOrder(OcrColumnField.QUANTITE)
        assertFalse(config.isAuto)
        assertEquals(1, config.selectedPositionOf(OcrColumnField.QUANTITE))

        // Clic 2 : Désignation en 2e
        config = config.toggleFieldInOrder(OcrColumnField.DESIGNATION)
        assertEquals(1, config.selectedPositionOf(OcrColumnField.QUANTITE))
        assertEquals(2, config.selectedPositionOf(OcrColumnField.DESIGNATION))

        // Clic 3 : P.U.HT en 3e
        config = config.toggleFieldInOrder(OcrColumnField.PU_HT)
        assertEquals(1, config.selectedPositionOf(OcrColumnField.QUANTITE))
        assertEquals(2, config.selectedPositionOf(OcrColumnField.DESIGNATION))
        assertEquals(3, config.selectedPositionOf(OcrColumnField.PU_HT))
        assertEquals(null, config.selectedPositionOf(OcrColumnField.REFERENCE))
        assertFalse(config.includeReference)
        assertEquals(
            listOf(OcrColumnField.QUANTITE, OcrColumnField.DESIGNATION, OcrColumnField.PU_HT),
            config.displayFields
        )

        // Extraction d'une ligne avec cet ordre (1er: Qts, 2e: Désignation, 3e: P.U.HT)
        val parsed = InvoiceOcrHelper.tryParseProductRow(
            row = "309   Hublot 24 W Round   361,13   111 589,17",
            catalogProducts = emptyList(),
            columnConfig = config
        )
        assertNotNull(parsed)
        assertEquals(309.0, parsed!!.quantite, 0.001)
        assertEquals("Hublot 24 W Round", parsed.designation)
        assertEquals(361.13, parsed.prixUnitaire, 0.01)

        // Test d'un ordre où Qts est en 1er, P.U.HT en 2e, Désignation en 3e
        val qtsPuDesConfig = OcrColumnConfig()
            .toggleFieldInOrder(OcrColumnField.QUANTITE)
            .toggleFieldInOrder(OcrColumnField.PU_HT)
            .toggleFieldInOrder(OcrColumnField.DESIGNATION)
        val parsedQtsPuDes = InvoiceOcrHelper.tryParseProductRow(
            row = "10   450,00   Disjoncteur 20A",
            catalogProducts = emptyList(),
            columnConfig = qtsPuDesConfig
        )
        assertNotNull(parsedQtsPuDes)
        assertEquals(10.0, parsedQtsPuDes!!.quantite, 0.001)
        assertEquals(450.0, parsedQtsPuDes.prixUnitaire, 0.01)
        assertEquals("Disjoncteur 20A", parsedQtsPuDes.designation)
    }
}

