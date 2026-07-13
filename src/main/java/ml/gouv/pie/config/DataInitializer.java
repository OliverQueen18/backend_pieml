package ml.gouv.pie.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.entity.*;
import ml.gouv.pie.entity.enums.*;
import ml.gouv.pie.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ml.gouv.pie.entity.enums.Permission;
import ml.gouv.pie.util.DefaultRolePermissions;
import ml.gouv.pie.service.TariffService;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.DependsOn({"citizenSchemaMigration", "centerSchemaMigration", "roleSchemaMigration", "vehicleLookupSchemaMigration"})
public class DataInitializer {

    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final CenterRepository centerRepository;
    private final DossierRepository dossierRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final RoleDefinitionRepository roleDefinitionRepository;
    private final TariffRepository tariffRepository;
    private final VehicleBrandRepository vehicleBrandRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        seedTypeDocuments();
        seedVehicleBrands();
        seedVehicleTypes();
        seedCenters();
        seedRoles();
        seedTariffs();
        seedAdminUser();
        seedDemoUser();
        log.info("Données initiales chargées");
    }

    private void seedTypeDocuments() {
        if (typeDocumentRepository.count() > 0) return;

        typeDocumentRepository.saveAll(List.of(
                type("CARTE_NINA", "Carte NINA", "Pièce d'identité nationale", true, 1),
                type("FACTURE_ACHAT", "Facture d'achat", "Facture ou reçu d'achat de l'engin", true, 2),
                type("CERTIFICAT_VENTE", "Certificat de vente", "Certificat de vente ou cession", true, 3),
                type("DECLARATION_DOUANE", "Déclaration de douane", "Pour les engins importés", false, 4),
                type("PHOTO_ENGIN", "Photo de l'engin", "Photo complète du véhicule", true, 5),
                type("PHOTO_MOTEUR", "Photo du numéro moteur", "Photo lisible du numéro moteur", true, 6),
                type("PHOTO_CHASSIS", "Photo du numéro châssis", "Photo lisible du numéro châssis", true, 7),
                type("ANCIENNE_CARTE_GRISE", "Ancienne carte grise", "Si renouvellement ou mutation", false, 8)
        ));
    }

    private TypeDocument type(String code, String libelle, String description, boolean obligatoire, int ordre) {
        return TypeDocument.builder()
                .code(code)
                .libelle(libelle)
                .description(description)
                .obligatoire(obligatoire)
                .actif(true)
                .ordre(ordre)
                .build();
    }

    private void seedVehicleBrands() {
        ensureBrand("TVS", "TVS", 1);
        ensureBrand("BOXER_BAJAJ", "Boxer (Bajaj)", 2);
        ensureBrand("APSONIC", "Apsonic", 3);
        ensureBrand("AOJUN", "Aojun", 4);
        ensureBrand("SANYA", "Sanya", 5);
        ensureBrand("HAOJUE", "Haojue", 6);
        ensureBrand("DAYUN", "Dayun", 7);
        ensureBrand("LIFAN", "Lifan", 8);
        ensureBrand("JINCHENG", "Jincheng", 9);
        ensureBrand("SENKE", "Senke", 10);
        ensureBrand("HONDA", "Honda", 11);
        ensureBrand("YAMAHA", "Yamaha", 12);
        ensureBrand("SUZUKI", "Suzuki", 13);
        ensureBrand("KAWASAKI", "Kawasaki", 14);
        ensureBrand("KTM", "KTM", 15);
        ensureBrand("ROYAL_ENFIELD", "Royal Enfield", 16);
        ensureBrand("AUTRE", "Autre", 17);
    }

    private void ensureBrand(String code, String libelle, int ordre) {
        if (vehicleBrandRepository.findByCode(code).isPresent()) return;
        vehicleBrandRepository.save(lookupBrand(code, libelle, ordre));
    }

    private void seedVehicleTypes() {
        ensureType("MOTO", "Moto", 1);
        ensureType("JAKARTA", "Jakarta", 2);
        ensureType("TRICYCLE", "Tricycle", 3);
        ensureType("QUADRICYCLE", "Quadricycle", 4);
        ensureType("SCOOTER", "Scooter", 5);
    }

    private void ensureType(String code, String libelle, int ordre) {
        if (vehicleTypeRepository.findByCode(code).isPresent()) return;
        vehicleTypeRepository.save(lookupType(code, libelle, ordre));
    }

    private VehicleBrand lookupBrand(String code, String libelle, int ordre) {
        return VehicleBrand.builder()
                .code(code)
                .libelle(libelle)
                .actif(true)
                .ordre(ordre)
                .build();
    }

    private VehicleType lookupType(String code, String libelle, int ordre) {
        return VehicleType.builder()
                .code(code)
                .libelle(libelle)
                .actif(true)
                .ordre(ordre)
                .build();
    }

    private void seedTariffs() {
        if (tariffRepository.count() > 0) return;

        tariffRepository.saveAll(List.of(
                Tariff.builder()
                        .code(TariffService.CODE_REGISTRATION)
                        .libelle("Frais d'immatriculation")
                        .description("Tarif forfaitaire pour l'immatriculation d'un engin à deux roues")
                        .amount(new BigDecimal("12000"))
                        .serviceFee(new BigDecimal("0"))
                        .actif(true)
                        .ordre(1)
                        .build(),
                Tariff.builder()
                        .code(TariffService.CODE_SERVICE)
                        .libelle("Frais de service")
                        .description("Frais de traitement en ligne via Trésor Pay")
                        .amount(new BigDecimal("0"))
                        .serviceFee(new BigDecimal("0"))
                        .actif(true)
                        .ordre(2)
                        .build()
        ));
    }

    private void seedCenters() {
        if (centerRepository.count() > 0) return;

        centerRepository.save(Center.builder()
                .name("Hamdallaye").city("Bamako")
                .address("Avenue Cheick Zayed, Hamdallaye ACI")
                .latitude(12.6244).longitude(-7.9895)
                .dailyCapacity(50).active(true).build());
        centerRepository.save(Center.builder()
                .name("Badalabougou").city("Bamako")
                .address("Route de Koulikoro, Badalabougou")
                .latitude(12.6100).longitude(-7.9800)
                .dailyCapacity(40).active(true).build());
        centerRepository.save(Center.builder()
                .name("Centre Ségou").city("Ségou")
                .address("Quartier Somonosso")
                .latitude(13.4317).longitude(-6.2603)
                .dailyCapacity(30).active(true).build());
        centerRepository.save(Center.builder()
                .name("Centre Sikasso").city("Sikasso")
                .address("Avenue de la République")
                .latitude(11.3175).longitude(-5.6667)
                .dailyCapacity(30).active(true).build());
    }

    private void seedRoles() {
        if (roleDefinitionRepository.count() == 0) {
            roleDefinitionRepository.saveAll(List.of(
                    roleDef(Role.SUPER_ADMIN, "Super administrateur",
                            "Accès complet à la plateforme et à la configuration système", true,
                            DefaultRolePermissions.forRole(Role.SUPER_ADMIN)),
                    roleDef(Role.ADMIN, "Gestionnaire de Centre",
                            "Gère un centre et peut créer des comptes Public, validateurs et immatriculateurs", true,
                            DefaultRolePermissions.forRole(Role.ADMIN)),
                    roleDef(Role.AUDIT, "Auditeur",
                            "Consultation des informations des centres associés (lecture seule)", true,
                            DefaultRolePermissions.forRole(Role.AUDIT)),
                    roleDef(Role.VALIDATEUR, "Validateur",
                            "Validation et rejet des dossiers d'immatriculation", true,
                            DefaultRolePermissions.forRole(Role.VALIDATEUR)),
                    roleDef(Role.IMMATRICULATEUR, "Immatriculateur",
                            "Traitement des rendez-vous et immatriculations sur site", true,
                            DefaultRolePermissions.forRole(Role.IMMATRICULATEUR)),
                    roleDef(Role.UTILISATEUR, "Utilisateur",
                            "Consultation opérationnelle : dossiers, citoyens et notifications", true,
                            DefaultRolePermissions.forRole(Role.UTILISATEUR)),
                    roleDef(Role.PUBLIC, "Public",
                            "Visualisation des statistiques uniquement", true,
                            DefaultRolePermissions.forRole(Role.PUBLIC)),
                    roleDef(Role.CITOYEN, "Citoyen",
                            "Dépôt et suivi des demandes d'immatriculation", true,
                            DefaultRolePermissions.forRole(Role.CITOYEN))
            ));
        }
        seedMissingRolePermissions();
        syncRoleCatalog();
        syncAdminPermissions();
    }

    private void syncAdminPermissions() {
        EnumSet<Permission> allPermissions = EnumSet.allOf(Permission.class);
        for (RoleDefinition role : roleDefinitionRepository.findAll()) {
            if (role.getCode() == Role.SUPER_ADMIN
                    && (role.getPermissions() == null || !role.getPermissions().containsAll(allPermissions))) {
                role.setPermissions(EnumSet.copyOf(allPermissions));
                roleDefinitionRepository.save(role);
            }
        }
    }

    private void syncRoleCatalog() {
        upsertRoleDefinition(Role.ADMIN, "Gestionnaire de Centre",
                "Gère un centre et peut créer des comptes Public, validateurs et immatriculateurs",
                DefaultRolePermissions.forRole(Role.ADMIN));
        upsertRoleDefinition(Role.AUDIT, "Auditeur",
                "Consultation des informations des centres associés (lecture seule)",
                DefaultRolePermissions.forRole(Role.AUDIT));
        upsertRoleDefinition(Role.UTILISATEUR, "Utilisateur",
                "Consultation opérationnelle : dossiers, citoyens et notifications",
                DefaultRolePermissions.forRole(Role.UTILISATEUR));
        upsertRoleDefinition(Role.PUBLIC, "Public",
                "Visualisation des statistiques uniquement",
                DefaultRolePermissions.forRole(Role.PUBLIC));
    }

    private void upsertRoleDefinition(Role code, String label, String description, Set<Permission> permissions) {
        roleDefinitionRepository.findByCode(code).ifPresentOrElse(role -> {
            role.setLabel(label);
            role.setDescription(description);
            if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                role.setPermissions(EnumSet.copyOf(permissions));
            }
            roleDefinitionRepository.save(role);
        }, () -> roleDefinitionRepository.save(roleDef(code, label, description, true, permissions)));
    }

    private void seedMissingRolePermissions() {
        for (RoleDefinition role : roleDefinitionRepository.findAll()) {
            if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                role.setPermissions(EnumSet.copyOf(DefaultRolePermissions.forRole(role.getCode())));
                roleDefinitionRepository.save(role);
            }
        }
    }

    private RoleDefinition roleDef(Role code, String label, String description, boolean system,
                                   Set<Permission> permissions) {
        return RoleDefinition.builder()
                .code(code)
                .label(label)
                .description(description)
                .active(true)
                .systemRole(system)
                .permissions(EnumSet.copyOf(permissions))
                .build();
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail("admin@pie.ml")) return;

        User admin = User.builder()
                .email("admin@pie.ml")
                .password(passwordEncoder.encode("password123"))
                .phone("+22370990000")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .otpVerified(true)
                .build();
        userRepository.save(admin);
        log.info("Compte administrateur créé : admin@pie.ml / password123");
    }

    private void seedDemoUser() {
        if (userRepository.existsByEmail("abdoulaye.traore@example.ml")) return;

        User user = User.builder()
                .email("abdoulaye.traore@example.ml")
                .password(passwordEncoder.encode("password123"))
                .phone("+22370123456")
                .role(Role.CITOYEN)
                .enabled(true)
                .otpVerified(true)
                .build();
        userRepository.save(user);

        Citizen citizen = Citizen.builder()
                .user(user)
                .firstName("Abdoulaye")
                .lastName("Traoré")
                .nina("123456789012345")
                .address("Hamdallaye ACI, Bamako")
                .latitude(12.6392)
                .longitude(-8.0029)
                .build();
        citizenRepository.save(citizen);

        List<TypeDocument> types = typeDocumentRepository.findByActifTrueOrderByOrdreAscLibelleAsc();
        VehicleBrand yamaha = vehicleBrandRepository.findByCode("YAMAHA").orElse(null);
        VehicleBrand honda = vehicleBrandRepository.findByCode("HONDA").orElse(null);
        VehicleType moto = vehicleTypeRepository.findByCode("MOTO").orElse(null);

        Dossier dossier = Dossier.builder()
                .referenceNumber("MD2024/000123")
                .citizen(citizen)
                .status(DossierStatus.IN_REVIEW)
                .build();
        dossierRepository.save(dossier);

        Vehicle vehicle = Vehicle.builder()
                .dossier(dossier)
                .brandEntity(yamaha)
                .vehicleTypeEntity(moto)
                .brand("Yamaha")
                .vehicleType(moto != null ? moto.getLibelle() : "Moto")
                .model("XTZ 125")
                .engineCapacity("125cc")
                .engineNumber("ENG-YAM-123456")
                .chassisNumber("CHS-YAM-789012")
                .color("Noir")
                .year(2023)
                .countryOfOrigin("Japon")
                .build();
        dossier.setVehicle(vehicle);

        for (TypeDocument typeDocument : types) {
            boolean optional = !typeDocument.isObligatoire();
            Document doc = Document.builder()
                    .dossier(dossier)
                    .typeDocument(typeDocument)
                    .status(optional ? DocumentStatus.PENDING : DocumentStatus.UPLOADED)
                    .fileName(optional ? null : typeDocument.getCode().toLowerCase() + ".pdf")
                    .build();
            dossier.getDocuments().add(doc);
        }

        Payment payment = Payment.builder()
                .dossier(dossier)
                .amount(new BigDecimal("12000"))
                .serviceFee(new BigDecimal("0"))
                .totalAmount(new BigDecimal("12000"))
                .status(PaymentStatus.PENDING)
                .build();

        Dossier dossier2 = Dossier.builder()
                .referenceNumber("MD2024/000098")
                .citizen(citizen)
                .status(DossierStatus.COMPLETED)
                .build();
        dossierRepository.save(dossier2);

        Vehicle vehicle2 = Vehicle.builder()
                .dossier(dossier2)
                .brandEntity(honda)
                .vehicleTypeEntity(moto)
                .brand("Honda")
                .vehicleType(moto != null ? moto.getLibelle() : "Moto")
                .model("CB125F")
                .engineCapacity("125cc")
                .engineNumber("ENG-HON-654321")
                .chassisNumber("CHS-HON-210987")
                .color("Rouge")
                .year(2022)
                .countryOfOrigin("Japon")
                .registrationNumber("ML-BKO-2024-98765")
                .build();
        dossier2.setVehicle(vehicle2);

        Appointment appointment = Appointment.builder()
                .dossier(dossier2)
                .center(centerRepository.findAll().get(0))
                .appointmentDate(LocalDate.of(2024, 5, 20))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();
        dossier2.setAppointment(appointment);

        dossierRepository.save(dossier);
        dossierRepository.save(dossier2);
    }
}
