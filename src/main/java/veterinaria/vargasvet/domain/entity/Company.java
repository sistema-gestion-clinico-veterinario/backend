package veterinaria.vargasvet.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "company")
public class Company {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String ruc;

    private String address;
    private String phone;
    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    /** Identificador en la URL (systemvet.com/<slug>/login) - resuelve la empresa
     * antes del login, sin pantalla de seleccion. Ver CompanyRepository.findBySlug. */
    @Column(nullable = false, unique = true)
    private String slug;

    /** Color de marca (hex, ej. #006BA8) para el login dinamico de esa empresa. */
    @Column(name = "color_primario", length = 7)
    private String colorPrimario;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "business_hours")
    private String businessHours;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "company")
    private List<Usuario> usuarios;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
