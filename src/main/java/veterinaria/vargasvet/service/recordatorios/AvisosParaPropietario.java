package veterinaria.vargasvet.service.recordatorios;

import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;
import java.util.List;

public record AvisosParaPropietario(Long apoderadoId, String correo, String nombre, Clinica clinica, List<Aviso> avisos) {

    public AvisosParaPropietario {
        avisos = List.copyOf(avisos);
    }

    public record Aviso(Long controlId, String mascota, String control, TipoControlPreventivo tipo,
                        LocalDate fechaRecomendada, TipoAvisoRecordatorio tipoAviso) {
    }

    public record Clinica(String nombre, String logoUrl, String correo, String telefono, String direccion) {
    }
}
