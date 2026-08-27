package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoDesparasitanteResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;
import veterinaria.vargasvet.dto.response.CartillaDetalleResponse;
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
    private final TipoDesparasitanteRepository tipoDesparasitanteRepository;
    private final ControlPreventivoRepository controlRepository;
    private final RegistroVacunaRepository vacunaRepository;
    private final RegistroDesparasitacionRepository desparasitacionRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<TipoVacunaResponse> listarTiposVacuna(Long mascotaId) {
        return listarTiposVacuna(obtenerMascotaAutorizada(mascotaId));
    }

    private List<TipoVacunaResponse> listarTiposVacuna(Mascota mascota) {
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
        tipo.setPrecio(request.getPrecio());
        tipo.setLote(normalizar(request.getLote()));
        tipo.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        tipo.setDosis(request.getDosis());
        tipo.setUnidadDosis(normalizar(request.getUnidadDosis()));
        tipo.setViaAdministracion(normalizar(request.getViaAdministracion()));
        tipo.setCreatedBy(actor);
        tipo.setUpdatedBy(actor);
        return toTipoResponse(tipoVacunaRepository.save(tipo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoDesparasitanteResponse> listarTiposDesparasitante(Long mascotaId) {
        return listarTiposDesparasitante(obtenerMascotaAutorizada(mascotaId));
    }

    private List<TipoDesparasitanteResponse> listarTiposDesparasitante(Mascota mascota) {
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        return tipoDesparasitanteRepository.findByCompanyIdAndEspecieAndActivoTrueOrderByNombre(companyId, mascota.getEspecie())
                .stream().map(this::toDesparasitanteResponse).toList();
    }

    @Override
    @Transactional
    public TipoDesparasitanteResponse crearTipoDesparasitante(TipoDesparasitanteRequest request) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una veterinaria");
        if (tipoDesparasitanteRepository.existsByCompanyIdAndNombreIgnoreCaseAndEspecie(
                companyId, request.getNombre().trim(), request.getEspecie())) {
            throw new IllegalArgumentException("Ya existe un desparasitante con ese nombre para la especie seleccionada");
        }
        Company company = new Company();
        company.setId(companyId);
        String actor = actor();
        TipoDesparasitante tipo = new TipoDesparasitante();
        tipo.setCompany(company);
        tipo.setNombre(request.getNombre().trim());
        tipo.setEspecie(request.getEspecie());
        tipo.setPeriodicidadMesesSugerida(request.getPeriodicidadMesesSugerida());
        tipo.setPrecio(request.getPrecio());
        tipo.setLote(normalizar(request.getLote()));
        tipo.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        tipo.setDosis(request.getDosis());
        tipo.setUnidadDosis(normalizar(request.getUnidadDosis()));
        tipo.setViaAdministracion(normalizar(request.getViaAdministracion()));
        tipo.setCreatedBy(actor);
        tipo.setUpdatedBy(actor);
        return toDesparasitanteResponse(tipoDesparasitanteRepository.save(tipo));
    }

    @Override
    @Transactional
    public List<ControlPreventivoResponse> listarControles(Long mascotaId) {
        obtenerMascotaAutorizada(mascotaId);
        return listarControlesAutorizados(mascotaId);
    }

    private List<ControlPreventivoResponse> listarControlesAutorizados(Long mascotaId) {
        List<ControlPreventivo> controles = controlRepository.findByMascotaIdOrderByFechaRecomendadaDesc(mascotaId);
        controles.forEach(this::actualizarEstadoPorFecha);
        return controles.stream().map(this::toControlResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AplicacionPreventivaResponse> listarAplicaciones(Long mascotaId) {
        obtenerMascotaAutorizada(mascotaId);
        return listarAplicacionesAutorizadas(mascotaId);
    }

    private List<AplicacionPreventivaResponse> listarAplicacionesAutorizadas(Long mascotaId) {
        List<AplicacionPreventivaResponse> resultado = new ArrayList<>();
        vacunaRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(mascotaId).forEach(v -> resultado.add(
                AplicacionPreventivaResponse.builder().id(v.getId()).tipo(TipoControlPreventivo.VACUNACION)
                        .nombreControl(v.getNombreVacuna()).fechaAplicacion(v.getFechaAplicacion())
                        .periodicidadMeses(v.getPeriodicidadMeses()).fechaProximaAplicacion(v.getFechaProximaDosis())
                        .intervaloCantidad(v.getIntervaloCantidad()).intervaloUnidad(v.getIntervaloUnidad())
                        .veterinarioNombre(nombreVeterinario(v.getVeterinario()))
                        .lote(v.getLote()).fechaVencimientoProducto(v.getFechaVencimientoProducto())
                        .dosis(v.getDosis()).unidadDosis(v.getUnidadDosis())
                        .viaAdministracion(v.getViaAdministracion()).sitioAplicacion(v.getSitioAplicacion())
                        .pesoKg(v.getPesoKg()).observaciones(v.getObservaciones()).build()));
        desparasitacionRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(mascotaId).forEach(d -> resultado.add(
                AplicacionPreventivaResponse.builder().id(d.getId()).tipo(TipoControlPreventivo.DESPARASITACION)
                        .nombreControl(d.getProducto()).fechaAplicacion(d.getFechaAplicacion())
                        .periodicidadMeses(d.getPeriodicidadMeses()).fechaProximaAplicacion(d.getFechaProximaAplicacion())
                        .intervaloCantidad(d.getIntervaloCantidad()).intervaloUnidad(d.getIntervaloUnidad())
                        .veterinarioNombre(nombreVeterinario(d.getVeterinario()))
                        .lote(d.getLote()).fechaVencimientoProducto(d.getFechaVencimientoProducto())
                        .dosis(d.getDosis()).unidadDosis(d.getUnidadDosis())
                        .viaAdministracion(d.getViaAdministracion()).sitioAplicacion(d.getSitioAplicacion())
                        .pesoKg(d.getPesoKg()).observaciones(d.getObservaciones()).build()));
        resultado.sort((a, b) -> b.getFechaAplicacion().compareTo(a.getFechaAplicacion()));
        return resultado;
    }

    @Override
    @Transactional
    public CartillaDetalleResponse obtenerDetalleCartilla(Long mascotaId) {
        Mascota mascota = obtenerMascotaAutorizada(mascotaId);
        return CartillaDetalleResponse.builder()
                .vacunas(listarTiposVacuna(mascota))
                .desparasitantes(listarTiposDesparasitante(mascota))
                .controles(listarControlesAutorizados(mascotaId))
                .aplicaciones(listarAplicacionesAutorizadas(mascotaId))
                .build();
    }

    @Override
    @Transactional
    public ControlPreventivoResponse programar(Long mascotaId, ControlPreventivoRequest request) {
        Mascota mascota = obtenerMascotaAutorizadaParaActualizar(mascotaId);
        TipoVacuna tipoVacuna = validarTipoVacuna(request.getTipo(), request.getTipoVacunaId(), mascota);
        String nombre = tipoVacuna != null ? tipoVacuna.getNombre() : normalizarNombre(request.getNombreControl());
        validarControlDuplicado(mascota.getId(), request.getTipo(), tipoVacuna, nombre, request.getFechaRecomendada(), null);
        ControlPreventivo control = nuevoControl(mascota, request.getTipo(), tipoVacuna, nombre, request.getFechaRecomendada(), actor());
        auditLogService.log("PROGRAMAR_CONTROL_PREVENTIVO", "Historias Clinicas",
                "Se programo " + nombre + " para la mascota " + mascota.getNombreCompleto());
        return toControlResponse(controlRepository.save(control));
    }

    @Override
    @Transactional
    public ControlPreventivoResponse reprogramar(Long controlId, ReprogramarControlPreventivoRequest request) {
        ControlPreventivo control = obtenerControlAbiertoAutorizado(controlId);
        validarControlDuplicado(control.getMascota().getId(), control.getTipo(), control.getTipoVacuna(),
                control.getNombreControl(), request.getFechaRecomendada(), control.getId());
        control.setFechaRecomendada(request.getFechaRecomendada());
        control.setCitaSuspende(null);
        cambiarEstado(control, EstadoControlPreventivo.PROGRAMADO, actor());
        actualizarEstadoPorFecha(control);
        auditLogService.log("REPROGRAMAR_CONTROL_PREVENTIVO", "Historias Clinicas",
                "Se reprogramo " + control.getNombreControl() + " para " + request.getFechaRecomendada());
        return toControlResponse(controlRepository.save(control));
    }

    @Override
    @Transactional
    public ControlPreventivoResponse cancelar(Long controlId) {
        ControlPreventivo control = obtenerControlAbiertoAutorizado(controlId);
        control.setCitaSuspende(null);
        cambiarEstado(control, EstadoControlPreventivo.CANCELADO, actor());
        auditLogService.log("CANCELAR_CONTROL_PREVENTIVO", "Historias Clinicas",
                "Se cancelo " + control.getNombreControl() + " para la mascota " + control.getMascota().getNombreCompleto());
        return toControlResponse(controlRepository.save(control));
    }

    @Override
    @Transactional
    public ControlPreventivoResponse registrarVacunacion(Long consultaId, RegistroVacunacionRequest request) {
        Consulta consulta = obtenerConsultaAutorizada(consultaId);
        Mascota mascota = obtenerMascotaAutorizadaParaActualizar(consulta.getHistoriaClinica().getMascota().getId());
        TipoVacuna tipoVacuna = validarTipoVacuna(TipoControlPreventivo.VACUNACION, request.getTipoVacunaId(), mascota);
        LocalDate proxima = calcularProximaFecha(request.getFechaAplicacion(), request.getFechaProximaDosis(),
                request.getPeriodicidadMeses());
        validarFechas(mascota, request.getFechaAplicacion(), proxima);
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

        ControlPreventivo siguiente = encontrarControlDuplicado(mascota.getId(), TipoControlPreventivo.VACUNACION,
                tipoVacuna, tipoVacuna.getNombre(), proxima);
        if (siguiente == null) {
            siguiente = controlRepository.save(nuevoControl(mascota, TipoControlPreventivo.VACUNACION,
                    tipoVacuna, tipoVacuna.getNombre(), proxima, actor));
        }
        auditLogService.log("REGISTRAR_VACUNACION", "Historias Clinicas",
                "Se registro la vacuna " + tipoVacuna.getNombre() + " para " + mascota.getNombreCompleto());
        return toControlResponse(siguiente);
    }

    @Override
    @Transactional
    public ControlPreventivoResponse registrarDesparasitacion(Long consultaId, RegistroDesparasitacionRequest request) {
        Consulta consulta = obtenerConsultaAutorizada(consultaId);
        Mascota mascota = obtenerMascotaAutorizadaParaActualizar(consulta.getHistoriaClinica().getMascota().getId());
        LocalDate proxima = calcularProximaFecha(request.getFechaAplicacion(), request.getFechaProximaAplicacion(),
                request.getPeriodicidadMeses());
        validarFechas(mascota, request.getFechaAplicacion(), proxima);
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

        ControlPreventivo siguiente = encontrarControlDuplicado(mascota.getId(), TipoControlPreventivo.DESPARASITACION,
                null, request.getProducto().trim(), proxima);
        if (siguiente == null) {
            siguiente = controlRepository.save(nuevoControl(mascota, TipoControlPreventivo.DESPARASITACION,
                    null, request.getProducto().trim(), proxima, actor));
        }
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

    private Mascota obtenerMascotaAutorizadaParaActualizar(Long id) {
        Mascota mascota = mascotaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        validarCompany(mascota);
        return mascota;
    }

    private Consulta obtenerConsultaAutorizada(Long id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada"));
        validarCompany(consulta.getHistoriaClinica().getMascota());
        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("No se pueden registrar aplicaciones en una consulta cerrada");
        }
        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isAdmin()
                && !SecurityUtils.hasAuthority("CLINICAL_RECORD_MANAGE")) {
            Integer usuarioActual = SecurityUtils.getCurrentUserId();
            boolean esVeterinarioAsignado = consulta.getVeterinario() != null
                    && consulta.getVeterinario().getUser() != null
                    && consulta.getVeterinario().getUser().getId().equals(usuarioActual);
            if (!esVeterinarioAsignado) {
                throw new IllegalArgumentException("Solo el veterinario asignado puede registrar la aplicacion");
            }
        }
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

    private void validarFechas(Mascota mascota, LocalDate aplicacion, LocalDate proxima) {
        if (aplicacion.isAfter(AppClock.today())) throw new IllegalArgumentException("La fecha de aplicacion no puede ser futura");
        if (mascota.getFechaNacimiento() != null && aplicacion.isBefore(mascota.getFechaNacimiento())) {
            throw new IllegalArgumentException("La fecha de aplicacion no puede ser anterior al nacimiento de la mascota");
        }
        if (proxima != null && !proxima.isAfter(aplicacion)) {
            throw new IllegalArgumentException("La proxima aplicacion debe ser posterior");
        }
    }

    private LocalDate calcularProximaFecha(LocalDate aplicacion, LocalDate fechaExplicita,
                                           Integer periodicidadMeses) {
        if (fechaExplicita != null) return fechaExplicita;
        if (periodicidadMeses == null) {
            throw new IllegalArgumentException("Indique la proxima fecha o una periodicidad");
        }
        return aplicacion.plusMonths(periodicidadMeses);
    }

    private ControlPreventivo obtenerControlAbiertoAutorizado(Long id) {
        ControlPreventivo control = controlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control preventivo no encontrado"));
        validarCompany(control.getMascota());
        if (!ESTADOS_ABIERTOS.contains(control.getEstado())) {
            throw new IllegalArgumentException("El control ya se encuentra cerrado");
        }
        return control;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TipoVacunaResponse> listarTiposVacunaPorCompany(Integer companyId, Pageable pageable) {
        return tipoVacunaRepository.findByCompanyId(resolveCatalogCompanyId(companyId), pageable).map(this::toTipoResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TipoDesparasitanteResponse> listarTiposDesparasitantePorCompany(Integer companyId, Pageable pageable) {
        return tipoDesparasitanteRepository.findByCompanyId(resolveCatalogCompanyId(companyId), pageable).map(this::toDesparasitanteResponse);
    }

    @Override
    @Transactional
    public TipoVacunaResponse actualizarTipoVacuna(Long id, TipoVacunaRequest request) {
        TipoVacuna tipo = tipoVacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacuna no encontrada"));
        validarCatalogCompany(tipo.getCompany(), "Vacuna no encontrada");
        tipo.setNombre(request.getNombre().trim());
        tipo.setEspecie(request.getEspecie());
        tipo.setPeriodicidadMesesSugerida(request.getPeriodicidadMesesSugerida());
        tipo.setPrecio(request.getPrecio());
        tipo.setLote(normalizar(request.getLote()));
        tipo.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        tipo.setDosis(request.getDosis());
        tipo.setUnidadDosis(normalizar(request.getUnidadDosis()));
        tipo.setViaAdministracion(normalizar(request.getViaAdministracion()));
        tipo.setUpdatedBy(actor());
        return toTipoResponse(tipoVacunaRepository.save(tipo));
    }

    @Override
    @Transactional
    public TipoDesparasitanteResponse actualizarTipoDesparasitante(Long id, TipoDesparasitanteRequest request) {
        TipoDesparasitante tipo = tipoDesparasitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desparasitante no encontrado"));
        validarCatalogCompany(tipo.getCompany(), "Desparasitante no encontrado");
        tipo.setNombre(request.getNombre().trim());
        tipo.setEspecie(request.getEspecie());
        tipo.setPeriodicidadMesesSugerida(request.getPeriodicidadMesesSugerida());
        tipo.setPrecio(request.getPrecio());
        tipo.setLote(normalizar(request.getLote()));
        tipo.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        tipo.setDosis(request.getDosis());
        tipo.setUnidadDosis(normalizar(request.getUnidadDosis()));
        tipo.setViaAdministracion(normalizar(request.getViaAdministracion()));
        tipo.setUpdatedBy(actor());
        return toDesparasitanteResponse(tipoDesparasitanteRepository.save(tipo));
    }

    @Override
    @Transactional
    public void cambiarEstadoTipoVacuna(Long id, boolean activo) {
        TipoVacuna tipo = tipoVacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacuna no encontrada"));
        validarCatalogCompany(tipo.getCompany(), "Vacuna no encontrada");
        tipo.setActivo(activo);
        tipo.setUpdatedBy(actor());
        tipoVacunaRepository.save(tipo);
    }

    @Override
    @Transactional
    public void cambiarEstadoTipoDesparasitante(Long id, boolean activo) {
        TipoDesparasitante tipo = tipoDesparasitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desparasitante no encontrado"));
        validarCatalogCompany(tipo.getCompany(), "Desparasitante no encontrado");
        tipo.setActivo(activo);
        tipo.setUpdatedBy(actor());
        tipoDesparasitanteRepository.save(tipo);
    }

    @Override
    @Transactional
    public void eliminarTipoVacuna(Long id) {
        TipoVacuna tipo = tipoVacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacuna no encontrada"));
        validarCatalogCompany(tipo.getCompany(), "Vacuna no encontrada");
        tipo.setActivo(false);
        tipo.setUpdatedBy(actor());
        tipoVacunaRepository.save(tipo);
    }

    @Override
    @Transactional
    public void eliminarTipoDesparasitante(Long id) {
        TipoDesparasitante tipo = tipoDesparasitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desparasitante no encontrado"));
        validarCatalogCompany(tipo.getCompany(), "Desparasitante no encontrado");
        tipo.setActivo(false);
        tipo.setUpdatedBy(actor());
        tipoDesparasitanteRepository.save(tipo);
    }

    private void validarControlDuplicado(Long mascotaId, TipoControlPreventivo tipo, TipoVacuna vacuna,
                                         String nombre, LocalDate fecha, Long controlExcluido) {
        boolean duplicado = vacuna != null
                ? controlRepository.existsByMascotaIdAndTipoVacunaIdAndFechaRecomendadaAndEstadoIn(
                        mascotaId, vacuna.getId(), fecha, ESTADOS_ABIERTOS)
                : controlRepository.existsByMascotaIdAndTipoAndNombreControlIgnoreCaseAndFechaRecomendadaAndEstadoIn(
                        mascotaId, tipo, nombre, fecha, ESTADOS_ABIERTOS);
        if (duplicado && controlExcluido == null) {
            throw new IllegalArgumentException("Ya existe un control abierto igual para la misma fecha");
        }
        if (duplicado && controlExcluido != null) {
            boolean esElMismo = controlRepository.findById(controlExcluido)
                    .map(c -> c.getFechaRecomendada().equals(fecha)).orElse(false);
            if (!esElMismo) throw new IllegalArgumentException("Ya existe un control abierto igual para la misma fecha");
        }
    }

    private ControlPreventivo encontrarControlDuplicado(Long mascotaId, TipoControlPreventivo tipo,
                                                        TipoVacuna vacuna, String nombre, LocalDate fecha) {
        return controlRepository.findByMascotaIdOrderByFechaRecomendadaDesc(mascotaId).stream()
                .filter(c -> ESTADOS_ABIERTOS.contains(c.getEstado()))
                .filter(c -> c.getTipo() == tipo && c.getFechaRecomendada().equals(fecha))
                .filter(c -> vacuna != null
                        ? c.getTipoVacuna() != null && vacuna.getId().equals(c.getTipoVacuna().getId())
                        : c.getNombreControl().equalsIgnoreCase(nombre))
                .findFirst().orElse(null);
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Debe indicar el control preventivo");
        return nombre.trim();
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
                .periodicidadMesesSugerida(v.getPeriodicidadMesesSugerida()).precio(v.getPrecio())
                .lote(v.getLote()).fechaVencimientoProducto(v.getFechaVencimientoProducto())
                .dosis(v.getDosis()).unidadDosis(v.getUnidadDosis()).viaAdministracion(v.getViaAdministracion())
                .activo(v.getActivo()).build();
    }

    private TipoDesparasitanteResponse toDesparasitanteResponse(TipoDesparasitante d) {
        return TipoDesparasitanteResponse.builder().id(d.getId()).nombre(d.getNombre()).especie(d.getEspecie())
                .periodicidadMesesSugerida(d.getPeriodicidadMesesSugerida()).precio(d.getPrecio())
                .lote(d.getLote()).fechaVencimientoProducto(d.getFechaVencimientoProducto())
                .dosis(d.getDosis()).unidadDosis(d.getUnidadDosis()).viaAdministracion(d.getViaAdministracion())
                .activo(d.getActivo()).build();
    }

    private Integer resolveCatalogCompanyId(Integer requestedCompanyId) {
        if (SecurityUtils.isSuperAdmin()) return requestedCompanyId;
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new org.springframework.security.access.AccessDeniedException("El usuario no tiene una empresa asignada");
        }
        return currentCompanyId;
    }

    private void validarCatalogCompany(Company company, String notFoundMessage) {
        if (SecurityUtils.isSuperAdmin()) return;
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (currentCompanyId == null || company == null || !currentCompanyId.equals(company.getId())) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ControlPreventivoResponse toControlResponse(ControlPreventivo c) {
        return ControlPreventivoResponse.builder().id(c.getId()).mascotaId(c.getMascota().getId()).tipo(c.getTipo())
                .tipoVacunaId(c.getTipoVacuna() == null ? null : c.getTipoVacuna().getId())
                .nombreControl(c.getNombreControl()).fechaRecomendada(c.getFechaRecomendada()).estado(c.getEstado())
                .citaSuspendeId(c.getCitaSuspende() == null ? null : c.getCitaSuspende().getId()).build();
    }
}
