package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.LigneOperationEntity
import com.example.data.model.OperationEntity
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.PartnerType
import com.example.data.model.ProductCatalogEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OperationRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun insertAndObserveProduct_authenticatedOwner_emitsRealtimeStockUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = OperationRepository(firestore)
        val productId = "prod-${UUID.randomUUID()}"
        val product = ProductCatalogEntity(
            id = productId,
            name = "Disjoncteur Différentiel 40A",
            reference = "DIFF-40A",
            defaultUnitPrice = 3800.0,
            stockActuel = 25.0,
            seuilAlerte = 5.0
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.insertCatalogProduct(product)
        }

        val emittedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.allCatalogProducts.first { list -> list.any { it.id == productId } }
        }
        assertTrue(emittedProducts.any { it.id == productId && it.stockActuel == 25.0 })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStock(productId, 18.0)
        }

        val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.allCatalogProducts.first { list ->
                list.any { it.id == productId && it.stockActuel == 18.0 }
            }
        }
        assertTrue(updatedProducts.any { it.id == productId && it.stockActuel == 18.0 })
    }

    @Test
    fun insertPartnerAndSaveSaleOperation_updatesClientBalanceAndEmitsRealtime() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = OperationRepository(firestore)
        val partnerId = "cli-${UUID.randomUUID()}"
        val clientName = "Client Test ${UUID.randomUUID().toString().take(6)}"
        val partner = PartnerEntity(
            id = partnerId,
            name = clientName,
            type = PartnerType.CLIENT,
            phone = "0555112233",
            address = "Alger",
            solde = 0.0
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.insertPartner(partner)
        }

        val opId = "FAC-${UUID.randomUUID().toString().take(8)}"
        val operation = OperationEntity(
            id = opId,
            dateTime = System.currentTimeMillis(),
            typeOperation = OperationType.FACTURE,
            clientFournisseur = clientName,
            montantVerse = 2000.0,
            modeReglement = "Espèces",
            appliquerTva = false,
            statut = "Validé",
            vendeur = "Alice Vendeur"
        )
        val lignes = listOf(
            LigneOperationEntity(
                id = "lig-${UUID.randomUUID()}",
                operationId = opId,
                produit = "Câble 2.5mm",
                quantite = 2.0,
                prixUnitaire = 2500.0
            )
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOperationWithLignes(operation, lignes)
        }

        val fetchedOp = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOperationWithLignesByIdOnce(opId)
        }
        assertNotNull(fetchedOp)
        assertEquals(5000.0, fetchedOp!!.totalTtc, 0.01)

        val updatedClients = withTimeout(FLOW_TIMEOUT_MS) {
            repository.clients.first { list ->
                list.any { it.id == partnerId && it.solde == 3000.0 }
            }
        }
        assertTrue(updatedClients.any { it.id == partnerId && it.solde == 3000.0 })
    }

    @Test
    fun getOperationById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = OperationRepository(firestore)
        val opId = "BLC-${UUID.randomUUID().toString().take(8)}"
        val op = OperationEntity(
            id = opId,
            typeOperation = OperationType.BL_CLIENT,
            clientFournisseur = "Client Alice",
            montantVerse = 1000.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveOperationWithLignes(op, emptyList())
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = OperationRepository(firestore)
        var thrown: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getOperationWithLignesByIdOnce(opId)
            }
        } catch (e: FirebaseFirestoreException) {
            thrown = e
        }
        assertNotNull("Expected FirebaseFirestoreException for cross-user access", thrown)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown?.code)
    }

    @Test
    fun observeProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = OperationRepository(firestore)

        var thrown: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.allCatalogProducts.first()
            }
        } catch (e: Throwable) {
            var current: Throwable? = e
            while (current != null) {
                if (current is FirebaseFirestoreException) {
                    thrown = current
                    break
                }
                current = current.cause
            }
        }
        assertNotNull("Expected FirebaseFirestoreException when unauthenticated", thrown)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@merelec.dz"
        const val BOB_EMAIL = "bob@merelec.dz"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
