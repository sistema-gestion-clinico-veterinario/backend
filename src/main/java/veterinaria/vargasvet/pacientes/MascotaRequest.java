package veterinaria.vargasvet.pacientes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.shared.EspecieMascota;
import veterinaria.vargasvet.shared.SexoMascota;

import java.time.LocalDate;

@Data
public class MascotaRequest {

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    private String nombreCompleto;

    @NotNull(message = "Debe seleccionar una especie")
    private EspecieMascota especie;

    private String otraEspecie;

    @NotBlank(message = "La raza es obligatoria")
    private String raza;

    @NotNull(message = "El sexo es obligatorio")
    private SexoMascota sexo;

    private LocalDate fechaNacimiento;
    private Double peso;
    private String color;
    private String senasParticulares;
    private Boolean esterilizado;
    private String fotoUrl;
    private String numeroMicrochip;
    private String observaciones;

    @NotNull(message = "Debe vincular un apoderado (dueño) a la mascota")
    private Long apoderadoId;
}
