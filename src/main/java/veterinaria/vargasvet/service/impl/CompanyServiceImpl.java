package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.service.CompanyService;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    public CompanyDTO getCompanyInfo() {
        Company company = companyRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se ha configurado la empresa aún"));
        return mapToDTO(company);
    }

    @Override
    @Transactional
    public CompanyDTO updateCompanyInfo(CompanyDTO dto) {
        Company company = companyRepository.findAll().stream()
                .findFirst()
                .orElse(new Company());

        company.setName(dto.getName());
        company.setRuc(dto.getRuc());
        company.setAddress(dto.getAddress());
        company.setPhone(dto.getPhone());
        company.setEmail(dto.getEmail());
        company.setLogoUrl(dto.getLogoUrl());
        company.setWebsite(dto.getWebsite());
        company.setDescription(dto.getDescription());
        company.setBusinessHours(dto.getBusinessHours());
        company.setActivo(true);

        Company saved = companyRepository.save(company);
        return mapToDTO(saved);
    }

    private CompanyDTO mapToDTO(Company company) {
        CompanyDTO dto = new CompanyDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setRuc(company.getRuc());
        dto.setAddress(company.getAddress());
        dto.setPhone(company.getPhone());
        dto.setEmail(company.getEmail());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setWebsite(company.getWebsite());
        dto.setDescription(company.getDescription());
        dto.setBusinessHours(company.getBusinessHours());
        return dto;
    }
}
