package veterinaria.vargasvet.modules.clinical.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.clinical.domain.enums.TipoArchivo;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "archivos_clinicos")
public class ArchivoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoArchivo tipo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "subido_por", length = 150)
    private String subidoPor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
