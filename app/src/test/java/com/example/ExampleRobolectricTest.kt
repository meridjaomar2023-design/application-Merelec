package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.OperationWithLignes
import com.example.data.model.PartnerType
import com.example.ui.viewmodel.FormDraftState
import com.example.util.CurrencyUtils
import com.example.util.ExportUtils
import com.example.util.ImportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gestion Merelec", appName)
    }

    @Test
    fun `test total produits app formula calculation`() {
        val op = OperationEntity(
            id = "BLC-001",
            typeOperation = OperationType.BL_CLIENT,
            clientFournisseur = "SARL Merelec Client",
            montantVerse = 5000.0,
            modeReglement = "Crédit"
        )
        val lignes = listOf(
            LigneOperationEntity(
                id = "L1",
                operationId = "BLC-001",
                produit = "Disjoncteur 32A",
                quantite = 10.0,
                prixUnitaire = 450.0
            ),
            LigneOperationEntity(
                id = "L2",
                operationId = "BLC-001",
                produit = "Câble Armé 4x16mm",
                quantite = 2.0,
                prixUnitaire = 1200.0
            )
        )
        val opWithLignes = OperationWithLignes(operation = op, lignes = lignes)

        // 10 * 450 + 2 * 1200 = 4500 + 2400 = 6900
        assertEquals(6900.0, opWithLignes.totalProduits, 0.001)
        // Reste = 6900 - 5000 = 1900
        assertEquals(1900.0, opWithLignes.resteAPayer, 0.001)
        assertEquals("Crédit", op.modeReglement)
    }

    @Test
    fun `test AppSheet Show_If rule for Reglement`() {
        val draftBl = FormDraftState(typeOperation = OperationType.BL_CLIENT)
        assertTrue(draftBl.showProductLines)

        val draftReglement = FormDraftState(typeOperation = OperationType.REGLEMENT)
        assertFalse(draftReglement.showProductLines)
    }

    @Test
    fun `test algerian dinar currency formatting`() {
        assertEquals("15 000,00 DA", CurrencyUtils.formatDa(15000.0))
        assertEquals("15 000 DA", CurrencyUtils.formatDaSmart(15000.0))
        assertEquals("2 500,50 DA", CurrencyUtils.formatDaSmart(2500.50))
    }

    @Test
    fun `test excel export xml content`() {
        val op = OperationEntity(
            id = "BLC-001",
            typeOperation = OperationType.BL_CLIENT,
            clientFournisseur = "Merelec Alger",
            montantVerse = 1000.0,
            modeReglement = "Crédit"
        )
        val lignes = listOf(
            LigneOperationEntity(
                id = "L1",
                operationId = "BLC-001",
                produit = "Transformateur 50kVA",
                quantite = 1.0,
                prixUnitaire = 250000.0
            )
        )
        val list = listOf(OperationWithLignes(op, lignes))
        val xml = ExportUtils.generateExcelXml(list)

        assertTrue(xml.contains("Worksheet ss:Name=\"Opérations Terrain\""))
        assertTrue(xml.contains("Worksheet ss:Name=\"Détails Lignes Produits\""))
        assertTrue(xml.contains("Worksheet ss:Name=\"Règlements et Paiements\""))
        assertTrue(xml.contains("Transformateur 50kVA"))
        assertTrue(xml.contains("Crédit"))
    }

    @Test
    fun `test import partners from csv text`() {
        val csv = """
            Nom;Telephone;Adresse;Notes
            SARL Electro Blida;0550112233;Blida;Client VIP
            Ets Constantinois;031889900;Constantine;Grossiste
        """.trimIndent()

        val partners = ImportUtils.parsePartners(csv, PartnerType.CLIENT)
        assertEquals(2, partners.size)
        assertEquals("SARL Electro Blida", partners[0].name)
        assertEquals("0550112233", partners[0].phone)
        assertEquals("Ets Constantinois", partners[1].name)
    }

    @Test
    fun `test import products from csv text`() {
        val csv = """
            Designation;Prix_Unitaire_DA;Categorie;Unite
            Câble 4x10mm;7500;Câblage;Rouleau
            Disjoncteur 63A;1450;Protection;U
        """.trimIndent()

        val products = ImportUtils.parseProducts(csv)
        assertEquals(2, products.size)
        assertEquals("Câble 4x10mm", products[0].name)
        assertEquals(7500.0, products[0].defaultUnitPrice ?: 0.0, 0.001)
        assertEquals("Disjoncteur 63A", products[1].name)
        assertEquals(1450.0, products[1].defaultUnitPrice ?: 0.0, 0.001)
    }

    @Test
    fun `test google drive backup json serialization and deserialization`() {
        val op = OperationEntity(
            id = "BLC-999",
            typeOperation = OperationType.BL_CLIENT,
            clientFournisseur = "SARL Merelec Alger",
            montantVerse = 50000.0,
            modeReglement = "Crédit",
            notes = "Livraison urgente chantier"
        )
        val lignes = listOf(
            LigneOperationEntity(
                id = "L1",
                operationId = "BLC-999",
                produit = "Armoire Électrique TGBT",
                quantite = 1.0,
                prixUnitaire = 120000.0
            )
        )
        val ops = listOf(OperationWithLignes(op, lignes))

        val clients = listOf(
            com.example.data.model.PartnerEntity(
                id = "C1",
                name = "Client Test Merelec",
                type = PartnerType.CLIENT,
                phone = "0555123456"
            )
        )
        val suppliers = listOf(
            com.example.data.model.PartnerEntity(
                id = "S1",
                name = "Fournisseur Câblerie",
                type = PartnerType.FOURNISSEUR,
                phone = "021987654"
            )
        )
        val prods = listOf(
            com.example.data.model.ProductCatalogEntity(
                id = "P1",
                name = "Disjoncteur 40A",
                defaultUnitPrice = 1800.0,
                category = "Protection",
                unit = "U"
            )
        )

        val json = com.example.util.BackupUtils.createBackupJson(ops, clients, suppliers, prods)
        assertTrue(json.contains("Gestion Merelec"))
        assertTrue(json.contains("Armoire Électrique TGBT"))
        assertTrue(json.contains("Client Test Merelec"))

        val parsed = com.example.util.BackupUtils.parseBackupJson(json)
        org.junit.Assert.assertNotNull(parsed)
        assertEquals(1, parsed!!.operationsCount)
        assertEquals(1, parsed.clientsCount)
        assertEquals(1, parsed.suppliersCount)
        assertEquals(1, parsed.productsCount)
        assertEquals("BLC-999", parsed.operations[0].first.id)
        assertEquals("Armoire Électrique TGBT", parsed.operations[0].second[0].produit)
    }
}
