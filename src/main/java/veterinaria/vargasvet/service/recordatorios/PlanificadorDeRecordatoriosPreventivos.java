package veterinaria.vargasvet.service.recordatorios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.CompanyComplianceService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.OwnerContactPolicy;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Aviso;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlanificadorDeRecordatoriosPreventivos {

    private static final int DIAS_DE_ANTICIPACION = 7;
    private static final int DIAS_MINIMOS_ENTRE_AVISOS = 7;
    private static final Set<EstadoControlPreventivo> ESTADOS_CANDIDATOS = EnumSet.of(
            EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
            EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO);

    private final ControlPreventivoRepository controlRepository;
    private final RecordatorioPreventivoRepository recordatorioRepository;
    private final OwnerContactPolicy ownerContactPolicy;
    private final PetOwnershipService petOwnershipService;
    private final ConsentimientoDatosService consentimientoDatosService;
    private final CompanyComplianceService complianceService;

    @Transactional
    public List<AvisosParaPropietario> planificar(LocalDate hoy) {
        List<ControlPreventivo> candidatos = controlRepository.findReminderCandidates(
                hoy.plusDays(DIAS_DE_ANTICIPACION), ESTADOS_CANDIDATOS);
        Set<ClaveDeAviso> yaRegistrados = clavesYaRegistradas(candidatos);
        Map<Long, List<Apoderado>> destinatariosPorMascota = petOwnershipService.destinatariosDeAvisos(
                candidatos.stream().map(ControlPreventivo::getMascota).distinct().toList());
        Set<ClaveDeConsentimiento> autorizados = autorizadosARecibirlos(destinatariosPorMascota);

        Map<Long, Apoderado> destinatarios = new LinkedHashMap<>();
        Map<Long, List<AvisoPendiente>> pendientes = new LinkedHashMap<>();
        for (ControlPreventivo control : candidatos) {
            TipoAvisoRecordatorio tipoAviso = tipoDeAviso(control, hoy);
            actualizarEstado(control, hoy);
            if (tipoAviso == null) continue;
            for (Apoderado destinatario : destinatariosPorMascota.getOrDefault(control.getMascota().getId(), List.of())) {
                if (!puedeRecibirlo(destinatario, autorizados)
                        || yaRegistrados.contains(new ClaveDeAviso(control.getId(), tipoAviso,
                        control.getFechaRecomendada(), destinatario.getId()))) {
                    continue;
                }
                destinatarios.put(destinatario.getId(), destinatario);
                pendientes.computeIfAbsent(destinatario.getId(), id -> new ArrayList<>())
                        .add(new AvisoPendiente(control, tipoAviso));
            }
        }
        if (pendientes.isEmpty()) return List.of();

        Set<Long> avisadosRecientemente = new HashSet<>(recordatorioRepository.findApoderadoIdsWithRecentReminder(
                pendientes.keySet(), AppClock.now().minusDays(DIAS_MINIMOS_ENTRE_AVISOS)));
        return pendientes.entrySet().stream()
                .filter(entrada -> !avisadosRecientemente.contains(entrada.getKey()))
                .filter(entrada -> ownerContactPolicy.puedeRecibirCorreo(destinatarios.get(entrada.getKey()).getUser()))
                .map(entrada -> paraElPropietario(destinatarios.get(entrada.getKey()), entrada.getValue()))
                .toList();
    }

    private Set<ClaveDeAviso> clavesYaRegistradas(List<ControlPreventivo> candidatos) {
        if (candidatos.isEmpty()) return Set.of();
        return recordatorioRepository.findExistingKeys(candidatos.stream().map(ControlPreventivo::getId).toList())
                .stream()
                .map(clave -> new ClaveDeAviso(clave.getControlId(), clave.getTipoAviso(), clave.getFechaProgramada(),
                        clave.getApoderadoId()))
                .collect(Collectors.toSet());
    }

    private Set<ClaveDeConsentimiento> autorizadosARecibirlos(Map<Long, List<Apoderado>> destinatariosPorMascota) {
        Set<ClaveDeConsentimiento> autorizados = new HashSet<>();
        destinatariosPorMascota.values().stream().flatMap(List::stream)
                .filter(apoderado -> apoderado.getCompany() != null && apoderado.getUser() != null)
                .collect(Collectors.groupingBy(apoderado -> apoderado.getCompany().getId(),
                        Collectors.mapping(apoderado -> apoderado.getUser().getId(), Collectors.toSet())))
                .forEach((companyId, usuarioIds) -> {
                    boolean gestionado = complianceService.gestionadoPorSoftvet(companyId,
                            ControlCumplimientoClinica.RECORDATORIOS_CORREO);
                    consentimientoDatosService.usuariosQueAutorizaron(gestionado ? usuarioIds : Set.of(), companyId,
                                    FinalidadDatos.RECORDATORIOS_PREVENTIVOS)
                            .forEach(usuarioId -> autorizados.add(new ClaveDeConsentimiento(usuarioId, companyId)));
                });
        return autorizados;
    }

    private boolean puedeRecibirlo(Apoderado destinatario, Set<ClaveDeConsentimiento> autorizados) {
        return destinatario.getUser() != null && destinatario.getUser().isActivo()
                && destinatario.getCompany() != null
                && autorizados.contains(new ClaveDeConsentimiento(destinatario.getUser().getId(),
                destinatario.getCompany().getId()));
    }

    private AvisosParaPropietario paraElPropietario(Apoderado destinatario, List<AvisoPendiente> pendientes) {
        Usuario usuario = destinatario.getUser();
        Company company = usuario.getCompany();
        List<Aviso> avisos = pendientes.stream()
                .sorted(Comparator.comparing((AvisoPendiente pendiente) -> prioridad(pendiente.tipoAviso()))
                        .thenComparing(pendiente -> pendiente.control().getFechaRecomendada()))
                .map(pendiente -> new Aviso(pendiente.control().getId(),
                        pendiente.control().getMascota().getNombreCompleto(), pendiente.control().getNombreControl(),
                        pendiente.control().getTipo(), pendiente.control().getFechaRecomendada(), pendiente.tipoAviso()))
                .toList();
        return new AvisosParaPropietario(destinatario.getId(), usuario.getEmail(),
                (usuario.getNombre() + " " + usuario.getApellido()).trim(), clinicaDe(company), avisos);
    }

    private AvisosParaPropietario.Clinica clinicaDe(Company company) {
        if (company == null) return null;
        return new AvisosParaPropietario.Clinica(company.getName(), company.getLogoUrl(), company.getEmail(),
                company.getPhone(), company.getAddress());
    }

    private TipoAvisoRecordatorio tipoDeAviso(ControlPreventivo control, LocalDate hoy) {
        LocalDate fecha = control.getFechaRecomendada();
        if (hoy.isAfter(fecha)) return TipoAvisoRecordatorio.ATRASADO;
        if (hoy.isEqual(fecha)) return TipoAvisoRecordatorio.PENDIENTE;
        if (!hoy.isBefore(fecha.minusDays(DIAS_DE_ANTICIPACION))) return TipoAvisoRecordatorio.PROXIMO;
        return null;
    }

    private void actualizarEstado(ControlPreventivo control, LocalDate hoy) {
        LocalDate fecha = control.getFechaRecomendada();
        EstadoControlPreventivo nuevo = fecha.isBefore(hoy) ? EstadoControlPreventivo.ATRASADO
                : fecha.isEqual(hoy) ? EstadoControlPreventivo.PENDIENTE
                : !fecha.isAfter(hoy.plusDays(DIAS_DE_ANTICIPACION)) ? EstadoControlPreventivo.PROXIMO
                : EstadoControlPreventivo.PROGRAMADO;
        if (control.getEstado() != nuevo) {
            control.setEstado(nuevo);
            control.setEstadoModificadoPor("SYSTEM");
            control.setFechaModificacionEstado(AppClock.now());
            control.setUpdatedBy("SYSTEM");
        }
    }

    private int prioridad(TipoAvisoRecordatorio tipo) {
        return switch (tipo) {
            case ATRASADO -> 0;
            case PENDIENTE -> 1;
            case PROXIMO -> 2;
        };
    }

    private record AvisoPendiente(ControlPreventivo control, TipoAvisoRecordatorio tipoAviso) { }

    private record ClaveDeAviso(Long controlId, TipoAvisoRecordatorio tipoAviso, LocalDate fechaProgramada,
                                Long apoderadoId) { }

    private record ClaveDeConsentimiento(Integer usuarioId, Integer companyId) { }
}
