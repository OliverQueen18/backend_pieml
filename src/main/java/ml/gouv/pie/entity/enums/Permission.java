package ml.gouv.pie.entity.enums;

import lombok.Getter;

@Getter
public enum Permission {
    ADMIN_DASHBOARD_VIEW("Tableau de bord admin", "Administration"),
    DOSSIERS_VIEW("Consulter les dossiers", "Dossiers"),
    DOSSIERS_VALIDATE("Valider les dossiers", "Dossiers"),
    DOSSIERS_REJECT("Rejeter les dossiers", "Dossiers"),
    CITIZENS_VIEW("Consulter les citoyens", "Utilisateurs"),
    CITIZENS_MANAGE("Gérer les citoyens", "Utilisateurs"),
    USERS_VIEW("Consulter les comptes staff", "Utilisateurs"),
    USERS_MANAGE("Gérer les comptes staff", "Utilisateurs"),
    CENTERS_VIEW("Consulter les centres", "Configuration"),
    CENTERS_MANAGE("Gérer les centres", "Configuration"),
    TYPE_DOCUMENTS_VIEW("Consulter les types de documents", "Configuration"),
    TYPE_DOCUMENTS_MANAGE("Gérer les types de documents", "Configuration"),
    VEHICLE_BRANDS_VIEW("Consulter les marques d'engins", "Configuration"),
    VEHICLE_BRANDS_MANAGE("Gérer les marques d'engins", "Configuration"),
    VEHICLE_TYPES_VIEW("Consulter les types d'engins", "Configuration"),
    VEHICLE_TYPES_MANAGE("Gérer les types d'engins", "Configuration"),
    ROLES_VIEW("Consulter les rôles", "Configuration"),
    ROLES_MANAGE("Gérer les rôles et permissions", "Configuration"),
    NOTIFICATIONS_VIEW("Consulter les notifications", "Configuration"),
    NOTIFICATIONS_MANAGE("Gérer les notifications", "Configuration"),
    PAYMENTS_VIEW("Consulter les paiements", "Configuration"),
    PAYMENTS_MANAGE("Gérer les paiements", "Configuration"),
    TARIFFS_VIEW("Consulter les tarifs", "Configuration"),
    TARIFFS_MANAGE("Gérer les tarifs", "Configuration"),
    APPOINTMENTS_MANAGE("Gérer les rendez-vous", "Immatriculation"),
    IMMATRICULATION_PROCESS("Traiter les immatriculations", "Immatriculation");

    private final String label;
    private final String category;

    Permission(String label, String category) {
        this.label = label;
        this.category = category;
    }
}
