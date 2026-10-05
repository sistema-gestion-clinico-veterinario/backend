package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "caja")
public class Caja {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "dispositivo_token_hash", length = 64)
    private String dispositivoTokenHash;

    @Column(name = "dispositivo_info", length = 120)
    private String dispositivoInfo;

    @Column(name = "dispositivo_vinculado_at")
    private LocalDateTime dispositivoVinculadoAt;

    @Column(name = "dispositivo_ultimo_uso_at")
    private LocalDateTime dispositivoUltimoUsoAt;

    @Column(name = "creada_at", nullable = false)
    private LocalDateTime creadaAt;
}
