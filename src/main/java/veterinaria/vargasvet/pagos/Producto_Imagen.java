package veterinaria.vargasvet.pagos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "producto_image")
@Data
public class Producto_Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_path", nullable = false, columnDefinition = "TEXT")
    private String imagePath;

    @Column(name = "is_principal", nullable = false)
    private boolean principal = false;

    @Column(name = "orden")
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto_Tienda producto;


    public Producto_Imagen() {
        this.principal = false;
        this.orden = 0;
    }
    public Producto_Imagen(String imagePath, boolean principal, Producto_Tienda producto) {
        this.imagePath = imagePath;
        this.principal = principal;
        this.producto = producto;
        this.orden = 0;
    }
}
