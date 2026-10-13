package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class EspecialidadRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s()\\-]+$", message = "El nombre solo debe contener letras, espacios, parentesis o guiones")
    private String nombre;

    @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
    @MeaningfulText(message = "La descripcion debe contener texto real, no solo numeros o simbolos")
    private String descripcion;

    @Valid
    private EmpresaReferencia company;

    public Especialidad aEntidad() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre);
        especialidad.setDescripcion(descripcion);
        if (company != null && company.id() != null) {
            Company empresa = new Company();
            empresa.setId(company.id());
            especialidad.setCompany(empresa);
        }
        return especialidad;
    }
}
