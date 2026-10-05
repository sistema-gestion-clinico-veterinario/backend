package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamposAvisoPrivacidad {

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no debe superar 150 caracteres")
    private String razonSocial;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^\\d{11}$", message = "El RUC debe tener 11 dígitos")
    private String ruc;

    @NotBlank(message = "El domicilio es obligatorio")
    @Size(max = 200, message = "El domicilio no debe superar 200 caracteres")
    private String domicilio;

    @NotBlank(message = "El correo para ejercer derechos es obligatorio")
    @Email(message = "El correo para ejercer derechos no es válido")
    @Size(max = 100, message = "El correo no debe superar 100 caracteres")
    private String correoDerechos;

    @Size(max = 100, message = "El código de inscripción no debe superar 100 caracteres")
    private String registroBancoDatos;

    @Size(max = 200, message = "El encargado del tratamiento no debe superar 200 caracteres")
    private String encargadoTratamiento;

    @NotEmpty(message = "Indica al menos una finalidad del tratamiento")
    @Size(max = 15, message = "Indica como máximo 15 finalidades")
    private List<@NotBlank(message = "Una finalidad está vacía") @Size(max = 250, message = "Cada finalidad admite hasta 250 caracteres") String> finalidades = new ArrayList<>();

    @NotEmpty(message = "Indica los datos obligatorios")
    @Size(max = 30, message = "Indica como máximo 30 datos obligatorios")
    private List<@NotBlank(message = "Un dato obligatorio está vacío") @Size(max = 150, message = "Cada dato admite hasta 150 caracteres") String> datosObligatorios = new ArrayList<>();

    @Size(max = 30, message = "Indica como máximo 30 datos facultativos")
    private List<@NotBlank(message = "Un dato facultativo está vacío") @Size(max = 150, message = "Cada dato admite hasta 150 caracteres") String> datosFacultativos = new ArrayList<>();

    @NotEmpty(message = "Indica a quiénes se comunican los datos")
    @Size(max = 15, message = "Indica como máximo 15 destinatarios")
    private List<@NotBlank(message = "Un destinatario está vacío") @Size(max = 250, message = "Cada destinatario admite hasta 250 caracteres") String> destinatarios = new ArrayList<>();

    @NotBlank(message = "Indica si se transfieren datos a terceros o al extranjero")
    @Size(max = 500, message = "El texto de transferencias no debe superar 500 caracteres")
    private String transferencias;

    @NotBlank(message = "Indica el plazo de conservación de los datos")
    @Size(max = 300, message = "El plazo de conservación no debe superar 300 caracteres")
    private String plazoConservacion;
}
