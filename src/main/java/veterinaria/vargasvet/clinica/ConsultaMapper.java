package veterinaria.vargasvet.clinica;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.clinica.Consulta;
import veterinaria.vargasvet.pacientes.Mascota;
import veterinaria.vargasvet.clinica.Prescripcion;
import veterinaria.vargasvet.admin.Usuario;
import veterinaria.vargasvet.clinica.ConsultaResponse;
import veterinaria.vargasvet.clinica.PrescripcionResumenResponse;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class ConsultaMapper {

    public ConsultaResponse toResponse(Consulta consulta) {
        if (consulta == null) {
            return null;
        }

        ConsultaResponse response = new ConsultaResponse();
        response.setId(consulta.getId());
        response.setVersion(consulta.getVersion());
        response.setEstado(consulta.getEstado());
        response.setTipoConsulta(consulta.getTipoConsulta());
        response.setFechaConsulta(consulta.getFechaConsulta());
        response.setMotivoConsulta(consulta.getMotivoConsulta());
        response.setAnamnesis(consulta.getAnamnesis());
        response.setExamenFisico(consulta.getExamenFisico());
        response.setPesoEnConsulta(consulta.getPesoEnConsulta());
        response.setTemperatura(consulta.getTemperatura());
        response.setFrecuenciaCardiaca(consulta.getFrecuenciaCardiaca());
        response.setFrecuenciaRespiratoria(consulta.getFrecuenciaRespiratoria());
        response.setMucosas(consulta.getMucosas());
        response.setTurgenciaPiel(consulta.getTurgenciaPiel());
        response.setVacunacionAlDia(consulta.getVacunacionAlDia());
        response.setDesparasitacionAlDia(consulta.getDesparasitacionAlDia());
        response.setObservaciones(consulta.getObservaciones());
        response.setFechaCierre(consulta.getFechaCierre());
        response.setCerradoPor(consulta.getCerradoPor());
        response.setIndicacionesReceta(consulta.getIndicacionesReceta());

        if (consulta.getCita() != null) {
            response.setCitaId(consulta.getCita().getId());
        }

        if (consulta.getVeterinario() != null) {
            response.setVeterinarioId(consulta.getVeterinario().getId());
            if (consulta.getVeterinario().getUser() != null) {
                response.setVeterinarioNombre(consulta.getVeterinario().getUser().getNombre() + " " + consulta.getVeterinario().getUser().getApellido());
            }
        }

        if (consulta.getHistoriaClinica() != null) {
            response.setHistoriaClinicaId(consulta.getHistoriaClinica().getId());
            response.setNumeroHc(consulta.getHistoriaClinica().getNumeroHc());
            response.setAntecedentesEnfermedades(consulta.getHistoriaClinica().getEnfermedades());
            response.setAntecedentesProcedimientos(consulta.getHistoriaClinica().getProcedimientos());
            response.setAntecedentesPersonales(consulta.getHistoriaClinica().getAntecedentesPersonales());
            response.setAntecedentesFamiliares(consulta.getHistoriaClinica().getAntecedentesFamiliares());
            response.setGrupoSanguineo(consulta.getHistoriaClinica().getGrupoSanguineo());

            Mascota mascota = consulta.getHistoriaClinica().getMascota();
            if (mascota != null) {
                response.setMascotaId(mascota.getId());
                response.setMascotaNombre(mascota.getNombreCompleto());
                response.setEspecie(mascota.getEspecie() != null ? mascota.getEspecie().name() : mascota.getOtraEspecie());
                response.setRaza(mascota.getRaza());
                response.setSexo(mascota.getSexo() != null ? mascota.getSexo().name() : null);
                response.setColor(mascota.getColor());
                response.setSenasParticulares(mascota.getSenasParticulares());
                
                if (mascota.getFechaNacimiento() != null) {
                    long meses = ChronoUnit.MONTHS.between(mascota.getFechaNacimiento(), LocalDate.now());
                    response.setEdadAproximadaMeses((int) meses);
                }

                if (mascota.getApoderado() != null) {
                    response.setApoderadoId(mascota.getApoderado().getId());
                    Usuario apoderadoUser = mascota.getApoderado().getUser();
                    if (apoderadoUser != null) {
                        response.setApoderadoNombre(apoderadoUser.getNombre() + " " + apoderadoUser.getApellido());
                        response.setApoderadoTelefono(apoderadoUser.getTelefono());
                        response.setApoderadoDireccion(apoderadoUser.getDireccion());
                    }
                }
            }
        }

        if (consulta.getPrescripciones() != null) {
            List<PrescripcionResumenResponse> prescripciones = consulta.getPrescripciones().stream()
                    .map(this::toPrescripcionResponse)
                    .collect(Collectors.toList());
            response.setPrescripciones(prescripciones);
        }

        return response;
    }

    public PrescripcionResumenResponse toPrescripcionResponse(Prescripcion p) {
        PrescripcionResumenResponse r = new PrescripcionResumenResponse();
        r.setId(p.getId());
        r.setMedicamento(p.getMedicamento());
        r.setPrincipioActivo(p.getPrincipioActivo());
        r.setDosis(p.getDosis());
        r.setFrecuencia(p.getFrecuencia());
        r.setDuracionDias(p.getDuracionDias());
        r.setViaAdministracion(p.getViaAdministracion());
        r.setInstrucciones(p.getInstrucciones());
        r.setFechaInicio(p.getFechaInicio());
        r.setFechaFin(p.getFechaFin());
        r.setFechaCreacion(p.getCreatedAt());
        if (p.getVeterinario() != null && p.getVeterinario().getUser() != null) {
            r.setVeterinarioNombre(p.getVeterinario().getUser().getNombre() + " " + p.getVeterinario().getUser().getApellido());
        }
        return r;
    }

    /** Para el listado global — incluye paciente y número de HC */
    public PrescripcionResumenResponse toPrescripcionListResponse(Prescripcion p) {
        PrescripcionResumenResponse r = toPrescripcionResponse(p);
        if (p.getConsulta() != null && p.getConsulta().getHistoriaClinica() != null) {
            r.setNumeroHc(p.getConsulta().getHistoriaClinica().getNumeroHc());
            if (p.getConsulta().getHistoriaClinica().getMascota() != null) {
                r.setPacienteNombre(p.getConsulta().getHistoriaClinica().getMascota().getNombreCompleto());
            }
        }
        return r;
    }
}
