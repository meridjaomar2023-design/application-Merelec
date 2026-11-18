package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "partenaires")
data class PartnerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_partenaire")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "nom")
    val name: String = "",

    @ColumnInfo(name = "type")
    val type: PartnerType = PartnerType.CLIENT, // CLIENT ou FOURNISSEUR

    @ColumnInfo(name = "telephone")
    val phone: String = "",

    @ColumnInfo(name = "adresse")
    val address: String = "",

    @ColumnInfo(name = "solde")
    val solde: Double = 0.0,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    // Coordonnées fiscales & bancaires (Algérie)
    @ColumnInfo(name = "rc")
    val rc: String = "", // RC N°

    @ColumnInfo(name = "article_imposition")
    val articleImposition: String = "", // N° article

    @ColumnInfo(name = "nif")
    val nif: String = "", // NIF N°

    @ColumnInfo(name = "nis")
    val nis: String = "", // NIS N°

    @ColumnInfo(name = "nin")
    val nin: String = "", // NIN N°

    @ColumnInfo(name = "rib")
    val rib: String = "" // RIB
)
