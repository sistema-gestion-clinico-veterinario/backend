package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.ControlPreventivoService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlPreventivoServiceImpl implements ControlPreventivoService {
    private static final EnumSet<EstadoControlPreventivo> ESTADOS_ABIERTOS = EnumSet.of(
            EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
            EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO,
            EstadoControlPreventivo.SUSPENDIDO_POR_CITA);

    private final TipoVacunaRepository tipoVacunaRepository;
    private final ControlPreventivoRepository controlRepository;
    private final RegistroVacunaRepository vacunaRepository;
    private final RegistroDesparasitacionRepository desparasitacionRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<TipoVacunaResponse> listarTiposVacuna(Long mascotaId) {
        Mascota mascota = obtenerMascotaAutorizada(mascotaId);
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        return tipoVacunaRepository.findByCompanyIdAndEspecieAndActivoTrueOrderByNombre(companyId, mascota.getEspecie())
                .stream().map(this::toTipoResponse).toList();
    }

    @Override
    @Transactional
    public TipoVacunaResponse crearTipoVacuna(TipoVacunaRequest request) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una veterinaria");
        if (tipoVacunaRepository.existsByCompanyIdAndNombreIgnoreCaseAndEspecie(companyId, request.getNombre().trim(), request.getEspecie())) {
            throw new IllegalArgumentException("Ya existe una vacuna con ese nombre para la especie seleccionada");
        }
        Company company = new Company();
        company.setId(companyId);
        String actor = actor();
        TipoVacuna tipo = new TipoVacuna();
        tipo.setCompany(company);
        tipo.setNombre(request.getNombre().trim());
        tipo.setEspecie(request.getEspecie());
        tipo.setPeriodicidadMesesSugerida(request.getPeriodicidadMesesSugerida());
        tipo.setCreatedBy(actor);
        tipo.setUpdatedBy(actor);
        return toTipoResponse(tipoVacunaRepository.save(tipo));
    }

    @Override
    @Transactional
    public List<ControlPreventivoResponse> listarControles(Long mascotaId) {
        obtenerMascotaAutorizada(mascotaId);
        List<ControlPreventivo> controles = controlRepository.findByMascotaIdOrderByFechaRecomendadaDesc(mascotaId);
        controles.forEach(this::actualizarEstadoPorFecha);
        return controles.stream().map(this::toControlResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AplicacionPreventivaResponse> listarAplicaciones(Long mascotaId) {
        obtenerMascotaAutorizada(mascotaId);
        List<AplicacionPreventivaResponse> resultado = new ArrayList<>();
        vacunaRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(mascotaId).forEach(v -> resultado.add(
                AplicacionPreventivaResponse.builder().id(v.getId()).tipo(TipoControlPreventivo.VACUNACION)
                        .nombreControl(v.getNombreVacuna()).fechaAplicacion(v.getFechaAplicacion())
                        .periodicidadMeses(v.getPeriodicidadMeses()).fechaProximaAplicacion(v.getFechaProximaDosis())
                        .veterinarioNombre(nombreVeterinario(v.getVeterinario())).build()));
        desparasitacionRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(mascotaId).forEach(d -> resultado.add(
                AplicacionPreventivaResponse.builder().id(d.getId()).tipo(TipoControlPreventivo.DESPARASITACION)
                        .nombreControl(d.getProducto()).fechaAplicacion(d.getFechaAplicacion())
                        .periodicidadMeses(d.getPeriodicidadMeses()).fechaProximaAplicacion(d.getFechaProximaAplicacion())
                        .veterinarioNombre(nombreVeterinario(d.getVeterinario())).build()));
        resultado.sort((a, b) -> b.getFechaAplicacion().compareTo(a.getFechaAplicacion()));
        return resultado;
    }

    @Override
    @Transactional
    public ControlPreventivoResponse programar(Long mascotaId, ControlPreventivoRequest request) {
        Mascota mascota = obtenerMascotaAutorizada(mascotaId);
        TipoVacuna tipoVacuna = validarTipoVacuna(request.getTipo(), request.getTipoVacunaId(), mascota);
        String nombre = tipoVacuna != null ? tipoVacuna.getNombre() : normalizarNombre(request.getNombreControl());
        ControlPreventivo control = nuevoControl(mascota, request.getTipo(), tipoVacuna, nombre, request.getFechaRecomendada(), actor());
        auditLogService.log("PROGRAMAR_CONTROL_PREVENTIVO", "Historias Clinicas",
                "Se programo " + nombre + " para la mascota " + mascota.getNombreCompleto());
        return toControlResponse(controlRepository.save(control));
    }

    @Override
    @Transactional
    public ControlPreventivoResponse registrarVacunacion(Long consultaId, RegistroVacunacionRequest request) {
        Consulta consulta = obtenerConsultaAutorizada(consultaId);
        Mascota mascota = consulta.getHistoriaClinica().getMascota();
        TipoVacuna tipoVacuna = validarTipoVacuna(TipoControlPreventivo.VACUNACION, request.getTipoVacunaId(), mascota);
        LocalDate proxima = request.getFechaProximaDosis() != null ? request.getFechaProximaDosis()
                : request.getFechaAplicacion().plusMonths(request.getPeriodicidadMeses());
        validarFechas(request.getFechaAplicacion(), proxima);
        String actor = actor();
        ControlPreventivo actual = completarControl(request.getControlPreventivoId(), mascota,
                TipoControlPreventivo.VACUNACION, tipoVacuna.getId(), actor);

        RegistroVacuna registro = new RegistroVacuna();
        registro.setHistoriaClinica(consulta.getHistoriaClinica());
        registro.setConsulta(consulta);
        registro.setVeterinario(consulta.getVeterinario());
        registro.setControlPreventivo(actual);
        registro.setTipoVacuna(tipoVacuna);
        registro.setNombreVacuna(tipoVacuna.getNombre());
        registro.setFechaAplicacion(request.getFechaAplicacion());
        registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
        registro.setFechaProximaDosis(proxima);
        registro.setCreatedBy(actor);
        registro.setUpdatedBy(actor);
        vacunaRepository.save(registro);

        ControlPreventivo siguiente = nuevoControl(mascota, TipoControlPreventivo.VACUNACION,
                tipoVacuna, tipoVacuna.getNombre(), proxima, actor);
        siguiente = controlRepository.save(siguiente);
        consulta.setVacunacionAlDia(!hayPendientes(mascota.getId(), TipoControlPreventivo.VACUNACION));
        consultaRepository.save(consulta);
        auditLogService.log("REGISTRAR_VACUNACION", "Historias Clinicas",
                "Se registro la vacuna " + tipoVacuna.getNombre() + " para " + mascota.getNombreCompleto());
        return toControlResponse(siguiente);
    }

    @Override
    @Transactional
    public ControlPreventivoResponse registrarDesparasitacion(Long consultaId, RegistroDesparasitacionRequest request) {
        Consulta consulta = obtenerConsultaAutorizada(consultaId);
        Mascota mascota = consulta.getHistoriaClinica().getMascota();
        LocalDate proxima = request.getFechaProximaAplicacion() != null ? request.getFechaProximaAplicacion()
                : request.getFechaAplicacion().plusMonths(request.getPeriodicidadMeses());
        validarFechas(request.getFechaAplicacion(), proxima);
        String actor = actor();
        ControlPreventivo actual = completarControl(request.getControlPreventivoId(), mascota,
                TipoControlPreventivo.DESPARASITACION, null, actor);

        RegistroDesparasitacion registro = new RegistroDesparasitacion();
        registro.setHistoriaClinica(consulta.getHistoriaClinica());
        registro.setConsulta(consulta);
        registro.setVeterinario(consulta.getVeterinario());
        registro.setControlPreventivo(actual);
        registro.setProducto(request.getProducto().trim());
        registro.setFechaAplicacion(request.getFechaAplicacion());
        registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
        registro.setFechaProximaAplicacion(proxima);
        registro.setCreatedBy(actor);
        registro.setUpdatedBy(actor);
        desparasitacionRepository.save(registro);

        ControlPreventivo siguiente = nuevoControl(mascota, TipoControlPreventivo.DESPARASITACION,
                null, request.getProducto().trim(), proxima, actor);
        siguiente = controlRepository.save(siguiente);
        consulta.setDesparasitacionAlDia(!hayPendientes(mascota.getId(), TipoControlPreventivo.DESPARASITACION));
        consultaRepository.save(consulta);
        auditLogService.log("REGISTRAR_DESPARASITACION", "Historias Clinicas",
                "Se registro la desparasitacion de " + mascota.getNombreCompleto());
        return toControlResponse(siguiente);
    }

    private ControlPreventivo completarControl(Long id, Mascota mascota, TipoControlPreventivo tipo,
                                                Long tipoVacunaId, String actor) {
        if (id == null) return null;
        ControlPreventivo control = controlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control preventivo no encontrado"));
        if (!control.getMascota().getId().equals(mascota.getId()) || control.getTipo() != tipo) {
            throw new IllegalArgumentException("El control no corresponde a la mascota o al tipo de aplicacion");
        }
        if (tipoVacunaId != null && (control.getTipoVacuna() == null || !tipoVacunaId.equals(control.getTipoVacuna().getId()))) {
            throw new IllegalArgumentException("La vacuna aplicada no corresponde al control seleccionado");
        }
        if (!ESTADOS_ABIERTOS.contains(control.getEstado())) {
            throw new IllegalArgumentException("El control seleccionado ya fue cerrado");
        }
        cambiarEstado(control, EstadoControlPreventivo.APLICADO, actor);
        control.setCitaSuspende(null);
        return controlRepository.save(control);
    }

    private ControlPreventivo nuevoControl(Mascota mascota, TipoControlPreventivo tipo, TipoVacuna tipoVacuna,
                                            String nombre, LocalDate fecha, String actor) {
        ControlPreventivo control = new ControlPreventivo();
        control.setMascota(mascota);
        control.setTipo(tipo);
        control.setTipoVacuna(tipoVacuna);
        control.setNombreControl(nombre);
        control.setFechaRecomendada(fecha);
        control.setCreatedBy(actor);
        control.setUpdatedBy(actor);
        control.setEstadoModificadoPor(actor);
        control.setFechaModificacionEstado(AppClock.now());
        actualizarEstadoPorFecha(control);
        return control;
    }

    private void actualizarEstadoPorFecha(ControlPreventivo control) {
        if (!EnumSet.of(EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
                EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO).contains(control.getEstado())) return;
        LocalDate hoy = AppClock.today();
        EstadoControlPreventivo nuevo = control.getFechaRecomendada().isBefore(hoy)
                ? EstadoControlPreventivo.ATRASADO
                : control.getFechaRecomendada().isEqual(hoy) ? EstadoControlPreventivo.PENDIENTE
                : !control.getFechaRecomendada().isAfter(hoy.plusDays(7)) ? EstadoControlPreventivo.PROXIMO
                : EstadoControlPreventivo.PROGRAMADO;
        if (control.getEstado() != nuevo) cambiarEstado(control, nuevo, "SYSTEM");
    }

    private void cambiarEstado(ControlPreventivo control, EstadoControlPreventivo estado, String actor) {
        control.setEstado(estado);
        control.setEstadoModificadoPor(actor);
        control.setFechaModificacionEstado(AppClock.now());
        control.setUpdatedBy(actor);
    }

    private Mascota obtenerMascotaAutorizada(Long id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        validarCompany(mascota);
        return mascota;
    }

    private Consulta obtenerConsultaAutorizada(Long id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada"));
        validarCompany(consulta.getHistoriaClinica().getMascota());
        return consulta;
    }

    private void validarCompany(Mascota mascota) {
        if (SecurityUtils.isSuperAdmin()) return;
        Integer actual = SecurityUtils.getCurrentCompanyId();
        Integer mascotaCompany = mascota.getApoderado().getUser().getCompany().getId();
        if (actual == null || !actual.equals(mascotaCompany)) throw new IllegalArgumentException("No tiene acceso a esta mascota");
    }

    private TipoVacuna validarTipoVacuna(TipoControlPreventivo tipo, Long id, Mascota mascota) {
        if (tipo == TipoControlPreventivo.DESPARASITACION) {
            if (id != null) throw new IllegalArgumentException("La desparasitacion no utiliza tipo de vacuna");
            return null;
        }
        if (id == null) throw new IllegalArgumentException("Debe seleccionar la vacuna");
        TipoVacuna vacuna = tipoVacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de vacuna no encontrado"));
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (!vacuna.getCompany().getId().equals(companyId) || vacuna.getEspecie() != mascota.getEspecie() || !vacuna.getActivo()) {
            throw new IllegalArgumentException("La vacuna no corresponde a la especie o veterinaria de la mascota");
        }
        return vacuna;
    }

    private void validarFechas(LocalDate aplicacion, LocalDate proxima) {
        if (aplicacion.isAfter(AppClock.today())) throw new IllegalArgumentException("La fecha de aplicacion no puede ser futura");
        if (!proxima.isAfter(aplicacion)) throw new IllegalArgumentException("La proxima aplicacion debe ser posterior");
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Debe indicar el control preventivo");
        return nombre.trim();
    }

    private boolean hayPendientes(Long mascotaId, TipoControlPreventivo tipo) {
        return controlRepository.existsByMascotaIdAndTipoAndEstadoIn(mascotaId, tipo,
                EnumSet.of(EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO));
    }

    private String actor() {
        String email = SecurityUtils.getCurrentUserEmail();
        return email == null || email.isBlank() ? "SYSTEM" : email;
    }

    private String nombreVeterinario(Empleado empleado) {
        if (empleado == null || empleado.getUser() == null) return null;
        return (empleado.getUser().getNombre() + " " + empleado.getUser().getApellido()).trim();
    }

    private TipoVacunaResponse toTipoResponse(TipoVacuna v) {
        return TipoVacunaResponse.builder().id(v.getId()).nombre(v.getNombre()).especie(v.getEspecie())
                .periodicidadMesesSugerida(v.getPeriodicidadMesesSugerida()).build();
    }

    private ControlPreventivoResponse toControlResponse(ControlPreventivo c) {
        return ControlPreventivoResponse.builder().id(c.getId()).mascotaId(c.getMascota().getId()).tipo(c.getTipo())
                .tipoVacunaId(c.getTipoVacuna() == null ? null : c.getTipoVacuna().getId())
                .nombreControl(c.getNombreControl()).fechaRecomendada(c.getFechaRecomendada()).estado(c.getEstado())
                .citaSuspendeId(c.getCitaSuspende() == null ? null : c.getCitaSuspende().getId()).build();
    }
}
