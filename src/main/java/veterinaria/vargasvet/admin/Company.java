package veterinaria.vargasvet.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "business_hours")
    private String businessHours;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @JsonIgnore
    @OneToMany(mappedBy = "company")
    private List<Usuario> usuarios;
}
