package com.example.data.model

enum class EmployeeRole(val label: String, val description: String) {
    ADMIN(
        label = "Administrateur",
        description = "Accès complet : gestion des employés, modification et suppression de tous les Bons, Factures et Créances."
    ),
    GERANT(
        label = "Gérant / Superviseur",
        description = "Peut modifier et supprimer les Bons, Factures et Créances."
    ),
    VENDEUR(
        label = "Vendeur Comptoir",
        description = "Peut créer et modifier les Bons de livraison, mais ne peut ni supprimer ni modifier les Factures ou Créances par défaut."
    ),
    CAISSIER(
        label = "Caissier / Recouvrement",
        description = "Peut gérer les règlements et modifier les Créances et Factures, sans droit de suppression par défaut."
    );

    companion object {
        fun fromString(value: String?): EmployeeRole {
            if (value.isNullOrBlank()) return VENDEUR
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) } ?: VENDEUR
        }
    }
}

enum class DocumentPermissionCategory(val label: String) {
    BON("Bon (BL / BC / Devis)"),
    FACTURE("Facture / Avoir"),
    CREANCE("Créance / Règlement")
}

fun OperationType.permissionCategory(): DocumentPermissionCategory {
    return when (this) {
        OperationType.BL_CLIENT,
        OperationType.BL_FOURNISSEUR,
        OperationType.BON_COMMANDE,
        OperationType.DEVIS_CLIENT,
        OperationType.DEVIS_FOURNISSEUR -> DocumentPermissionCategory.BON

        OperationType.FACTURE,
        OperationType.FACTURE_FOURNISSEUR,
        OperationType.AVOIR_CLIENT,
        OperationType.AVOIR_FOURNISSEUR -> DocumentPermissionCategory.FACTURE

        OperationType.REGLEMENT -> DocumentPermissionCategory.CREANCE
    }
}

