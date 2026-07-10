package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDate;

@Data
public class MascotaRequest {

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombreCompleto;

    @NotNull(message = "Debe seleccionar una especie")
    private EspecieMascota especie;

    @Size(max = 60, message = "La especie personalizada no debe superar 60 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}\\s]+$", message = "La especie personalizada solo debe contener letras y espacios")
    private String otraEspecie;

    @NotNull(message = "Debe seleccionar una raza")
    private Long razaId;

    @NotNull(message = "El sexo es obligatorio")
    private SexoMascota sexo;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
    @DecimalMax(value = "120.0", message = "El peso no debe superar 120 kg")
    private Double peso;

    @Size(max = 50, message = "El color no debe superar 50 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}\\s]+$", message = "El color solo debe contener letras y espacios")
    private String color;

    @Size(max = 300, message = "Las senas particulares no deben superar 300 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las senas particulares contienen caracteres no permitidos")
    @MeaningfulText(message = "Las senas particulares deben contener texto real, no solo numeros o simbolos")
    private String senasParticulares;
    private Boolean esterilizado;
    @Size(max = 500, message = "La URL de foto no debe superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://[^\\s<>]+$", message = "La URL de foto debe iniciar con http:// o https:// y no contener espacios")
    private String fotoUrl;

    @Size(max = 30, message = "El microchip no debe superar 30 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*$", message = "El microchip contiene caracteres no permitidos")
    @MeaningfulText(requireLetter = false, message = "El microchip no puede estar compuesto solo por simbolos")
    private String numeroMicrochip;

    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las observaciones contienen caracteres no permitidos")
    @MeaningfulText(message = "Las observaciones deben contener texto real, no solo numeros o simbolos")
    private String observaciones;

    @NotNull(message = "Debe vincular un apoderado (dueño) a la mascota")
    private Long apoderadoId;

    @AssertTrue(message = "Debe especificar la especie cuando selecciona OTRO")
    public boolean isOtraEspecieValida() {
        return especie != EspecieMascota.OTRO || (otraEspecie != null && !otraEspecie.isBlank());
    }

    @AssertTrue(message = "El peso no corresponde al rango esperado para la especie")
    public boolean isPesoValidoParaEspecie() {
        if (peso == null || especie == null) return true;
        return switch (especie) {
            case PERRO -> peso >= 0.5 && peso <= 120.0;
            case GATO -> peso >= 0.3 && peso <= 20.0;
            case AVE -> peso >= 0.02 && peso <= 5.0;
            case REPTIL -> peso >= 0.01 && peso <= 100.0;
            case ROEDOR -> peso >= 0.02 && peso <= 10.0;
            case EXOTICO, OTRO -> peso >= 0.01 && peso <= 120.0;
        };
    }

    @AssertTrue(message = "La edad no corresponde al rango esperado para la especie")
    public boolean isEdadValidaParaEspecie() {
        if (fechaNacimiento == null || especie == null) return true;
        LocalDate fechaMinima = LocalDate.now().minusYears(maxEdadPorEspecie());
        return !fechaNacimiento.isBefore(fechaMinima);
    }

    private int maxEdadPorEspecie() {
        return switch (especie) {
            case PERRO, GATO -> 20;
            case AVE -> 80;
            case REPTIL, EXOTICO, OTRO -> 100;
            case ROEDOR -> 15;
        };
    }
}
