package veterinaria.vargasvet.modules.company.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.dto.response.CompanyListResponse;

public interface CompanyService {
    CompanyDTO getCompanyInfo();
    CompanyDTO updateCompanyInfo(CompanyDTO companyDTO);
    Page<CompanyListResponse> listarTodas(int page, int size);
    CompanyDTO findById(Integer id);
    CompanyDTO save(CompanyDTO dto);
    CompanyDTO update(Integer id, CompanyDTO dto);
}
