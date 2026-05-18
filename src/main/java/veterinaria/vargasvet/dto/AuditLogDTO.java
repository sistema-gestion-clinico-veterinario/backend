package veterinaria.vargasvet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String timestamp;
    private String userEmail;
    private String userRole;
    private Integer companyId;
    private String companyName;
    private String action;
    private String module;
    private String details;
    private String ipAddress;
}
