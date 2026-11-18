package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saisie_terrain")
data class OperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_unique")
    val id: String = "", // ID_Unique (Key)

    @ColumnInfo(name = "date_heure")
    val dateTime: Long = System.currentTimeMillis(), // Date_Heure (DateTime)

    @ColumnInfo(name = "type_operation")
    val typeOperation: OperationType = OperationType.BL_CLIENT, // Type_Operation

    @ColumnInfo(name = "client_fournisseur")
    val clientFournisseur: String = "", // Client_Fournisseur (Text)

    @ColumnInfo(name = "montant_verse")
    val montantVerse: Double = 0.0, // Montant_Verse (Price / Text, formaté en DA)

    @ColumnInfo(name = "mode_reglement")
    val modeReglement: String = "Espèces", // Mode de paiement (Espèces, Chèque, Virement, CCP)

    @ColumnInfo(name = "notes")
    val notes: String = "", // Notes / Remarques de terrain

    @ColumnInfo(name = "appliquer_tva")
    val appliquerTva: Boolean = false, // Activation TVA

    @ColumnInfo(name = "taux_tva")
    val tauxTva: Double = 19.0, // Taux de TVA (19% standard en Algérie)

    @ColumnInfo(name = "statut")
    val statut: String = "Validé", // Statut du document (Validé, Brouillon, Payé)

    @ColumnInfo(name = "vendeur")
    val vendeur: String = "" // Vendeur / Opérateur ayant saisi l'opération
)
