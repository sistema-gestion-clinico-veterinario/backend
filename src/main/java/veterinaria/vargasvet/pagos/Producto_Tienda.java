package veterinaria.vargasvet.pagos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "productos")
public class Producto_Tienda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JsonProperty("code_reference")
    private String codeReference;
    private String name;
    private int quantity;
    @JsonProperty("discount_rate")
    private double discountRate;
    private double price;
    @JsonProperty("unit_measure_id")
    private int unitMeasureId;
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    @Column(columnDefinition = "TEXT")
    @JsonProperty("coverPath")
    private String coverPath;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(
            mappedBy = "producto",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @BatchSize(size = 10)
    private List<Producto_Imagen> imagenes;

    public void sincronizarCoverPath() {
        if ((this.coverPath == null || this.coverPath.isEmpty()) &&
                this.imagenes != null && !this.imagenes.isEmpty()) {

            Producto_Imagen imagenPrincipal = this.imagenes.stream()
                    .filter(Producto_Imagen::isPrincipal)
                    .findFirst()
                    .orElse(this.imagenes.get(0));

            if (imagenPrincipal != null) {
                this.coverPath = imagenPrincipal.getImagePath();
            }
        }
        if (this.imagenes != null && !this.imagenes.isEmpty()) {
            long countPrincipales = this.imagenes.stream()
                    .filter(Producto_Imagen::isPrincipal)
                    .count();

            if (countPrincipales > 1) {
                boolean primera = true;
                for (Producto_Imagen imagen : this.imagenes) {
                    if (primera) {
                        primera = false;
                        continue;
                    }
                    imagen.setPrincipal(false);
                }
            }
        }
    }
    @Column(name = "is_new")
    private Boolean isNew = false;
}