data class EmployeeEntity(
    val id: String = "",
    val name: String,
    val email: String,
    val password: String,
    val role: EmployeeRole = EmployeeRole.VENDEUR,
    val isActive: Boolean = true,
    // Droits sur les Bons (BL Client, BL Fournisseur, Bon de Commande, Devis)
    val canModifyBon: Boolean = true,
    val canDeleteBon: Boolean = true,
    // Droits sur les Factures (Facture, Avoir Client, Avoir Fournisseur)
    val canModifyFacture: Boolean = false,
    val canDeleteFacture: Boolean = false,
    // Droits sur les Créances (Règlements, Ajustement de solde, Remise à zéro créance)
    val canModifyCreance: Boolean = false,
    val canDeleteCreance: Boolean = false,
    // Droit d'administration des employés
    val canManageEmployees: Boolean = false
) {
    val hasAnyWritePermission: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || canModifyBon || canModifyFacture || canModifyCreance

    val hasAllWritePermissions: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || (canModifyBon && canModifyFacture && canModifyCreance)

    val hasAnyDeletePermission: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || canDeleteBon || canDeleteFacture || canDeleteCreance

    val hasAllDeletePermissions: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || (canDeleteBon && canDeleteFacture && canDeleteCreance)

    val isAdmin: Boolean
        get() = role == EmployeeRole.ADMIN ||
            normalizeEmail(email).equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)

    val canWriteCatalogAndPartners: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || hasAnyWritePermission

    val canDeleteCatalogAndPartners: Boolean
        get() = isAdmin || role == EmployeeRole.GERANT || hasAnyDeletePermission

    fun withAllWritePermissions(enabled: Boolean): EmployeeEntity = copy(
        canModifyBon = enabled,
        canModifyFacture = enabled,
        canModifyCreance = enabled
    )

    fun withAllDeletePermissions(enabled: Boolean): EmployeeEntity = copy(
        canDeleteBon = enabled,
        canDeleteFacture = enabled,
        canDeleteCreance = enabled
    )

    fun canModifyOperation(type: OperationType): Boolean {
        if (isAdmin || role == EmployeeRole.GERANT) return true
        return when (type.permissionCategory()) {
            DocumentPermissionCategory.BON -> canModifyBon
            DocumentPermissionCategory.FACTURE -> canModifyFacture
            DocumentPermissionCategory.CREANCE -> canModifyCreance
        }
    }

    fun canDeleteOperation(type: OperationType): Boolean {
        if (isAdmin || role == EmployeeRole.GERANT) return true
        return when (type.permissionCategory()) {
            DocumentPermissionCategory.BON -> canDeleteBon
            DocumentPermissionCategory.FACTURE -> canDeleteFacture
            DocumentPermissionCategory.CREANCE -> canDeleteCreance
        }
    }

    fun permissionDeniedMessage(action: String, category: DocumentPermissionCategory): String {
        return "Accès refusé (${name} - ${role.label}) : Vous n'avez pas l'autorisation de $action pour « ${category.label} »."
    }

    companion object {
        const val PRIMARY_ADMIN_EMAIL = "meridjaomar2023@gmail.com"
        const val SECONDARY_USER_EMAIL = "merelec.bejaia@gmail.com"
        const val SECONDARY_USER_EMAIL_COMMA_ALIAS = "merelec,bejaia@gmail.com"

        /**
         * Normalise une saisie d'adresse email en remplaçant les virgules éventuelles par des points
         * (ex: "merelec,bejaia@gmail.com" -> "merelec.bejaia@gmail.com").
         */
        fun normalizeEmail(raw: String): String {
            return raw.trim().replace(Regex("\\s+"), "").replace(',', '.').lowercase( java.util.Locale.ROOT)
        }

        fun defaultAdmin(
            email: String = PRIMARY_ADMIN_EMAIL,
            name: String = "Omar Meridja (Administrateur)",
            password: String = "admin123"
        ) = EmployeeEntity(
            id = "EMP-ADMIN-001",
            name = name,
            email = normalizeEmail(email),
            password = password,
            role = EmployeeRole.ADMIN,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = true,
            canModifyFacture = true,
            canDeleteFacture = true,
            canModifyCreance = true,
            canDeleteCreance = true,
            canManageEmployees = true
        )

        fun defaultMerelecBejaia(
            email: String = SECONDARY_USER_EMAIL,
            name: String = "Merelec Béjaïa",
            password: String = "merelec123"
        ) = EmployeeEntity(
            id = "EMP-BEJAIA-001",
            name = name,
            email = normalizeEmail(email),
            password = password,
            role = EmployeeRole.GERANT,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = true,
            canModifyFacture = true,
            canDeleteFacture = true,
            canModifyCreance = true,
            canDeleteCreance = true,
            canManageEmployees = false
        )

        fun defaultGerant() = defaultMerelecBejaia()

        fun defaultVendeur() = EmployeeEntity(
            id = "EMP-VEND-001",
            name = "Utilisateur 1 (Yacine Vendeur)",
            email = "vendeur@merelec.dz",
            password = "vendeur123",
            role = EmployeeRole.VENDEUR,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = true,
            canModifyFacture = true,
            canDeleteFacture = true,
            canModifyCreance = false,
            canDeleteCreance = false,
            canManageEmployees = false
        )

        fun defaultVendeur2() = EmployeeEntity(
            id = "EMP-VEND-002",
            name = "Utilisateur 2 (Karim Vendeur)",
            email = "vendeur2@merelec.dz",
            password = "vendeur234",
            role = EmployeeRole.VENDEUR,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = true,
            canModifyFacture = true,
            canDeleteFacture = true,
            canModifyCreance = false,
            canDeleteCreance = false,
            canManageEmployees = false
        )

        fun defaultCaissier() = EmployeeEntity(
            id = "EMP-CAIS-001",
            name = "Utilisateur 3 (Amine Caissier)",
            email = "caisse@merelec.dz",
            password = "caisse123",
            role = EmployeeRole.CAISSIER,
            isActive = true,
            canModifyBon = true,
            canDeleteBon = true,
            canModifyFacture = true,
            canDeleteFacture = true,
            canModifyCreance = true,
            canDeleteCreance = false,
            canManageEmployees = false
        )

        fun defaultThreeAdditionalUsers(): List<EmployeeEntity> = listOf(
            defaultVendeur(),
            defaultVendeur2(),
            defaultCaissier()
        )

        fun applyRolePreset(current: EmployeeEntity, newRole: EmployeeRole): EmployeeEntity {
            return when (newRole) {
                EmployeeRole.ADMIN -> current.copy(
                    role = newRole,
                    canModifyBon = true,
                    canDeleteBon = true,
                    canModifyFacture = true,
                    canDeleteFacture = true,
                    canModifyCreance = true,
                    canDeleteCreance = true,
                    canManageEmployees = true
                )
                EmployeeRole.GERANT -> current.copy(
                    role = newRole,
                    canModifyBon = true,
                    canDeleteBon = true,
                    canModifyFacture = true,
                    canDeleteFacture = true,
                    canModifyCreance = true,
                    canDeleteCreance = true,
                    canManageEmployees = false
                )
                EmployeeRole.VENDEUR -> current.copy(
                    role = newRole,
                    canModifyBon = true,
                    canDeleteBon = false,
                    canModifyFacture = false,
                    canDeleteFacture = false,
                    canModifyCreance = false,
                    canDeleteCreance = false,
                    canManageEmployees = false
                )
                EmployeeRole.CAISSIER -> current.copy(
                    role = newRole,
                    canModifyBon = false,
                    canDeleteBon = false,
                    canModifyFacture = true,
                    canDeleteFacture = false,
                    canModifyCreance = true,
                    canDeleteCreance = false,
                    canManageEmployees = false
                )
            }
        }
    }
}
