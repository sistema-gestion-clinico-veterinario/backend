package veterinaria.vargasvet.admin;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.admin.CompanyDTO;
import veterinaria.vargasvet.admin.CompanyListResponse;

public interface CompanyService {
    CompanyDTO getCompanyInfo();
    CompanyDTO updateCompanyInfo(CompanyDTO companyDTO);
    Page<CompanyListResponse> listarTodas(int page, int size);
    CompanyDTO findById(Integer id);
    CompanyDTO save(CompanyDTO dto);
    CompanyDTO update(Integer id, CompanyDTO dto);
}
