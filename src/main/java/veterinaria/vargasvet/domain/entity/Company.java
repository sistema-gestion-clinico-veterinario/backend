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

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "company")
    private List<Usuario> usuarios;
}
