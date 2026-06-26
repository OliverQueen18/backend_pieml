package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.*;
import ml.gouv.pie.entity.enums.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DossierMapperService {

    private final TypeDocumentService typeDocumentService;

    public DtoMapper.DossierDto toDto(Dossier dossier) {
        DtoMapper.VehicleDto vehicleDto = null;
        if (dossier.getVehicle() != null) {
            Vehicle v = dossier.getVehicle();
            vehicleDto = DtoMapper.VehicleDto.builder()
                    .brandId(v.getBrandEntity() != null ? v.getBrandEntity().getId() : null)
                    .vehicleTypeId(v.getVehicleTypeEntity() != null ? v.getVehicleTypeEntity().getId() : null)
                    .brand(v.getBrand())
                    .vehicleType(v.getVehicleType())
                    .model(v.getModel())
                    .engineCapacity(v.getEngineCapacity())
                    .engineNumber(v.getEngineNumber())
                    .chassisNumber(v.getChassisNumber())
                    .color(v.getColor())
                    .year(v.getYear())
                    .countryOfOrigin(v.getCountryOfOrigin())
                    .registrationNumber(v.getRegistrationNumber())
                    .build();
        }

        List<DtoMapper.DocumentDto> docDtos = dossier.getDocuments().stream()
                .sorted(Comparator.comparingInt(d -> d.getTypeDocument().getOrdre()))
                .map(d -> DtoMapper.DocumentDto.builder()
                        .id(d.getId())
                        .typeDocument(typeDocumentService.toDto(d.getTypeDocument()))
                        .status(d.getStatus())
                        .fileName(d.getFileName())
                        .fileSize(d.getFileSize())
                        .contentType(d.getContentType())
                        .uploadedAt(d.getUploadedAt())
                        .build())
                .toList();

        DtoMapper.PaymentDto paymentDto = null;
        if (dossier.getPayment() != null) {
            Payment p = dossier.getPayment();
            Center paymentCenter = dossier.getProcessingCenter();
            Vehicle paymentVehicle = dossier.getVehicle();
            VehicleType paymentVehicleType = paymentVehicle != null ? paymentVehicle.getVehicleTypeEntity() : null;
            String paymentVehicleTypeLabel = paymentVehicleType != null
                    ? paymentVehicleType.getLibelle()
                    : (paymentVehicle != null ? paymentVehicle.getVehicleType() : null);
            paymentDto = DtoMapper.PaymentDto.builder()
                    .id(p.getId())
                    .amount(p.getAmount())
                    .serviceFee(p.getServiceFee())
                    .totalAmount(p.getTotalAmount())
                    .transactionId(p.getTransactionId())
                    .status(p.getStatus())
                    .paymentMethod(p.getPaymentMethod())
                    .paymentDate(p.getPaymentDate())
                    .centerId(paymentCenter != null ? paymentCenter.getId() : null)
                    .centerName(paymentCenter != null ? paymentCenter.getName() : null)
                    .centerCity(paymentCenter != null ? paymentCenter.getCity() : null)
                    .vehicleTypeId(paymentVehicleType != null ? paymentVehicleType.getId() : null)
                    .vehicleTypeLabel(paymentVehicleTypeLabel)
                    .build();
        }

        DtoMapper.AppointmentDto appointmentDto = null;
        if (dossier.getAppointment() != null) {
            Appointment a = dossier.getAppointment();
            appointmentDto = DtoMapper.AppointmentDto.builder()
                    .id(a.getId())
                    .centerId(a.getCenter().getId())
                    .centerName(a.getCenter().getName())
                    .centerCity(a.getCenter().getCity())
                    .centerAddress(a.getCenter().getAddress())
                    .centerLatitude(a.getCenter().getLatitude())
                    .centerLongitude(a.getCenter().getLongitude())
                    .appointmentDate(a.getAppointmentDate())
                    .appointmentTime(a.getAppointmentTime())
                    .status(a.getStatus())
                    .build();
        }

        DtoMapper.ProcessingCenterDto processingCenterDto = null;
        if (dossier.getProcessingCenter() != null) {
            Center center = dossier.getProcessingCenter();
            processingCenterDto = DtoMapper.ProcessingCenterDto.builder()
                    .id(center.getId())
                    .name(center.getName())
                    .city(center.getCity())
                    .address(center.getAddress())
                    .build();
        }

        DtoMapper.VehicleDeclarationDto declarationDto = null;
        if (dossier.getVehicleDeclaration() != null) {
            VehicleDeclaration declaration = dossier.getVehicleDeclaration();
            declarationDto = DtoMapper.VehicleDeclarationDto.builder()
                    .id(declaration.getId())
                    .declarationType(declaration.getDeclarationType())
                    .fileName(declaration.getFileName())
                    .fileSize(declaration.getFileSize())
                    .contentType(declaration.getContentType())
                    .declaredAt(declaration.getDeclaredAt())
                    .build();
        }

        DtoMapper.PlateDeliveryDto plateDeliveryDto = null;
        if (dossier.getPlateDelivery() != null) {
            PlateDelivery plateDelivery = dossier.getPlateDelivery();
            plateDeliveryDto = DtoMapper.PlateDeliveryDto.builder()
                    .id(plateDelivery.getId())
                    .plateNumber(plateDelivery.getPlateNumber())
                    .deliveryDate(plateDelivery.getDeliveryDate())
                    .collectorFirstName(plateDelivery.getCollectorFirstName())
                    .collectorLastName(plateDelivery.getCollectorLastName())
                    .collectorPhone(plateDelivery.getCollectorPhone())
                    .collectorAddress(plateDelivery.getCollectorAddress())
                    .fileName(plateDelivery.getFileName())
                    .fileSize(plateDelivery.getFileSize())
                    .contentType(plateDelivery.getContentType())
                    .createdAt(plateDelivery.getCreatedAt())
                    .updatedAt(plateDelivery.getUpdatedAt())
                    .build();
        }

        DtoMapper.DossierCitizenDto citizenDto = null;
        if (dossier.getCitizen() != null) {
            Citizen citizen = dossier.getCitizen();
            User user = citizen.getUser();
            citizenDto = DtoMapper.DossierCitizenDto.builder()
                    .id(citizen.getId())
                    .firstName(citizen.getFirstName())
                    .lastName(citizen.getLastName())
                    .email(user != null ? user.getEmail() : null)
                    .phone(user != null ? user.getPhone() : null)
                    .nina(citizen.getNina())
                    .address(citizen.getAddress())
                    .latitude(citizen.getLatitude())
                    .longitude(citizen.getLongitude())
                    .build();
        }

        return DtoMapper.DossierDto.builder()
                .id(dossier.getId())
                .referenceNumber(dossier.getReferenceNumber())
                .status(dossier.getStatus())
                .rejectionReason(dossier.getRejectionReason())
                .citizen(citizenDto)
                .vehicle(vehicleDto)
                .documents(docDtos)
                .payment(paymentDto)
                .appointment(appointmentDto)
                .processingCenter(processingCenterDto)
                .vehicleDeclaration(declarationDto)
                .plateDelivery(plateDeliveryDto)
                .createdAt(dossier.getCreatedAt())
                .updatedAt(dossier.getUpdatedAt())
                .build();
    }
}
