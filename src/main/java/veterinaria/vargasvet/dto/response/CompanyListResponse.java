package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class CompanyListResponse {
    private Integer id;
    private String name;
    private String ruc;
    private String address;
    private String phone;
    private String email;
    private Boolean activo;
}
