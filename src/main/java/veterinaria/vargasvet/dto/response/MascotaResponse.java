package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.SexoMascota;

import java.time.LocalDate;

@Data
public class MascotaResponse {
    private Long id;
    private String uuid;
    private String nombreCompleto;
    private EspecieMascota especie;
    private String otraEspecie;
    private String raza;
    private SexoMascota sexo;
    private LocalDate fechaNacimiento;
    private Double peso;
    private String color;
    private String senasParticulares;
    private Boolean esterilizado;
    private Boolean activo;
    private String fotoUrl;
    private String numeroMicrochip;
    private String observaciones;
    
    
    private Long apoderadoId;
    private String apoderadoNombreCompleto;

    private Boolean tieneHistoriaClinica;
}
