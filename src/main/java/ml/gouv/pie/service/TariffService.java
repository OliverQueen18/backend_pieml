package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.TariffRequest;
import ml.gouv.pie.entity.Tariff;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.TariffRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {

    public static final String CODE_REGISTRATION = "REGISTRATION";
    public static final String CODE_SERVICE = "SERVICE";

    private final TariffRepository tariffRepository;

    @Value("${app.payment.registration-fee:12000}")
    private BigDecimal fallbackRegistrationFee;

    @Value("${app.payment.service-fee:0}")
    private BigDecimal fallbackServiceFee;

    @Transactional(readOnly = true)
    public List<DtoMapper.TariffDto> getAllTariffs() {
        return tariffRepository.findAllByOrderByOrdreAscLibelleAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.TariffDto> getActiveTariffs() {
        return tariffRepository.findByActifTrueOrderByOrdreAscLibelleAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getRegistrationFee() {
        return tariffRepository.findByCodeAndActifTrue(CODE_REGISTRATION)
                .map(Tariff::getAmount)
                .orElse(fallbackRegistrationFee);
    }

    @Transactional(readOnly = true)
    public BigDecimal getServiceFee() {
        return tariffRepository.findByCodeAndActifTrue(CODE_REGISTRATION)
                .map(Tariff::getServiceFee)
                .or(() -> tariffRepository.findByCodeAndActifTrue(CODE_SERVICE).map(Tariff::getAmount))
                .orElse(fallbackServiceFee);
    }

    @Transactional
    public DtoMapper.TariffDto create(TariffRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (tariffRepository.existsByCode(code)) {
            throw new BusinessException("Ce code tarif existe déjà");
        }

        Tariff tariff = Tariff.builder()
                .code(code)
                .libelle(request.getLibelle().trim())
                .description(request.getDescription())
                .amount(request.getAmount())
                .serviceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO)
                .actif(request.isActif())
                .ordre(request.getOrdre())
                .build();

        tariff = tariffRepository.save(tariff);
        syncServiceTariff(tariff);
        return toDto(tariff);
    }

    @Transactional
    public DtoMapper.TariffDto update(Long id, TariffRequest request) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tarif non trouvé", HttpStatus.NOT_FOUND));

        String code = request.getCode().trim().toUpperCase();
        if (tariffRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException("Ce code tarif existe déjà");
        }

        tariff.setCode(code);
        tariff.setLibelle(request.getLibelle().trim());
        tariff.setDescription(request.getDescription());
        tariff.setAmount(request.getAmount());
        tariff.setServiceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO);
        tariff.setActif(request.isActif());
        tariff.setOrdre(request.getOrdre());

        tariff = tariffRepository.save(tariff);
        syncServiceTariff(tariff);
        return toDto(tariff);
    }

    private void syncServiceTariff(Tariff tariff) {
        if (!CODE_REGISTRATION.equals(tariff.getCode())) {
            return;
        }
        tariffRepository.findByCode(CODE_SERVICE).ifPresent(serviceTariff -> {
            serviceTariff.setAmount(tariff.getServiceFee());
            tariffRepository.save(serviceTariff);
        });
    }

    @Transactional
    public void delete(Long id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tarif non trouvé", HttpStatus.NOT_FOUND));
        tariffRepository.delete(tariff);
    }

    public DtoMapper.TariffDto toDto(Tariff tariff) {
        return DtoMapper.TariffDto.builder()
                .id(tariff.getId())
                .code(tariff.getCode())
                .libelle(tariff.getLibelle())
                .description(tariff.getDescription())
                .amount(tariff.getAmount())
                .serviceFee(tariff.getServiceFee())
                .actif(tariff.isActif())
                .ordre(tariff.getOrdre())
                .createdAt(tariff.getCreatedAt())
                .updatedAt(tariff.getUpdatedAt())
                .build();
    }
}
