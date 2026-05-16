package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.dto.request.CompanyOperatingHourDTO;
import veterinaria.vargasvet.dto.response.CompanyListResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CompanyService;
import veterinaria.vargasvet.util.BusinessValidator;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final BusinessValidator businessValidator;

    @Override
    public CompanyDTO getCompanyInfo() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Company company;
        
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + companyId));
        } else {
            company = companyRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No se ha configurado ninguna empresa aún"));
        }
        return mapToDTO(company);
    }

    @Override
    @Transactional
    public CompanyDTO updateCompanyInfo(CompanyDTO dto) {
        Integer id = dto.getId();
        if (id == null) {
            return companyRepository.findAll().stream()
                    .findFirst()
                    .map(c -> update(c.getId(), dto))
                    .orElseGet(() -> save(dto));
        }
        return update(id, dto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyListResponse> listarTodas(int page, int size) {
        return companyRepository.findAll(PageRequest.of(page, size, Sort.by("name").ascending()))
                .map(this::toListResponse);
    }

    @Override
    public CompanyDTO findById(Integer id) {
        return companyRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public CompanyDTO save(CompanyDTO dto) {
        Company company = new Company();
        updateEntityFromDTO(company, dto);
        company.setActivo(true);
        Company savedCompany = companyRepository.save(company);
        saveOperatingHours(savedCompany, dto.getOperatingHours());
        return mapToDTO(savedCompany);
    }

    @Override
    @Transactional
    public CompanyDTO update(Integer id, CompanyDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));
        if (!company.isActivo() && !SecurityUtils.isSuperAdmin()) {
            throw new IllegalStateException("La empresa está inactiva. Solo un super administrador puede modificarla.");
        }
        updateEntityFromDTO(company, dto);
        Company savedCompany = companyRepository.save(company);
        saveOperatingHours(savedCompany, dto.getOperatingHours());
        return mapToDTO(savedCompany);
    }

    private void updateEntityFromDTO(Company company, CompanyDTO dto) {
        company.setName(dto.getName());
        company.setRuc(dto.getRuc());
        company.setAddress(dto.getAddress());
        company.setPhone(dto.getPhone());
        company.setEmail(dto.getEmail());
        company.setLogoUrl(dto.getLogoUrl());
        company.setWebsite(dto.getWebsite());
        company.setDescription(dto.getDescription());
        company.setBusinessHours(dto.getBusinessHours());
    }

    private void saveOperatingHours(Company company, List<CompanyOperatingHourDTO> hoursDTO) {
        if (hoursDTO == null || hoursDTO.isEmpty()) return;
        
        for (CompanyOperatingHourDTO hDTO : hoursDTO) {
            DiaSemana dia = DiaSemana.valueOf(hDTO.getDiaSemana());
            CompanyOperatingHour hour = companyOperatingHourRepository
                    .findByCompanyIdAndDiaSemana(company.getId(), dia)
                    .orElse(new CompanyOperatingHour());
            
            hour.setCompany(company);
            hour.setDiaSemana(dia);
            hour.setOpeningTime(hDTO.getOpeningTime());
            hour.setClosingTime(hDTO.getClosingTime());
            hour.setIsOpen(hDTO.getIsOpen() != null ? hDTO.getIsOpen() : true);
            companyOperatingHourRepository.save(hour);
        }
    }

    private CompanyListResponse toListResponse(Company company) {
        CompanyListResponse response = new CompanyListResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setRuc(company.getRuc());
        response.setAddress(company.getAddress());
        response.setPhone(company.getPhone());
        response.setEmail(company.getEmail());
        response.setActivo(company.isActivo());
        response.setBusinessHours(company.getBusinessHours());
        return response;
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
        
        dto.setOperatingHours(companyOperatingHourRepository.findByCompanyId(company.getId()).stream()
                .map(h -> {
                    CompanyOperatingHourDTO hDTO = new CompanyOperatingHourDTO();
                    hDTO.setDiaSemana(h.getDiaSemana().name());
                    hDTO.setOpeningTime(h.getOpeningTime());
                    hDTO.setClosingTime(h.getClosingTime());
                    hDTO.setIsOpen(h.getIsOpen());
                    return hDTO;
                }).collect(Collectors.toList()));
        
        return dto;
    }
}
