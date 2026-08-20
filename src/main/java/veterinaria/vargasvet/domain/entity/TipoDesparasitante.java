package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tipo_desparasitante", uniqueConstraints = @UniqueConstraint(
        name = "uk_tipo_desparasitante_company_nombre_especie",
        columnNames = {"company_id", "nombre", "especie"}))
public class TipoDesparasitante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EspecieMascota especie;

    @Column(name = "periodicidad_meses_sugerida")
    private Integer periodicidadMesesSugerida;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 150)
    private String createdBy;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }
}