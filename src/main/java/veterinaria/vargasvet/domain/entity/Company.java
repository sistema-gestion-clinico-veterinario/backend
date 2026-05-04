package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "business_hours")
    private String businessHours;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "company")
    private List<Usuario> usuarios;
}
