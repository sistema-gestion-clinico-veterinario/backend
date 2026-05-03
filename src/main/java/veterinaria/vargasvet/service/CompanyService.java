package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CompanyDTO;

public interface CompanyService {
    CompanyDTO getCompanyInfo();
    CompanyDTO updateCompanyInfo(CompanyDTO companyDTO);
}
