package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlServicio;
import veterinaria.vargasvet.dto.request.CartillaAplicacionEditRequest;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.dto.response.MascotaCartillaResponse;
import veterinaria.vargasvet.mapper.MascotaCartillaMapper;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CartillaService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class CartillaServiceImpl implements CartillaService {

    private static final EnumSet<EstadoControlPreventivo> ESTADOS_ABIERTOS = EnumSet.of(
            EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
            EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO,
            EstadoControlPreventivo.SUSPENDIDO_POR_CITA);

    private final MascotaRepository mascotaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServiciosVeterinariosRepository serviciosRepository;
    private final TipoVacunaRepository tipoVacunaRepository;
    private final TipoDesparasitanteRepository tipoDesparasitanteRepository;
    private final RegistroVacunaRepository vacunaRepository;
    private final RegistroDesparasitacionRepository desparasitacionRepository;
    private final ControlPreventivoRepository controlRepository;
    private final CitaRepository citaRepository;
    private final AuditLogService auditLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MascotaCartillaMapper mascotaCartillaMapper;

    @Override
    @Transactional
    public CartillaAplicacionResponse registrarVacunacion(CartillaAplicacionRequest request) {
        if (request.getTipoVacunaId() == null) {
            throw new IllegalArgumentException("Debe seleccionar la vacuna");
        }
        return registrar(request, TipoControlPreventivo.VACUNACION);
    }

    @Override
    @Transactional
    public CartillaAplicacionResponse registrarDesparasitacion(CartillaAplicacionRequest request) {
        if (request.getTipoDesparasitanteId() == null) {
            throw new IllegalArgumentException("Debe seleccionar el desparasitante");
        }
        return registrar(request, TipoControlPreventivo.DESPARASITACION);
    }

    @Override
    public Page<MascotaCartillaResponse> listarMascotasConCartilla(Integer companyId, String nombre, EspecieMascota especie, Boolean activo, Pageable pageable) {
        Page<Mascota> mascotas = mascotaRepository.buscar(companyId, nombre, especie, null, activo, pageable);

        return mascotas.map(m -> {
            LocalDate fv = vacunaRepository
                    .findFirstByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(m.getId())
                    .map(RegistroVacuna::getFechaAplicacion)
                    .orElse(null);
            LocalDate fd = desparasitacionRepository
                    .findFirstByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(m.getId())
                    .map(RegistroDesparasitacion::getFechaAplicacion)
                    .orElse(null);

            LocalDate fechaUltima;
            if (fv != null && fd != null) {
                fechaUltima = fv.isAfter(fd) ? fv : fd;
            } else {
                fechaUltima = fv != null ? fv : fd;
            }

            return mascotaCartillaMapper.toResponse(m, fechaUltima);
        });
    }

    private CartillaAplicacionResponse registrar(CartillaAplicacionRequest request, TipoControlPreventivo tipo) {
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        validarCompany(mascota);

        Empleado empleado = resolverEmpleado(mascota);
        ServiciosVeterinarios servicio = resolverServicioPreventivo(request.getServicioId(), tipo, mascota);

        TipoVacuna tipoVacuna = null;
        TipoDesparasitante tipoDesparasitante = null;
        String nombre;
        if (tipo == TipoControlPreventivo.VACUNACION) {
            tipoVacuna = validarTipoVacuna(request.getTipoVacunaId(), mascota);
            nombre = tipoVacuna.getNombre();
            completarDesdeCatalogo(request, tipoVacuna);
        } else {
            tipoDesparasitante = validarTipoDesparasitante(request.getTipoDesparasitanteId(), mascota);
            nombre = tipoDesparasitante.getNombre();
            completarDesdeCatalogo(request, tipoDesparasitante);
        }

        validarDatosAplicacion(request);
        LocalDate aplicacion = request.getFechaAplicacion();
        LocalDate proxima = calcularProxima(aplicacion, request);
        validarFechas(mascota, aplicacion, proxima);

        BigDecimal total = tipo == TipoControlPreventivo.VACUNACION
                ? tipoVacuna.getPrecio() : tipoDesparasitante.getPrecio();

        String actor = actor();

        HistoriaClinica hc = historiaClinicaRepository.findByMascotaId(mascota.getId())
                .orElseGet(() -> {
                    HistoriaClinica nueva = new HistoriaClinica();
                    nueva.setMascota(mascota);
                    nueva.setNumeroHc(String.format("HC-%06d", mascota.getId()));
                    nueva.setActiva(true);
                    return historiaClinicaRepository.save(nueva);
                });

        ControlPreventivo controlAplicado = completarControl(request.getControlPreventivoId(), mascota,
                tipo, tipoVacuna);

        Cita cobro = crearCitaCobro(mascota, empleado, servicio, tipo.name(), total, aplicacion);
        citaRepository.save(cobro);
        notificarCaja(cobro, mascota, tipo, total);

        if (tipo == TipoControlPreventivo.VACUNACION) {
            RegistroVacuna registro = new RegistroVacuna();
            registro.setHistoriaClinica(hc);
            registro.setConsulta(null);
            registro.setCita(cobro);
            registro.setVeterinario(empleado);
            registro.setControlPreventivo(controlAplicado);
            registro.setTipoVacuna(tipoVacuna);
            registro.setNombreVacuna(nombre);
            registro.setFechaAplicacion(aplicacion);
            registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
            registro.setFechaProximaDosis(proxima);
            copiarTrazabilidad(request, registro);
            registro.setCreatedBy(actor);
            registro.setUpdatedBy(actor);
            vacunaRepository.save(registro);
            crearSiguienteControl(mascota, tipo, tipoVacuna, nombre, proxima, actor);
            auditLogService.log("REGISTRAR_VACUNACION_CARTILLA", "Cartilla",
                    "Se registro la vacuna " + nombre + " para " + mascota.getNombreCompleto()
                            + " (codigo de cobro " + cobro.getNumeroCita() + ")");
            return toResponse(registro.getId(), tipo, mascota, hc, nombre, aplicacion, proxima,
                    request, empleado, cobro, total);
        }

        RegistroDesparasitacion registro = new RegistroDesparasitacion();
        registro.setHistoriaClinica(hc);
        registro.setConsulta(null);
        registro.setCita(cobro);
        registro.setVeterinario(empleado);
        registro.setControlPreventivo(controlAplicado);
        registro.setTipoDesparasitante(tipoDesparasitante);
        registro.setProducto(nombre);
        registro.setFechaAplicacion(aplicacion);
        registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
        registro.setFechaProximaAplicacion(proxima);
        copiarTrazabilidad(request, registro);
        registro.setCreatedBy(actor);
        registro.setUpdatedBy(actor);
        desparasitacionRepository.save(registro);
        crearSiguienteControl(mascota, tipo, null, nombre, proxima, actor);
        auditLogService.log("REGISTRAR_DESPARASITACION_CARTILLA", "Cartilla",
                "Se registro la desparasitacion de " + mascota.getNombreCompleto()
                        + " (codigo de cobro " + cobro.getNumeroCita() + ")");
        return toResponse(registro.getId(), tipo, mascota, hc, nombre, aplicacion, proxima,
                request, empleado, cobro, total);
    }

    private Cita crearCitaCobro(Mascota mascota, Empleado empleado, ServiciosVeterinarios servicio,
                                String motivo, BigDecimal total, LocalDate fechaAplicacion) {
        int duracion = servicio.getDuracionEstimada() != null && servicio.getDuracionEstimada() > 0
                ? servicio.getDuracionEstimada() : 30;
        LocalDateTime ahora = fechaAplicacion.isEqual(AppClock.today())
                ? AppClock.now() : fechaAplicacion.atTime(LocalTime.NOON);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita(motivo);
        cita.setFechaHoraInicio(ahora);
        cita.setFechaHoraFin(ahora.plusMinutes(duracion));
        cita.setDuracionMinutos(duracion);
        cita.setEstado(EstadoCita.COMPLETADA);
        cita.setTotalServicio(total);
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEsEmergencia(false);
        return cita;
    }

    private Empleado resolverEmpleado(Mascota mascota) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Empleado empleado = userId == null ? null : empleadoRepository.findByUserId(userId).orElse(null);
        if (empleado == null) {
            throw new IllegalArgumentException("El usuario autenticado no esta asociado a un profesional activo");
        }
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (empleado.getUser() == null || empleado.getUser().getCompany() == null
                || !companyId.equals(empleado.getUser().getCompany().getId())
                || !Boolean.TRUE.equals(empleado.getEstado())) {
            throw new IllegalArgumentException("El profesional no pertenece a la veterinaria o se encuentra inactivo");
        }
        return empleado;
    }

    private ServiciosVeterinarios resolverServicioPreventivo(Long servicioId, TipoControlPreventivo tipo, Mascota mascota) {
        if (servicioId == null) {
            Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
            TipoControlServicio esperado = tipo == TipoControlPreventivo.VACUNACION
                    ? TipoControlServicio.VACUNACION : TipoControlServicio.DESPARASITACION;
            return serviciosRepository.findByCompanyIdAndDisponibleTrueAndActivoTrue(companyId).stream()
                    .filter(s -> s.getTipoControlPreventivo() == esperado)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No existe un servicio activo configurado para "
                            + (tipo == TipoControlPreventivo.VACUNACION ? "vacunacion" : "desparasitacion")));
        }
        ServiciosVeterinarios servicio = serviciosRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio preventivo no encontrado"));
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (!servicio.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("El servicio no pertenece a la veterinaria de la mascota");
        }
        TipoControlServicio esperado = tipo == TipoControlPreventivo.VACUNACION
                ? TipoControlServicio.VACUNACION : TipoControlServicio.DESPARASITACION;
        if (servicio.getTipoControlPreventivo() != esperado) {
            throw new IllegalArgumentException("El servicio seleccionado no corresponde a "
                    + (tipo == TipoControlPreventivo.VACUNACION ? "vacunacion" : "desparasitacion"));
        }
        return servicio;
    }

    private TipoVacuna validarTipoVacuna(Long id, Mascota mascota) {
        TipoVacuna vacuna = tipoVacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de vacuna no encontrado"));
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (!vacuna.getCompany().getId().equals(companyId)
                || vacuna.getEspecie() != mascota.getEspecie() || !vacuna.getActivo()) {
            throw new IllegalArgumentException("La vacuna no corresponde a la especie o veterinaria de la mascota");
        }
        return vacuna;
    }

    private TipoDesparasitante validarTipoDesparasitante(Long id, Mascota mascota) {
        TipoDesparasitante desparasitante = tipoDesparasitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desparasitante no encontrado"));
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (!desparasitante.getCompany().getId().equals(companyId)
                || desparasitante.getEspecie() != mascota.getEspecie() || !desparasitante.getActivo()) {
            throw new IllegalArgumentException("El desparasitante no corresponde a la especie o veterinaria de la mascota");
        }
        return desparasitante;
    }

    private LocalDate calcularProxima(LocalDate aplicacion, CartillaAplicacionRequest request) {
        if (Boolean.FALSE.equals(request.getProgramarProximoControl())) return null;
        if (request.getFechaProxima() != null) return request.getFechaProxima();
        if (request.getIntervaloCantidad() != null && request.getIntervaloUnidad() != null) {
            return switch (request.getIntervaloUnidad()) {
                case DIAS -> aplicacion.plusDays(request.getIntervaloCantidad());
                case SEMANAS -> aplicacion.plusWeeks(request.getIntervaloCantidad());
                case MESES -> aplicacion.plusMonths(request.getIntervaloCantidad());
            };
        }
        if (request.getPeriodicidadMeses() != null) return aplicacion.plusMonths(request.getPeriodicidadMeses());
        return null;
    }

    private void validarDatosAplicacion(CartillaAplicacionRequest request) {
        boolean cantidad = request.getIntervaloCantidad() != null;
        boolean unidad = request.getIntervaloUnidad() != null;
        if (cantidad != unidad) {
            throw new IllegalArgumentException("El intervalo requiere cantidad y unidad");
        }
        if (request.getDosis() != null && (request.getUnidadDosis() == null || request.getUnidadDosis().isBlank())) {
            throw new IllegalArgumentException("Indique la unidad de la dosis");
        }
        if (request.getFechaVencimientoProducto() != null
                && request.getFechaVencimientoProducto().isBefore(request.getFechaAplicacion())) {
            throw new IllegalArgumentException("No se puede aplicar un producto vencido");
        }
    }

    private void validarFechas(Mascota mascota, LocalDate aplicacion, LocalDate proxima) {
        if (aplicacion.isAfter(AppClock.today())) {
            throw new IllegalArgumentException("La fecha de aplicacion no puede ser futura");
        }
        if (mascota.getFechaNacimiento() != null && aplicacion.isBefore(mascota.getFechaNacimiento())) {
            throw new IllegalArgumentException("La fecha de aplicacion no puede ser anterior al nacimiento de la mascota");
        }
        if (proxima != null && !proxima.isAfter(aplicacion)) {
            throw new IllegalArgumentException("La proxima aplicacion debe ser posterior");
        }
    }

    private void validarCompany(Mascota mascota) {
        if (SecurityUtils.isSuperAdmin()) return;
        Integer actual = SecurityUtils.getCurrentCompanyId();
        Integer mascotaCompany = mascota.getApoderado().getUser().getCompany().getId();
        if (actual == null || !actual.equals(mascotaCompany)) {
            throw new IllegalArgumentException("No tiene acceso a esta mascota");
        }
    }

    private ControlPreventivo completarControl(Long controlId, Mascota mascota,
                                                TipoControlPreventivo tipo, TipoVacuna vacuna) {
        if (controlId == null) return null;
        ControlPreventivo control = controlRepository.findById(controlId)
                .orElseThrow(() -> new ResourceNotFoundException("Control preventivo no encontrado"));
        if (!control.getMascota().getId().equals(mascota.getId()) || control.getTipo() != tipo) {
            throw new IllegalArgumentException("El control no corresponde a la mascota o al tipo de aplicacion");
        }
        if (vacuna != null && (control.getTipoVacuna() == null
                || !vacuna.getId().equals(control.getTipoVacuna().getId()))) {
            throw new IllegalArgumentException("La vacuna aplicada no corresponde al control seleccionado");
        }
        if (!ESTADOS_ABIERTOS.contains(control.getEstado())) {
            throw new IllegalArgumentException("El control seleccionado ya fue cerrado");
        }
        control.setEstado(EstadoControlPreventivo.APLICADO);
        control.setCitaSuspende(null);
        control.setEstadoModificadoPor(actor());
        control.setFechaModificacionEstado(AppClock.now());
        control.setUpdatedBy(actor());
        return controlRepository.save(control);
    }

    private void crearSiguienteControl(Mascota mascota, TipoControlPreventivo tipo, TipoVacuna vacuna,
                                       String nombre, LocalDate fecha, String actor) {
        if (fecha == null) return;
        boolean existe = vacuna != null
                ? controlRepository.existsByMascotaIdAndTipoVacunaIdAndFechaRecomendadaAndEstadoIn(
                        mascota.getId(), vacuna.getId(), fecha, ESTADOS_ABIERTOS)
                : controlRepository.existsByMascotaIdAndTipoAndNombreControlIgnoreCaseAndFechaRecomendadaAndEstadoIn(
                        mascota.getId(), tipo, nombre, fecha, ESTADOS_ABIERTOS);
        if (existe) return;
        ControlPreventivo siguiente = new ControlPreventivo();
        siguiente.setMascota(mascota);
        siguiente.setTipo(tipo);
        siguiente.setTipoVacuna(vacuna);
        siguiente.setNombreControl(nombre);
        siguiente.setFechaRecomendada(fecha);
        siguiente.setCreatedBy(actor);
        siguiente.setUpdatedBy(actor);
        siguiente.setEstadoModificadoPor(actor);
        siguiente.setFechaModificacionEstado(AppClock.now());
        siguiente.setEstado(estadoParaFecha(fecha));
        controlRepository.save(siguiente);
    }

    private EstadoControlPreventivo estadoParaFecha(LocalDate fecha) {
        LocalDate hoy = AppClock.today();
        if (fecha.isBefore(hoy)) return EstadoControlPreventivo.ATRASADO;
        if (fecha.isEqual(hoy)) return EstadoControlPreventivo.PENDIENTE;
        if (!fecha.isAfter(hoy.plusDays(7))) return EstadoControlPreventivo.PROXIMO;
        return EstadoControlPreventivo.PROGRAMADO;
    }

    private void copiarTrazabilidad(CartillaAplicacionRequest request, RegistroVacuna registro) {
        registro.setIntervaloCantidad(request.getIntervaloCantidad());
        registro.setIntervaloUnidad(request.getIntervaloUnidad());
        registro.setLote(normalizar(request.getLote()));
        registro.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        registro.setDosis(request.getDosis());
        registro.setUnidadDosis(normalizar(request.getUnidadDosis()));
        registro.setViaAdministracion(normalizar(request.getViaAdministracion()));
        registro.setSitioAplicacion(normalizar(request.getSitioAplicacion()));
        registro.setPesoKg(request.getPesoKg());
        registro.setObservaciones(normalizar(request.getObservaciones()));
    }

    private void copiarTrazabilidad(CartillaAplicacionRequest request, RegistroDesparasitacion registro) {
        registro.setIntervaloCantidad(request.getIntervaloCantidad());
        registro.setIntervaloUnidad(request.getIntervaloUnidad());
        registro.setLote(normalizar(request.getLote()));
        registro.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        registro.setDosis(request.getDosis());
        registro.setUnidadDosis(normalizar(request.getUnidadDosis()));
        registro.setViaAdministracion(normalizar(request.getViaAdministracion()));
        registro.setSitioAplicacion(normalizar(request.getSitioAplicacion()));
        registro.setPesoKg(request.getPesoKg());
        registro.setObservaciones(normalizar(request.getObservaciones()));
    }

    private void completarDesdeCatalogo(CartillaAplicacionRequest request, TipoVacuna producto) {
        if (!Boolean.FALSE.equals(request.getProgramarProximoControl()) && request.getPeriodicidadMeses() == null && request.getIntervaloCantidad() == null && request.getFechaProxima() == null
                && producto.getPeriodicidadMesesSugerida() != null) request.setPeriodicidadMeses(producto.getPeriodicidadMesesSugerida());
        if (request.getLote() == null) request.setLote(producto.getLote());
        if (request.getFechaVencimientoProducto() == null) request.setFechaVencimientoProducto(producto.getFechaVencimientoProducto());
        if (request.getDosis() == null) request.setDosis(producto.getDosis());
        if (request.getUnidadDosis() == null) request.setUnidadDosis(producto.getUnidadDosis());
        if (request.getViaAdministracion() == null) request.setViaAdministracion(producto.getViaAdministracion());
    }

    private void completarDesdeCatalogo(CartillaAplicacionRequest request, TipoDesparasitante producto) {
        if (!Boolean.FALSE.equals(request.getProgramarProximoControl()) && request.getPeriodicidadMeses() == null && request.getIntervaloCantidad() == null && request.getFechaProxima() == null
                && producto.getPeriodicidadMesesSugerida() != null) request.setPeriodicidadMeses(producto.getPeriodicidadMesesSugerida());
        if (request.getLote() == null) request.setLote(producto.getLote());
        if (request.getFechaVencimientoProducto() == null) request.setFechaVencimientoProducto(producto.getFechaVencimientoProducto());
        if (request.getDosis() == null) request.setDosis(producto.getDosis());
        if (request.getUnidadDosis() == null) request.setUnidadDosis(producto.getUnidadDosis());
        if (request.getViaAdministracion() == null) request.setViaAdministracion(producto.getViaAdministracion());
    }

    private void notificarCaja(Cita cobro, Mascota mascota, TipoControlPreventivo tipo, BigDecimal total) {
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        Map<String, Object> event = new HashMap<>();
        event.put("tipo", "CUENTA_PREVENTIVA_CREADA");
        event.put("citaId", cobro.getId());
        event.put("numeroCita", cobro.getNumeroCita());
        event.put("mascotaNombre", mascota.getNombreCompleto());
        event.put("control", tipo.name());
        event.put("total", total);
        messagingTemplate.convertAndSend("/topic/caja/" + companyId, event);
    }

    @Override
    @Transactional
    public CartillaAplicacionResponse editarVacunacion(Long id, CartillaAplicacionEditRequest request) {
        RegistroVacuna registro = vacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacuna no encontrado"));
        validarCompany(registro.getHistoriaClinica().getMascota());

        if (request.getFechaAplicacion() != null) registro.setFechaAplicacion(request.getFechaAplicacion());
        if (request.getLote() != null) registro.setLote(normalizar(request.getLote()));
        if (request.getFechaVencimientoProducto() != null) registro.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        if (request.getDosis() != null) registro.setDosis(request.getDosis());
        if (request.getUnidadDosis() != null) registro.setUnidadDosis(normalizar(request.getUnidadDosis()));
        if (request.getViaAdministracion() != null) registro.setViaAdministracion(normalizar(request.getViaAdministracion()));
        if (request.getSitioAplicacion() != null) registro.setSitioAplicacion(normalizar(request.getSitioAplicacion()));
        if (request.getPesoKg() != null) registro.setPesoKg(request.getPesoKg());
        if (request.getObservaciones() != null) registro.setObservaciones(normalizar(request.getObservaciones()));
        if (request.getIntervaloCantidad() != null) registro.setIntervaloCantidad(request.getIntervaloCantidad());
        if (request.getIntervaloUnidad() != null) registro.setIntervaloUnidad(IntervaloUnidad.valueOf(request.getIntervaloUnidad()));

        registro.setUpdatedBy(actor());
        vacunaRepository.save(registro);

        Mascota mascota = registro.getHistoriaClinica().getMascota();
        auditLogService.log("EDITAR_VACUNACION_CARTILLA", "Cartilla",
                "Se edito la vacuna " + registro.getNombreVacuna() + " de " + mascota.getNombreCompleto());

        return toResponse(registro);
    }

    @Override
    @Transactional
    public CartillaAplicacionResponse editarDesparasitacion(Long id, CartillaAplicacionEditRequest request) {
        RegistroDesparasitacion registro = desparasitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de desparasitacion no encontrado"));
        validarCompany(registro.getHistoriaClinica().getMascota());

        if (request.getFechaAplicacion() != null) registro.setFechaAplicacion(request.getFechaAplicacion());
        if (request.getLote() != null) registro.setLote(normalizar(request.getLote()));
        if (request.getFechaVencimientoProducto() != null) registro.setFechaVencimientoProducto(request.getFechaVencimientoProducto());
        if (request.getDosis() != null) registro.setDosis(request.getDosis());
        if (request.getUnidadDosis() != null) registro.setUnidadDosis(normalizar(request.getUnidadDosis()));
        if (request.getViaAdministracion() != null) registro.setViaAdministracion(normalizar(request.getViaAdministracion()));
        if (request.getSitioAplicacion() != null) registro.setSitioAplicacion(normalizar(request.getSitioAplicacion()));
        if (request.getPesoKg() != null) registro.setPesoKg(request.getPesoKg());
        if (request.getObservaciones() != null) registro.setObservaciones(normalizar(request.getObservaciones()));
        if (request.getIntervaloCantidad() != null) registro.setIntervaloCantidad(request.getIntervaloCantidad());
        if (request.getIntervaloUnidad() != null) registro.setIntervaloUnidad(IntervaloUnidad.valueOf(request.getIntervaloUnidad()));

        registro.setUpdatedBy(actor());
        desparasitacionRepository.save(registro);

        Mascota mascota = registro.getHistoriaClinica().getMascota();
        auditLogService.log("EDITAR_DESPARASITACION_CARTILLA", "Cartilla",
                "Se edito la desparasitacion " + registro.getProducto() + " de " + mascota.getNombreCompleto());

        return toResponse(registro);
    }

    @Override
    @Transactional
    public void cambiarEstadoVacunacion(Long id, boolean activo) {
        RegistroVacuna registro = vacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacuna no encontrado"));
        validarCompany(registro.getHistoriaClinica().getMascota());

        registro.setActivo(activo);
        registro.setEstadoModificadoPor(actor());
        registro.setFechaModificacionEstado(AppClock.now());
        registro.setUpdatedBy(actor());
        vacunaRepository.save(registro);

        Mascota mascota = registro.getHistoriaClinica().getMascota();
        auditLogService.log(activo ? "ACTIVAR_VACUNACION" : "DESACTIVAR_VACUNACION", "Cartilla",
                (activo ? "Se activo" : "Se desactivo") + " la vacuna " + registro.getNombreVacuna()
                        + " de " + mascota.getNombreCompleto());
    }

    @Override
    @Transactional
    public void cambiarEstadoDesparasitacion(Long id, boolean activo) {
        RegistroDesparasitacion registro = desparasitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de desparasitacion no encontrado"));
        validarCompany(registro.getHistoriaClinica().getMascota());

        registro.setActivo(activo);
        registro.setEstadoModificadoPor(actor());
        registro.setFechaModificacionEstado(AppClock.now());
        registro.setUpdatedBy(actor());
        desparasitacionRepository.save(registro);

        Mascota mascota = registro.getHistoriaClinica().getMascota();
        auditLogService.log(activo ? "ACTIVAR_DESPARASITACION" : "DESACTIVAR_DESPARASITACION", "Cartilla",
                (activo ? "Se activo" : "Se desactivo") + " la desparasitacion " + registro.getProducto()
                        + " de " + mascota.getNombreCompleto());
    }

    private CartillaAplicacionResponse toResponse(RegistroVacuna registro) {
        Mascota mascota = registro.getHistoriaClinica().getMascota();
        Empleado emp = registro.getVeterinario();
        return CartillaAplicacionResponse.builder()
                .registroId(registro.getId())
                .tipo(TipoControlPreventivo.VACUNACION)
                .mascotaId(mascota.getId())
                .mascotaNombre(mascota.getNombreCompleto())
                .numeroHc(registro.getHistoriaClinica().getNumeroHc())
                .nombre(registro.getNombreVacuna())
                .fechaAplicacion(registro.getFechaAplicacion())
                .fechaProxima(registro.getFechaProximaDosis())
                .periodicidadMeses(registro.getPeriodicidadMeses())
                .intervaloCantidad(registro.getIntervaloCantidad())
                .intervaloUnidad(registro.getIntervaloUnidad())
                .veterinarioNombre(nombreVeterinario(emp))
                .lote(registro.getLote())
                .fechaVencimientoProducto(registro.getFechaVencimientoProducto())
                .dosis(registro.getDosis())
                .unidadDosis(registro.getUnidadDosis())
                .viaAdministracion(registro.getViaAdministracion())
                .sitioAplicacion(registro.getSitioAplicacion())
                .pesoKg(registro.getPesoKg())
                .observaciones(registro.getObservaciones())
                .build();
    }

    private CartillaAplicacionResponse toResponse(RegistroDesparasitacion registro) {
        Mascota mascota = registro.getHistoriaClinica().getMascota();
        Empleado emp = registro.getVeterinario();
        return CartillaAplicacionResponse.builder()
                .registroId(registro.getId())
                .tipo(TipoControlPreventivo.DESPARASITACION)
                .mascotaId(mascota.getId())
                .mascotaNombre(mascota.getNombreCompleto())
                .numeroHc(registro.getHistoriaClinica().getNumeroHc())
                .nombre(registro.getProducto())
                .fechaAplicacion(registro.getFechaAplicacion())
                .fechaProxima(registro.getFechaProximaAplicacion())
                .periodicidadMeses(registro.getPeriodicidadMeses())
                .intervaloCantidad(registro.getIntervaloCantidad())
                .intervaloUnidad(registro.getIntervaloUnidad())
                .veterinarioNombre(nombreVeterinario(emp))
                .lote(registro.getLote())
                .fechaVencimientoProducto(registro.getFechaVencimientoProducto())
                .dosis(registro.getDosis())
                .unidadDosis(registro.getUnidadDosis())
                .viaAdministracion(registro.getViaAdministracion())
                .sitioAplicacion(registro.getSitioAplicacion())
                .pesoKg(registro.getPesoKg())
                .observaciones(registro.getObservaciones())
                .build();
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String actor() {
        String email = SecurityUtils.getCurrentUserEmail();
        return email == null || email.isBlank() ? "SYSTEM" : email;
    }

    private CartillaAplicacionResponse toResponse(Long registroId, TipoControlPreventivo tipo, Mascota mascota,
                                                  HistoriaClinica hc, String nombre, LocalDate aplicacion,
                                                  LocalDate proxima, CartillaAplicacionRequest request, Empleado empleado,
                                                  Cita cobro, BigDecimal total) {
        return CartillaAplicacionResponse.builder()
                .registroId(registroId)
                .tipo(tipo)
                .mascotaId(mascota.getId())
                .mascotaNombre(mascota.getNombreCompleto())
                .numeroHc(hc.getNumeroHc())
                .nombre(nombre)
                .fechaAplicacion(aplicacion)
                .fechaProxima(proxima)
                .periodicidadMeses(request.getPeriodicidadMeses())
                .intervaloCantidad(request.getIntervaloCantidad())
                .intervaloUnidad(request.getIntervaloUnidad())
                .veterinarioNombre(nombreVeterinario(empleado))
                .lote(normalizar(request.getLote()))
                .fechaVencimientoProducto(request.getFechaVencimientoProducto())
                .dosis(request.getDosis())
                .unidadDosis(normalizar(request.getUnidadDosis()))
                .viaAdministracion(normalizar(request.getViaAdministracion()))
                .sitioAplicacion(normalizar(request.getSitioAplicacion()))
                .pesoKg(request.getPesoKg())
                .observaciones(normalizar(request.getObservaciones()))
                .citaId(cobro.getId())
                .codigoCobro(cobro.getNumeroCita())
                .total(total)
                .build();
    }

    private String nombreVeterinario(Empleado empleado) {
        if (empleado == null || empleado.getUser() == null) return null;
        return (empleado.getUser().getNombre() + " " + empleado.getUser().getApellido()).trim();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse> listarRecordatoriosWhatsApp(Integer companyId) {
        LocalDate hoy = AppClock.today();
        var candidatos = controlRepository.findPendientesByCompany(companyId,
                EnumSet.of(EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
                        EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO));

        return candidatos.stream().map(cp -> {
            Mascota m = cp.getMascota();
            var apoderado = m.getApoderado();
            var user = apoderado.getUser();
            LocalDate fecha = cp.getFechaRecomendada();
            long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, fecha);
            String resumenDias;
            String estado;
            if (dias < 0) { resumenDias = "Venció hace " + Math.abs(dias) + " día(s)"; estado = "ATRASADO"; }
            else if (dias == 0) { resumenDias = "Es hoy"; estado = "PENDIENTE"; }
            else { resumenDias = "En " + dias + " día(s)"; estado = "PROXIMO"; }

            String tipoDisplay = cp.getTipo() == TipoControlPreventivo.VACUNACION ? "Vacunación" : "Desparasitación";
            String nombreControl = cp.getNombreControl();
            String telefono = user.getTelefono();

            String mensaje = String.format(
                    "Hola %s, le recordamos que %s tiene un control de %s programado para el %s (%s). " +
                    "Por favor coordinar su atención. - %s",
                    user.getNombre() != null ? user.getNombre().split(" ")[0] : "estimado/a",
                    m.getNombreCompleto(),
                    tipoDisplay,
                    fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    resumenDias,
                    user.getCompany() != null ? user.getCompany().getName() : "Veterinaria"
            );

            return veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse.builder()
                    .controlId(cp.getId())
                    .mascotaNombre(m.getNombreCompleto())
                    .apoderadoId(apoderado.getId())
                    .apoderadoNombre((user.getNombre() + " " + user.getApellido()).trim())
                    .apoderadoTelefono(telefono)
                    .tipoControl(tipoDisplay)
                    .nombreControl(nombreControl)
                    .fechaRecomendada(fecha)
                    .estado(estado)
                    .diasRestantes((int) dias)
                    .resumenDias(resumenDias)
                    .mensajeWhatsApp(mensaje)
                    .build();
        }).toList();
    }
}
