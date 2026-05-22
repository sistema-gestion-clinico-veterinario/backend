package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.CitaMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.util.BusinessValidator;

import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.CitaWsEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServiciosVeterinariosRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final ConsultaRepository consultaRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final CompanyExceptionRepository companyExceptionRepository;
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CitaMapper citaMapper;
    private final BusinessValidator businessValidator;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int DURACION_ESTIMADA_MINUTOS = 20;

    @Override
    @Transactional
    public CitaResponse createCita(CitaRequest request) {
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        if (!mascota.getActivo()) {
            throw new IllegalArgumentException("No se puede crear una cita para una mascota inactiva");
        }

        if (mascota.getApoderado() == null
                || mascota.getApoderado().getUser() == null
                || !mascota.getApoderado().getUser().isActivo()) {
            throw new IllegalArgumentException("No se puede crear una cita porque el propietario de la mascota está inactivo");
        }

        businessValidator.checkCompanyActiva(
            mascota.getApoderado() != null && mascota.getApoderado().getUser() != null
                && mascota.getApoderado().getUser().getCompany() != null
                ? mascota.getApoderado().getUser().getCompany().getId() : null);

        Empleado veterinario = empleadoRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado con ID: " + request.getVeterinarioId()));

        if (veterinario.getUser() == null) {
            throw new IllegalArgumentException("No se puede asignar la cita a un empleado sin usuario asociado");
        }

        if (!Boolean.TRUE.equals(veterinario.getEstado())) {
            throw new IllegalArgumentException("No se puede asignar la cita a un empleado inactivo");
        }

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (mascota.getApoderado().getUser().getCompany() == null || !mascota.getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para crear citas para mascotas de otra clínica");
            }
            if (veterinario.getUser().getCompany() == null || !veterinario.getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para asignar citas a veterinarios de otra clínica");
            }
        }

        ServiciosVeterinarios servicio = null;
        if (request.getServicioId() != null) {
            servicio = servicioRepository.findById(request.getServicioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + request.getServicioId()));
        }

        int duracion = (servicio != null && servicio.getDuracionEstimada() != null)
                ? servicio.getDuracionEstimada()
                : DURACION_ESTIMADA_MINUTOS;

        LocalDateTime fechaInicio = request.getFechaHoraInicio();
        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        DiaSemana diaSemana = toDiaSemana(fechaInicio.getDayOfWeek());
        LocalTime horaInicio = fechaInicio.toLocalTime();
        LocalTime horaFinCita = fechaFin.toLocalTime();

        // Validación contra el horario de la clínica (solo si NO es emergencia)
        boolean esEmergencia = Boolean.TRUE.equals(request.getEsEmergencia());
        Company company = veterinario.getUser().getCompany();
        
        if (!esEmergencia && company != null) {
            LocalDate fechaCita = fechaInicio.toLocalDate();
            
            // 1. Validar excepciones (feriados/cierres)
            companyExceptionRepository.findByCompanyIdAndDate(company.getId(), fechaCita)
                    .ifPresent(ex -> {
                        if (Boolean.FALSE.equals(ex.getIsOpen())) {
                            throw new IllegalArgumentException("La clínica está cerrada el día " + fechaCita + " (" + ex.getDescription() + ")");
                        }
                    });

            // 2. Validar horario maestro de la clínica
            CompanyOperatingHour opHour = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(company.getId(), diaSemana)
                    .orElseThrow(() -> new IllegalArgumentException("La clínica no atiende los días " + diaSemana));
            
            if (Boolean.FALSE.equals(opHour.getIsOpen())) {
                throw new IllegalArgumentException("La clínica no abre los días " + diaSemana);
            }

            if (horaInicio.isBefore(opHour.getOpeningTime()) || horaFinCita.isAfter(opHour.getClosingTime())) {
                throw new IllegalArgumentException(String.format("La cita (%s-%s) está fuera del horario de la clínica (%s-%s)",
                        horaInicio, horaFinCita, opHour.getOpeningTime(), opHour.getClosingTime()));
            }

            // 3. Validar el turno específico del veterinario para esa fecha
            HorarioEmpleado turno = veterinario.getHorarios().stream()
                    .filter(h -> h.getFecha().equals(fechaCita) && Boolean.TRUE.equals(h.getActivo()))
                    .filter(h -> (horaInicio.equals(h.getHoraInicio()) || horaInicio.isAfter(h.getHoraInicio())) &&
                                 (horaFinCita.equals(h.getHoraFin()) || horaFinCita.isBefore(h.getHoraFin())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El veterinario no tiene turno asignado para cubrir este horario el día " + fechaCita));
        }

        boolean hayCruceVeterinario = citaRepository.existsOverlappingCita(veterinario.getId(), fechaInicio, fechaFin);
        if (hayCruceVeterinario) {
            throw new IllegalArgumentException("El veterinario ya tiene una cita programada en ese horario");
        }

        boolean hayCruceMascota = citaRepository.existsOverlappingCitaMascota(mascota.getId(), fechaInicio, fechaFin);
        if (hayCruceMascota) {
            throw new IllegalArgumentException("La mascota ya tiene una cita programada en ese horario con otro veterinario");
        }

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setFechaHoraInicio(fechaInicio);
        cita.setFechaHoraFin(fechaFin);
        cita.setDuracionMinutos(duracion);
        cita.setMotivoCita(request.getMotivoCita());
        cita.setNotas(request.getNotas());
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setEsEmergencia(esEmergencia);
        
        // Atributos obligatorios en la entidad Cita
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setTotalServicio(BigDecimal.ZERO);

        if (servicio != null) {
            cita.setServicio(servicio);
            cita.setTotalServicio(servicio.getPrecio());
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        usuarioRepository.findByEmail(currentUserEmail).ifPresent(cita::setCreadoPor);

        Cita savedCita = citaRepository.save(cita);

        auditLogService.log(
            "CREAR_CITA",
            "Citas",
            "Se agendó una nueva cita para la mascota " + mascota.getNombreCompleto() + " con el veterinario " + (veterinario.getUser() != null ? (veterinario.getUser().getNombre() + " " + veterinario.getUser().getApellido()) : "sin usuario") + " el " + cita.getFechaHoraInicio()
        );

        CitaResponse createdResponse = citaMapper.toResponse(savedCita);
        broadcastCitaEvent("CREAR_CITA", savedCita, createdResponse);
        return createdResponse;
    }

    @Override
    @Transactional
    public Long iniciarAtencion(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (cita.getMascota().getApoderado().getUser().getCompany() == null || 
                !cita.getMascota().getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para iniciar esta cita");
            }
        }

        if (cita.getEstado() == EstadoCita.EN_PROCESO) {
            boolean esGroomerEnProceso = cita.getEmpleado().getTiposEmpleado().stream()
                    .anyMatch(t -> "GROMMER".equalsIgnoreCase(t.getNombre()));
            if (esGroomerEnProceso) return null;
            if (cita.getConsulta() != null) {
                return cita.getConsulta().getId();
            }
            return consultaRepository.findByCitaId(id)
                    .map(Consulta::getId)
                    .orElseThrow(() -> new IllegalArgumentException("La cita está en proceso pero no se encontró la consulta asociada"));
        }

        if (cita.getEstado() != EstadoCita.PROGRAMADA &&
            cita.getEstado() != EstadoCita.REPROGRAMADA &&
            cita.getEstado() != EstadoCita.CONFIRMADA &&
            cita.getEstado() != EstadoCita.SALA_DE_ESPERA) {
            throw new IllegalArgumentException("No se puede iniciar una cita con estado: " + cita.getEstado());
        }

        if (!cita.getMascota().getActivo()) {
            throw new IllegalArgumentException("No se puede iniciar la atención porque la mascota está inactiva");
        }

        java.math.BigDecimal total = cita.getTotalServicio();
        if (total != null && total.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal mitad = total.multiply(new java.math.BigDecimal("0.5"));
            if (pagado.compareTo(mitad) < 0) {
                throw new IllegalArgumentException(
                    "Para iniciar la atención se debe haber abonado al menos el 50% del costo. " +
                    "Total: S/ " + total.setScale(2, java.math.RoundingMode.HALF_UP) +
                    " — Pagado: S/ " + pagado.setScale(2, java.math.RoundingMode.HALF_UP) +
                    " — Mínimo requerido: S/ " + mitad.setScale(2, java.math.RoundingMode.HALF_UP)
                );
            }
        }

        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isAdmin()) {
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            java.time.LocalDateTime inicioCita = cita.getFechaHoraInicio();
            long minutosRestantes = java.time.Duration.between(ahora, inicioCita).toMinutes();
            if (minutosRestantes > 60) {
                long horasRestantes = minutosRestantes / 60;
                long minutosExtra = minutosRestantes % 60;
                String tiempoRestante = horasRestantes > 0
                    ? horasRestantes + "h " + minutosExtra + "min"
                    : minutosExtra + " minutos";
                throw new IllegalArgumentException(
                    "Aún no es posible iniciar esta atención. Faltan " + tiempoRestante +
                    " para la cita. Solo se puede iniciar dentro de la hora previa al horario programado."
                );
            }
        }

        long citasEnProceso = citaRepository.countEnProcesoByEmpleadoExcluding(cita.getEmpleado().getId(), id);
        if (citasEnProceso > 0) {
            throw new IllegalArgumentException("El empleado ya tiene una atención en proceso. Debe completarla antes de iniciar otra.");
        }

        boolean esGroomer = cita.getEmpleado().getTiposEmpleado().stream()
                .anyMatch(t -> "GROMMER".equalsIgnoreCase(t.getNombre()));

        cita.setEstado(EstadoCita.EN_PROCESO);
        citaRepository.save(cita);

        auditLogService.log(
            "INICIAR_ATENCION",
            "Citas",
            "Se inició la atención " + (esGroomer ? "de grooming" : "médica") + " de la mascota " + cita.getMascota().getNombreCompleto() + " con el empleado " + (cita.getEmpleado().getUser() != null ? (cita.getEmpleado().getUser().getNombre() + " " + cita.getEmpleado().getUser().getApellido()) : "sin usuario") + " el " + cita.getFechaHoraInicio()
        );

        if (esGroomer) {
            broadcastCitaEvent("INICIAR_ATENCION", cita, citaMapper.toResponse(cita));
            return null;
        }

        HistoriaClinica hc = historiaClinicaRepository.findByMascotaId(cita.getMascota().getId())
                .orElseGet(() -> {
                    HistoriaClinica nuevaHc = new HistoriaClinica();
                    nuevaHc.setMascota(cita.getMascota());
                    nuevaHc.setNumeroHc(String.format("HC-%06d", cita.getMascota().getId()));
                    nuevaHc.setActiva(true);
                    return historiaClinicaRepository.save(nuevaHc);
                });

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(hc);
        consulta.setCita(cita);
        consulta.setVeterinario(cita.getEmpleado());
        consulta.setFechaConsulta(LocalDateTime.now());
        consulta.setMotivoConsulta(cita.getMotivoCita());
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        
        Consulta savedConsulta = consultaRepository.save(consulta);
        broadcastCitaEvent("INICIAR_ATENCION", cita, citaMapper.toResponse(cita));
        return savedConsulta.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CitaResponse> listar(Integer companyId, LocalDate fecha, EstadoCita estado, Long veterinarioId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        Long filteredVeterinarioId = veterinarioId;
        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isAdmin()) {

            if (SecurityUtils.hasRole("ROLE_VETERINARIO")) {
                filteredVeterinarioId = SecurityUtils.getCurrentUserId().longValue();
                filteredVeterinarioId = empleadoRepository.findByUserId(SecurityUtils.getCurrentUserId())
                        .map(Empleado::getId)
                        .orElse(veterinarioId);
            }
        }
        
        return citaRepository.buscar(resolvedCompanyId, fecha, estado, filteredVeterinarioId,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(citaMapper::toResponse);
    }

    @Override
    @Transactional
    public void cancelarCita(Long id, String motivo) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        validarPermisoEmpresa(cita);

        if (SecurityUtils.hasRole("ROLE_APODERADO")) {
            throw new IllegalArgumentException("Un apoderado no tiene permiso para cancelar citas");
        }

        if (cita.getEstado() == EstadoCita.COMPLETADA || cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("No se puede cancelar una cita que ya está " + cita.getEstado());
        }

        if (cita.getEstado() == EstadoCita.EN_PROCESO) {
            throw new IllegalArgumentException("No se puede cancelar una cita que ya está en proceso médico");
        }

        // Regla: 2 horas antes para apoderado, 1 hora para personal administrativo
        int hoursLimit = SecurityUtils.hasRole("ROLE_APODERADO") ? 2 : 1;
        if (LocalDateTime.now().isAfter(cita.getFechaHoraInicio().minusHours(hoursLimit))) {
            throw new IllegalArgumentException("No se puede cancelar la cita faltando menos de " + hoursLimit + " horas para su inicio");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(motivo);
        citaRepository.save(cita);

        broadcastCitaEvent("CANCELAR_CITA", cita, citaMapper.toResponse(cita));

        auditLogService.log(
            "CANCELAR_CITA",
            "Citas",
            "Se canceló la cita de la mascota " + cita.getMascota().getNombreCompleto() + " programada para el " + cita.getFechaHoraInicio() + ". Motivo: " + motivo
        );

        // Envío de correo electrónico al apoderado
        try {
            if (cita.getMascota().getApoderado() != null && cita.getMascota().getApoderado().getUser() != null) {
                String emailDestinatario = cita.getMascota().getApoderado().getUser().getEmail();
                if (emailDestinatario != null && !emailDestinatario.isBlank()) {
                    Company company = cita.getMascota().getApoderado().getUser().getCompany();
                    if (company == null && cita.getEmpleado() != null && cita.getEmpleado().getUser() != null) {
                        company = cita.getEmpleado().getUser().getCompany();
                    }

                    String companyName = company != null ? company.getName() : "VargasVet";
                    String companyLogo = (company != null && company.getLogoUrl() != null) ? company.getLogoUrl() : "";
                    String companyEmail = company != null ? company.getEmail() : "";
                    String companyPhone = company != null ? company.getPhone() : "";
                    String companyAddress = company != null ? company.getAddress() : "";

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String fechaCitaStr = formatter.format(cita.getFechaHoraInicio());

                    Map<String, Object> model = new HashMap<>();
                    model.put("nombreApoderado", cita.getMascota().getApoderado().getUser().getNombre());
                    model.put("nombreMascota", cita.getMascota().getNombreCompleto());
                    model.put("fechaCita", fechaCitaStr);
                    model.put("motivo", motivo != null && !motivo.isBlank() ? motivo : "No especificado");
                    model.put("companyName", companyName);
                    model.put("companyLogo", companyLogo);
                    model.put("companyEmail", companyEmail);
                    model.put("companyPhone", companyPhone);
                    model.put("companyAddress", companyAddress);

                    Mail mail = emailService.createMail(emailDestinatario, "Cita Cancelada - " + companyName, model);
                    emailService.sendEmail(mail, "email/cita-cancelar-template");
                }
            }
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de cancelación a " + cita.getMascota().getApoderado().getUser().getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void eliminarCita(Long id) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("Solo el administrador puede eliminar citas");
        }

        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        validarPermisoEmpresa(cita);

        if (cita.getEstado() != EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("Solo se permite eliminar citas en estado Cancelada");
        }

        cita.setEstado(EstadoCita.ELIMINADA);
        cita.setEliminada(true);
        cita.setEliminadoAt(LocalDateTime.now());
        
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        usuarioRepository.findByEmail(currentUserEmail).ifPresent(cita::setEliminadoPor);
        
        citaRepository.save(cita);
        broadcastCitaEvent("ELIMINAR_CITA", cita, citaMapper.toResponse(cita));

        auditLogService.log(
            "ELIMINAR_CITA",
            "Citas",
            "Se eliminó (borrado lógico) la cita cancelada de la mascota " + cita.getMascota().getNombreCompleto() + " programada para el " + cita.getFechaHoraInicio()
        );
    }

    @Override
    @Transactional
    public CitaResponse actualizarCita(Long id, CitaRequest request) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        validarPermisoEmpresa(cita);

        if (cita.getEstado() == EstadoCita.COMPLETADA || cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("No se puede modificar una cita que ya está " + cita.getEstado());
        }

        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        Empleado veterinario = empleadoRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));

        ServiciosVeterinarios servicio = null;
        if (request.getServicioId() != null) {
            servicio = servicioRepository.findById(request.getServicioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        }

        int duracion = (servicio != null && servicio.getDuracionEstimada() != null)
                ? servicio.getDuracionEstimada()
                : DURACION_ESTIMADA_MINUTOS;

        LocalDateTime fechaInicio = request.getFechaHoraInicio();
        LocalDateTime fechaFin = fechaInicio.plusMinutes(duracion);

        // Validar cruces (excluyendo la cita actual)
        boolean hayCruceVeterinario = citaRepository.existsOverlappingCitaExcludeSelf(veterinario.getId(), fechaInicio, fechaFin, id);
        if (hayCruceVeterinario) {
            throw new IllegalArgumentException("El veterinario ya tiene otra cita en ese horario");
        }

        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setFechaHoraInicio(fechaInicio);
        cita.setFechaHoraFin(fechaFin);
        cita.setDuracionMinutos(duracion);
        cita.setMotivoCita(request.getMotivoCita());
        cita.setNotas(request.getNotas());
        cita.setEsEmergencia(Boolean.TRUE.equals(request.getEsEmergencia()));

        if (servicio != null) {
            cita.setServicio(servicio);
            cita.setTotalServicio(servicio.getPrecio());
        }

        Cita updatedCita = citaRepository.save(cita);
        CitaResponse updatedResponse = citaMapper.toResponse(updatedCita);
        broadcastCitaEvent("ACTUALIZAR_CITA", updatedCita, updatedResponse);

        auditLogService.log(
            "ACTUALIZAR_CITA",
            "Citas",
            "Se actualizaron los datos de la cita de la mascota " + updatedCita.getMascota().getNombreCompleto() + " programada para el " + updatedCita.getFechaHoraInicio()
        );

        return updatedResponse;
    }

    @Override
    @Transactional
    public CitaResponse reprogramarCita(Long id, CitaRequest request) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        validarPermisoEmpresa(cita);

        // Guardar la fecha/hora original para el correo de reprogramación
        LocalDateTime originalFechaInicio = cita.getFechaHoraInicio();

        // Regla: Solo Programada o Cancelada
        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CANCELADA && cita.getEstado() != EstadoCita.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo se pueden reprogramar citas en estado Programada, Cancelada o Reprogramada");
        }

        // Regla: 6 horas antes para apoderado, 1 hora para personal administrativo
        int hoursLimit = SecurityUtils.hasRole("ROLE_APODERADO") ? 6 : 1;
        if (cita.getEstado() == EstadoCita.PROGRAMADA || cita.getEstado() == EstadoCita.REPROGRAMADA) {
            if (LocalDateTime.now().isAfter(cita.getFechaHoraInicio().minusHours(hoursLimit))) {
                throw new IllegalArgumentException("No se puede reprogramar una cita con menos de " + hoursLimit + " horas de anticipación");
            }
        }

        Empleado veterinario = empleadoRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));

        LocalDateTime fechaInicio = request.getFechaHoraInicio();
        LocalDateTime fechaFin = fechaInicio.plusMinutes(cita.getDuracionMinutos());

        // Validar disponibilidad
        boolean hayCruce = citaRepository.existsOverlappingCitaExcludeSelf(veterinario.getId(), fechaInicio, fechaFin, id);
        if (hayCruce) {
            throw new IllegalArgumentException("El veterinario ya tiene otra cita en ese horario");
        }

        // Aplicar cambios
        cita.setEmpleado(veterinario);
        cita.setFechaHoraInicio(fechaInicio);
        cita.setFechaHoraFin(fechaFin);
        cita.setEstado(EstadoCita.REPROGRAMADA);
        cita.setMotivoReprogramacion(request.getNotas()); // Usamos notas como motivo si no hay campo específico en request
        
        // Auditoría
        cita.setReprogramadoAt(LocalDateTime.now());
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        usuarioRepository.findByEmail(currentUserEmail).ifPresent(cita::setReprogramadoPor);

        Cita savedCita = citaRepository.save(cita);
        CitaResponse reprogramadaResponse = citaMapper.toResponse(savedCita);
        broadcastCitaEvent("REPROGRAMAR_CITA", savedCita, reprogramadaResponse);

        auditLogService.log(
            "REPROGRAMAR_CITA",
            "Citas",
            "Se reprogramó la cita para la mascota " + cita.getMascota().getNombreCompleto() + " para la nueva fecha " + fechaInicio
        );

        // Envío de correo electrónico al apoderado
        try {
            if (cita.getMascota().getApoderado() != null && cita.getMascota().getApoderado().getUser() != null) {
                String emailDestinatario = cita.getMascota().getApoderado().getUser().getEmail();
                if (emailDestinatario != null && !emailDestinatario.isBlank()) {
                    Company company = cita.getMascota().getApoderado().getUser().getCompany();
                    if (company == null && cita.getEmpleado() != null && cita.getEmpleado().getUser() != null) {
                        company = cita.getEmpleado().getUser().getCompany();
                    }

                    String companyName = company != null ? company.getName() : "VargasVet";
                    String companyLogo = (company != null && company.getLogoUrl() != null) ? company.getLogoUrl() : "";
                    String companyEmail = company != null ? company.getEmail() : "";
                    String companyPhone = company != null ? company.getPhone() : "";
                    String companyAddress = company != null ? company.getAddress() : "";

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String fechaAnteriorStr = formatter.format(originalFechaInicio);
                    String fechaNuevaStr = formatter.format(fechaInicio);

                    Map<String, Object> model = new HashMap<>();
                    model.put("nombreApoderado", cita.getMascota().getApoderado().getUser().getNombre());
                    model.put("nombreMascota", cita.getMascota().getNombreCompleto());
                    model.put("fechaAnterior", fechaAnteriorStr);
                    model.put("fechaNueva", fechaNuevaStr);
                    model.put("motivo", request.getNotas() != null && !request.getNotas().isBlank() ? request.getNotas() : "No especificado");
                    model.put("companyName", companyName);
                    model.put("companyLogo", companyLogo);
                    model.put("companyEmail", companyEmail);
                    model.put("companyPhone", companyPhone);
                    model.put("companyAddress", companyAddress);

                    Mail mail = emailService.createMail(emailDestinatario, "Cita Reprogramada - " + companyName, model);
                    emailService.sendEmail(mail, "email/cita-reprogramar-template");
                }
            }
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de reprogramación a " + cita.getMascota().getApoderado().getUser().getEmail() + ": " + e.getMessage());
        }

        return reprogramadaResponse;
    }

    private void validarPermisoEmpresa(Cita cita) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (cita.getMascota().getApoderado().getUser().getCompany() == null ||
                !cita.getMascota().getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para realizar esta acción en esta cita");
            }
        }
    }

    private DiaSemana toDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY    -> DiaSemana.LUNES;
            case TUESDAY   -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY  -> DiaSemana.JUEVES;
            case FRIDAY    -> DiaSemana.VIERNES;
            case SATURDAY  -> DiaSemana.SABADO;
            case SUNDAY    -> DiaSemana.DOMINGO;
        };
    }

    private void broadcastCitaEvent(String tipo, Cita cita, CitaResponse response) {
        Integer companyId = null;
        if (cita.getEmpleado() != null && cita.getEmpleado().getUser() != null
                && cita.getEmpleado().getUser().getCompany() != null) {
            companyId = cita.getEmpleado().getUser().getCompany().getId();
        }
        final Integer finalCompanyId = companyId;
        final CitaWsEvent event = CitaWsEvent.builder()
                .tipo(tipo)
                .cita(response)
                .companyId(finalCompanyId)
                .build();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (finalCompanyId != null) {
                    messagingTemplate.convertAndSend("/topic/citas/" + finalCompanyId, event);
                }
            } catch (Exception e) {
                System.err.println("WS ERROR: No se pudo transmitir evento de cita: " + e.getMessage());
            }
        });
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            }
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getAdminDisponibilidad(Long empleadoId, String fecha, Long servicioId) {
        LocalDate localDate = LocalDate.parse(fecha);

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        ServiciosVeterinarios servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        Integer companyId = empleado.getUser().getCompany().getId();
        int duracion = servicio.getDuracionEstimada() != null ? servicio.getDuracionEstimada() : 20;

        java.util.List<String> availableSlots = new java.util.ArrayList<>();

        boolean clinicClosed = companyExceptionRepository.findByCompanyIdAndDateString(companyId, fecha)
                .map(ex -> Boolean.FALSE.equals(ex.getIsOpen()))
                .orElse(false);
        if (clinicClosed) return availableSlots;

        java.time.DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        DiaSemana diaSemana = switch (dayOfWeek) {
            case MONDAY    -> DiaSemana.LUNES;
            case TUESDAY   -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY  -> DiaSemana.JUEVES;
            case FRIDAY    -> DiaSemana.VIERNES;
            case SATURDAY  -> DiaSemana.SABADO;
            case SUNDAY    -> DiaSemana.DOMINGO;
        };

        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, diaSemana);
        if (opHourOpt.isEmpty() || Boolean.FALSE.equals(opHourOpt.get().getIsOpen())) return availableSlots;

        LocalTime clinicOpen  = opHourOpt.get().getOpeningTime();
        LocalTime clinicClose = opHourOpt.get().getClosingTime();

        java.util.List<HorarioEmpleado> shifts = horarioEmpleadoRepository.findByEmpleadoIdAndFechaString(empleadoId, fecha);
        if (shifts.isEmpty()) return availableSlots;

        java.util.List<Cita> existingAppointments = citaRepository.findActiveByEmpleadoIdAndFechaString(empleadoId, fecha);

        final int step = Math.max(duracion, 1);

        for (HorarioEmpleado shift : shifts) {
            if (Boolean.FALSE.equals(shift.getActivo())) continue;
            LocalTime shiftStart = shift.getHoraInicio();
            LocalTime shiftEnd   = shift.getHoraFin();

            java.util.Set<LocalTime> candidates = new java.util.TreeSet<>();

            LocalTime t = shiftStart;
            while (!t.isAfter(shiftEnd)) {
                candidates.add(t);
                t = t.plusMinutes(step);
            }

            for (Cita appt : existingAppointments) {
                LocalTime endTime = appt.getFechaHoraFin().toLocalTime();
                if (!endTime.isBefore(shiftStart) && !endTime.isAfter(shiftEnd)) {
                    candidates.add(endTime);
                }
            }

            for (LocalTime slotStart2 : candidates) {
                LocalTime slotEnd = slotStart2.plusMinutes(duracion);
                if (slotEnd.isAfter(shiftEnd)) continue;
                if (slotStart2.isBefore(clinicOpen) || slotEnd.isAfter(clinicClose)) continue;

                boolean overlap = existingAppointments.stream().anyMatch(appt -> {
                    LocalTime s = appt.getFechaHoraInicio().toLocalTime();
                    LocalTime e = appt.getFechaHoraFin().toLocalTime();
                    return slotStart2.isBefore(e) && slotEnd.isAfter(s);
                });

                if (!overlap) availableSlots.add(slotStart2.toString());
            }
        }

        return availableSlots;
    }
}
