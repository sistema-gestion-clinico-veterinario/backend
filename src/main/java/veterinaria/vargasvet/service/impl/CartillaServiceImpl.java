package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlServicio;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CartillaService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartillaServiceImpl implements CartillaService {

    private final MascotaRepository mascotaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServiciosVeterinariosRepository serviciosRepository;
    private final TipoVacunaRepository tipoVacunaRepository;
    private final TipoDesparasitanteRepository tipoDesparasitanteRepository;
    private final RegistroVacunaRepository vacunaRepository;
    private final RegistroDesparasitacionRepository desparasitacionRepository;
    private final CitaRepository citaRepository;
    private final AuditLogService auditLogService;

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

    private CartillaAplicacionResponse registrar(CartillaAplicacionRequest request, TipoControlPreventivo tipo) {
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        validarCompany(mascota);

        Empleado empleado = resolverEmpleado(request.getEmpleadoId());
        ServiciosVeterinarios servicio = validarServicioPreventivo(request.getServicioId(), tipo, mascota);

        TipoVacuna tipoVacuna = null;
        TipoDesparasitante tipoDesparasitante = null;
        String nombre;
        if (tipo == TipoControlPreventivo.VACUNACION) {
            tipoVacuna = validarTipoVacuna(request.getTipoVacunaId(), mascota);
            nombre = tipoVacuna.getNombre();
        } else {
            tipoDesparasitante = validarTipoDesparasitante(request.getTipoDesparasitanteId(), mascota);
            nombre = tipoDesparasitante.getNombre();
        }

        LocalDate aplicacion = request.getFechaAplicacion();
        LocalDate proxima = calcularProxima(aplicacion, request.getFechaProxima(), request.getPeriodicidadMeses());
        validarFechas(mascota, aplicacion, proxima);

        BigDecimal total = request.getTotal() != null ? request.getTotal()
                : (tipo == TipoControlPreventivo.VACUNACION ? tipoVacuna.getPrecio() : tipoDesparasitante.getPrecio());

        String actor = actor();

        HistoriaClinica hc = historiaClinicaRepository.findByMascotaId(mascota.getId())
                .orElseGet(() -> {
                    HistoriaClinica nueva = new HistoriaClinica();
                    nueva.setMascota(mascota);
                    nueva.setNumeroHc(String.format("HC-%06d", mascota.getId()));
                    nueva.setActiva(true);
                    return historiaClinicaRepository.save(nueva);
                });

        Cita cobro = crearCitaCobro(mascota, empleado, servicio, tipo.name(), total);
        citaRepository.save(cobro);

        if (tipo == TipoControlPreventivo.VACUNACION) {
            RegistroVacuna registro = new RegistroVacuna();
            registro.setHistoriaClinica(hc);
            registro.setConsulta(null);
            registro.setCita(cobro);
            registro.setVeterinario(empleado);
            registro.setTipoVacuna(tipoVacuna);
            registro.setNombreVacuna(nombre);
            registro.setFechaAplicacion(aplicacion);
            registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
            registro.setFechaProximaDosis(proxima);
            registro.setCreatedBy(actor);
            registro.setUpdatedBy(actor);
            vacunaRepository.save(registro);
            auditLogService.log("REGISTRAR_VACUNACION_CARTILLA", "Cartilla",
                    "Se registro la vacuna " + nombre + " para " + mascota.getNombreCompleto()
                            + " (codigo de cobro " + cobro.getNumeroCita() + ")");
            return toResponse(registro.getId(), tipo, mascota, hc, nombre, aplicacion, proxima,
                    request.getPeriodicidadMeses(), empleado, cobro, total);
        }

        RegistroDesparasitacion registro = new RegistroDesparasitacion();
        registro.setHistoriaClinica(hc);
        registro.setConsulta(null);
        registro.setCita(cobro);
        registro.setVeterinario(empleado);
        registro.setProducto(nombre);
        registro.setFechaAplicacion(aplicacion);
        registro.setPeriodicidadMeses(request.getPeriodicidadMeses());
        registro.setFechaProximaAplicacion(proxima);
        registro.setCreatedBy(actor);
        registro.setUpdatedBy(actor);
        desparasitacionRepository.save(registro);
        auditLogService.log("REGISTRAR_DESPARASITACION_CARTILLA", "Cartilla",
                "Se registro la desparasitacion de " + mascota.getNombreCompleto()
                        + " (codigo de cobro " + cobro.getNumeroCita() + ")");
        return toResponse(registro.getId(), tipo, mascota, hc, nombre, aplicacion, proxima,
                request.getPeriodicidadMeses(), empleado, cobro, total);
    }

    private Cita crearCitaCobro(Mascota mascota, Empleado empleado, ServiciosVeterinarios servicio,
                                String motivo, BigDecimal total) {
        int duracion = servicio.getDuracionEstimada() != null && servicio.getDuracionEstimada() > 0
                ? servicio.getDuracionEstimada() : 30;
        LocalDateTime ahora = AppClock.now();
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita(motivo);
        cita.setFechaHoraInicio(ahora);
        cita.setFechaHoraFin(ahora.plusMinutes(duracion));
        cita.setDuracionMinutos(duracion);
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setTotalServicio(total);
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEsEmergencia(false);
        return cita;
    }

    private Empleado resolverEmpleado(Long empleadoId) {
        if (empleadoId != null) {
            return empleadoRepository.findById(empleadoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
        }
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return empleadoRepository.findByUserId(userId).orElse(null);
        }
        return null;
    }

    private ServiciosVeterinarios validarServicioPreventivo(Long servicioId, TipoControlPreventivo tipo, Mascota mascota) {
        ServiciosVeterinarios servicio = serviciosRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio preventivo no encontrado"));
        Integer companyId = mascota.getApoderado().getUser().getCompany().getId();
        if (!servicio.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("El servicio no pertenece a la veterinaria de la mascota");
        }
        // Se acepta tanto el servicio que coincide con el tipo como un preventivo generico,
        // pero se exige que no sea NO_APLICA para evitar cobros sin control.
        if (servicio.getTipoControlPreventivo() == null
                || servicio.getTipoControlPreventivo() == TipoControlServicio.NO_APLICA) {
            throw new IllegalArgumentException("El servicio seleccionado no es preventivo");
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

    private LocalDate calcularProxima(LocalDate aplicacion, LocalDate fechaExplicita, Integer periodicidadMeses) {
        if (fechaExplicita != null) return fechaExplicita;
        if (periodicidadMeses == null) {
            throw new IllegalArgumentException("Indique la proxima fecha o una periodicidad");
        }
        return aplicacion.plusMonths(periodicidadMeses);
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

    private String actor() {
        String email = SecurityUtils.getCurrentUserEmail();
        return email == null || email.isBlank() ? "SYSTEM" : email;
    }

    private CartillaAplicacionResponse toResponse(Long registroId, TipoControlPreventivo tipo, Mascota mascota,
                                                  HistoriaClinica hc, String nombre, LocalDate aplicacion,
                                                  LocalDate proxima, Integer periodicidad, Empleado empleado,
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
                .periodicidadMeses(periodicidad)
                .veterinarioNombre(nombreVeterinario(empleado))
                .citaId(cobro.getId())
                .codigoCobro(cobro.getNumeroCita())
                .total(total)
                .build();
    }

    private String nombreVeterinario(Empleado empleado) {
        if (empleado == null || empleado.getUser() == null) return null;
        return (empleado.getUser().getNombre() + " " + empleado.getUser().getApellido()).trim();
    }
}