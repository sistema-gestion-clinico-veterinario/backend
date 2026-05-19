package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.*;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.CitaMapper;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.mapper.MascotaMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ApoderadoPortalService;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.service.HistoriaClinicaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import veterinaria.vargasvet.domain.enums.EstadoCita;

@Service
@RequiredArgsConstructor
public class ApoderadoPortalServiceImpl implements ApoderadoPortalService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final PrescripcionRepository prescripcionRepository;
    private final ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final CompanyExceptionRepository companyExceptionRepository;
    private final CitaService citaService;
    private final HistoriaClinicaService historiaClinicaService;
    private final MascotaMapper mascotaMapper;
    private final CitaMapper citaMapper;
    private final ConsultaMapper consultaMapper;

    private Apoderado getAuthenticatedApoderado() {
        String email = SecurityUtils.getCurrentUserEmail();
        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        java.util.Optional<Apoderado> apoderadoOpt = apoderadoRepository.findByUserId(user.getId());
        if (apoderadoOpt.isPresent()) {
            return apoderadoOpt.get();
        }

        // Vista previa segura para Super Admin y Admin:
        // Si el usuario autenticado es un administrador, le permitimos visualizar el portal
        // con la información del primer apoderado registrado EN SU SEDE/CLÍNICA activa.
        if (SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin()) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (companyId != null) {
                List<Apoderado> companyApoderados = apoderadoRepository.findByCompanyId(companyId);
                if (!companyApoderados.isEmpty()) {
                    return companyApoderados.get(0);
                }
            } else {
                List<Apoderado> allApoderados = apoderadoRepository.findAll();
                if (!allApoderados.isEmpty()) {
                    return allApoderados.get(0);
                }
            }
        }

        throw new ResourceNotFoundException("Perfil de propietario no encontrado para este usuario");
    }

    @Override
    @Transactional(readOnly = true)
    public ApoderadoPerfilResponse getPerfil() {
        Apoderado apoderado = getAuthenticatedApoderado();
        ApoderadoPerfilResponse response = new ApoderadoPerfilResponse();
        response.setId(apoderado.getId());
        response.setNombre(apoderado.getUser().getNombre());
        response.setApellido(apoderado.getUser().getApellido());
        response.setEmail(apoderado.getUser().getEmail());
        response.setTelefono(apoderado.getUser().getTelefono());
        response.setDireccion(apoderado.getUser().getDireccion());
        response.setNumeroDocumento(apoderado.getNumeroDocumento());
        response.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        response.setGenero(apoderado.getGenero());
        response.setReferencias(apoderado.getReferencias());
        response.setObservaciones(apoderado.getObservaciones());
        if (apoderado.getUser().getCompany() != null) {
            response.setCompanyId(apoderado.getUser().getCompany().getId());
        }

        List<MascotaResponse> mascotas = getMascotas();
        response.setMascotas(mascotas);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MascotaResponse> getMascotas() {
        Apoderado apoderado = getAuthenticatedApoderado();
        return mascotaRepository.findByApoderadoId(apoderado.getId()).stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(mascotaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MascotaResponse> getMascotasPaginated(String nombre, veterinaria.vargasvet.domain.enums.EspecieMascota especie, Boolean activo, org.springframework.data.domain.Pageable pageable) {
        Apoderado apoderado = getAuthenticatedApoderado();
        return mascotaRepository.buscarPortalMascotas(apoderado.getId(), nombre, especie, activo, pageable)
                .map(mascotaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaDetalleResponse getHistoriaMascota(Long mascotaId) {
        Apoderado apoderado = getAuthenticatedApoderado();
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));

        if (!mascota.getApoderado().getId().equals(apoderado.getId())) {
            throw new AccessDeniedException("No tienes permiso para acceder al historial de esta mascota");
        }

        return historiaClinicaService.getPorMascota(mascotaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> getCitas(Long mascotaId) {
        Apoderado apoderado = getAuthenticatedApoderado();
        List<Cita> rawCitas;
        if (mascotaId != null) {
            rawCitas = citaRepository.findByApoderadoIdAndMascotaId(apoderado.getId(), mascotaId);
        } else {
            rawCitas = citaRepository.findByApoderadoId(apoderado.getId());
        }
        return rawCitas.stream()
                .map(citaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescripcionResumenResponse> getRecetas() {
        Apoderado apoderado = getAuthenticatedApoderado();
        return prescripcionRepository.findByApoderadoId(apoderado.getId()).stream()
                .map(consultaMapper::toPrescripcionListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioResponse> getServicios() {
        Apoderado apoderado = getAuthenticatedApoderado();
        Integer companyId = apoderado.getUser().getCompany().getId();
        List<ServiciosVeterinarios> list = serviciosVeterinariosRepository.findByCompanyIdAndDisponibleTrueAndActivoTrue(companyId);

        return list.stream()
                .map(s -> {
                    ServicioResponse r = new ServicioResponse();
                    r.setId(s.getId());
                    r.setNombre(s.getNombre());
                    r.setDescripcion(s.getDescripcion());
                    r.setPrecio(s.getPrecio());
                    r.setDisponible(s.getDisponible());
                    r.setActivo(s.getActivo());
                    r.setCompanyId(s.getCompany().getId());
                    r.setCompanyName(s.getCompany().getName());
                    r.setDuracionEstimada(s.getDuracionEstimada());
                    if (s.getTipoEmpleado() != null) {
                        r.setTipoEmpleadoId(s.getTipoEmpleado().getId());
                        r.setTipoEmpleadoNombre(s.getTipoEmpleado().getNombre());
                    }
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoListResponse> getEmpleados(Long servicioId) {
        Apoderado apoderado = getAuthenticatedApoderado();
        Integer companyId = apoderado.getUser().getCompany().getId();

        Long requiredTipoEmpleadoId = null;
        if (servicioId != null) {
            ServiciosVeterinarios service = serviciosVeterinariosRepository.findById(servicioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
            if (service.getTipoEmpleado() != null) {
                requiredTipoEmpleadoId = service.getTipoEmpleado().getId();
            }
        }

        List<Empleado> employees = empleadoRepository.findActiveByCompanyIdAndTipoEmpleadoId(companyId, requiredTipoEmpleadoId);
        return employees.stream()
                .map(e -> {
                    EmpleadoListResponse r = new EmpleadoListResponse();
                    r.setId(e.getId());
                    r.setNombre(e.getUser().getNombre());
                    r.setApellido(e.getUser().getApellido());
                    r.setEmail(e.getUser().getEmail());
                    r.setTelefono(e.getUser().getTelefono());
                    r.setFotoUrl(e.getFotoUrl());
                    r.setActivo(e.getEstado());
                    if (e.getUser() != null) {
                        r.setUserId(e.getUser().getId());
                    }
                    if (e.getTiposEmpleado() != null) {
                        r.setTiposEmpleado(e.getTiposEmpleado().stream()
                                .map(t -> t.getNombre())
                                .collect(Collectors.toList()));
                    }
                    if (e.getEspecialidades() != null) {
                        r.setEspecialidades(e.getEspecialidades().stream()
                                .map(es -> es.getNombre())
                                .collect(Collectors.toList()));
                    }
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDisponibilidad(Long empleadoId, String fecha, Long servicioId) {
        LocalDate localDate = LocalDate.parse(fecha);
        Apoderado apoderado = getAuthenticatedApoderado();
        Integer companyId = apoderado.getUser().getCompany().getId();

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        ServiciosVeterinarios servicio = serviciosVeterinariosRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        int duracion = servicio.getDuracionEstimada() != null ? servicio.getDuracionEstimada() : 20;

        List<String> availableSlots = new java.util.ArrayList<>();

        // Check clinic exception
        boolean clinicClosed = companyExceptionRepository.findByCompanyIdAndDate(companyId, localDate)
                .map(ex -> Boolean.FALSE.equals(ex.getIsOpen()))
                .orElse(false);
        if (clinicClosed) {
            return availableSlots;
        }

        // Check clinic operating hours
        java.time.DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        veterinaria.vargasvet.domain.enums.DiaSemana diaSemana = switch (dayOfWeek) {
            case MONDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.LUNES;
            case TUESDAY   -> veterinaria.vargasvet.domain.enums.DiaSemana.MARTES;
            case WEDNESDAY -> veterinaria.vargasvet.domain.enums.DiaSemana.MIERCOLES;
            case THURSDAY  -> veterinaria.vargasvet.domain.enums.DiaSemana.JUEVES;
            case FRIDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.VIERNES;
            case SATURDAY  -> veterinaria.vargasvet.domain.enums.DiaSemana.SABADO;
            case SUNDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.DOMINGO;
        };

        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, diaSemana);
        if (opHourOpt.isEmpty() || Boolean.FALSE.equals(opHourOpt.get().getIsOpen())) {
            return availableSlots;
        }
        LocalTime clinicOpen = opHourOpt.get().getOpeningTime();
        LocalTime clinicClose = opHourOpt.get().getClosingTime();

        // Fetch employee shifts for this date
        List<HorarioEmpleado> shifts = horarioEmpleadoRepository.findByEmpleadoIdAndFecha(empleadoId, localDate);
        if (shifts.isEmpty()) {
            return availableSlots;
        }

        // Fetch existing appointments for the employee on this date
        List<Cita> existingAppointments = citaRepository.findActiveByEmpleadoIdAndFecha(empleadoId, localDate);

        // Calculate time-slot interval (20 minutes step)
        int slotStepMinutes = 20;

        // Minimum 2 hours lead time for same-day booking
        LocalTime minAllowedTime = LocalTime.MIN;
        if (localDate.equals(LocalDate.now())) {
            minAllowedTime = LocalTime.now().plusHours(2);
        }

        for (HorarioEmpleado shift : shifts) {
            if (Boolean.FALSE.equals(shift.getActivo())) continue;

            LocalTime current = shift.getHoraInicio();
            LocalTime shiftEnd = shift.getHoraFin();

            while (current.plusMinutes(duracion).isBefore(shiftEnd) || current.plusMinutes(duracion).equals(shiftEnd)) {
                LocalTime slotStart = current;
                LocalTime slotEnd = current.plusMinutes(duracion);

                // Ensure slot is within clinic hours
                if ((slotStart.isAfter(clinicOpen) || slotStart.equals(clinicOpen)) &&
                    (slotEnd.isBefore(clinicClose) || slotEnd.equals(clinicClose))) {

                    // Check lead time constraint
                    if (!localDate.equals(LocalDate.now()) || slotStart.isAfter(minAllowedTime)) {

                        // Check overlap with existing appointments
                        boolean overlap = false;
                        for (Cita appt : existingAppointments) {
                            LocalTime apptStart = appt.getFechaHoraInicio().toLocalTime();
                            LocalTime apptEnd = appt.getFechaHoraFin().toLocalTime();

                            if (slotStart.isBefore(apptEnd) && slotEnd.isAfter(apptStart)) {
                                overlap = true;
                                break;
                            }
                        }

                        if (!overlap) {
                            availableSlots.add(slotStart.toString());
                        }
                    }
                }

                current = current.plusMinutes(slotStepMinutes);
            }
        }

        return availableSlots;
    }

    @Override
    @Transactional
    public CitaResponse createPortalCita(CitaRequest request) {
        Apoderado apoderado = getAuthenticatedApoderado();
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        if (!mascota.getApoderado().getId().equals(apoderado.getId())) {
            throw new AccessDeniedException("No tienes permiso para programar citas para esta mascota");
        }

        // Limit validations for clients (apoderados)
        LocalDate bookingDate = request.getFechaHoraInicio().toLocalDate();
        List<Cita> todayAppointments = citaRepository.findActiveByApoderadoIdAndFecha(apoderado.getId(), bookingDate);

        // Limit 1: Max 2 appointments for the same pet on a single day
        long samePetCount = todayAppointments.stream()
                .filter(c -> c.getMascota().getId().equals(request.getMascotaId()))
                .count();
        if (samePetCount >= 2) {
            throw new IllegalArgumentException("No puedes registrar más de 2 citas para la misma mascota en un mismo día.");
        }

        // Limit 2: Max 3 appointments in total for the tutor on a single day
        if (todayAppointments.size() >= 3) {
            throw new IllegalArgumentException("No puedes registrar más de 3 citas en total para un mismo día.");
        }

        return citaService.createCita(request);
    }

    @Override
    @Transactional
    public CitaResponse updatePortalCita(Long id, CitaRequest request) {
        Apoderado apoderado = getAuthenticatedApoderado();
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con el ID: " + id));

        if (!cita.getMascota().getApoderado().getId().equals(apoderado.getId())) {
            throw new AccessDeniedException("No tienes permiso para actualizar esta cita");
        }

        if (cita.getEstado() == EstadoCita.COMPLETADA || cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.EN_PROCESO) {
            throw new IllegalArgumentException("No se puede modificar una cita que ya se encuentra " + cita.getEstado());
        }

        // Validate that the new pet belongs to the apoderado
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con el ID: " + request.getMascotaId()));
        if (!mascota.getApoderado().getId().equals(apoderado.getId())) {
            throw new AccessDeniedException("No tienes permiso para registrar citas para esta mascota");
        }

        // The apoderado cannot change the date/time in this standard details update action. Preserve original date/time.
        request.setFechaHoraInicio(cita.getFechaHoraInicio());
        request.setEsEmergencia(cita.getEsEmergencia());

        // Validate availability of the new professional/service on the original date and time slot
        checkAvailability(request.getVeterinarioId(), cita.getFechaHoraInicio(), request.getServicioId(), id);

        return citaService.actualizarCita(id, request);
    }

    @Override
    @Transactional
    public CitaResponse reschedulePortalCita(Long id, CitaRequest request) {
        Apoderado apoderado = getAuthenticatedApoderado();
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con el ID: " + id));

        if (!cita.getMascota().getApoderado().getId().equals(apoderado.getId())) {
            throw new AccessDeniedException("No tienes permiso para reprogramar esta cita");
        }

        // Rule: Only Scheduled, Canceled, or Rescheduled states can be rescheduled
        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CANCELADA && cita.getEstado() != EstadoCita.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo se pueden reprogramar citas en estado PROGRAMADA, CANCELADA o REPROGRAMADA");
        }

        // Rule: Can only reschedule up to 6 hours before the original appointment start time
        if (LocalDateTime.now().isAfter(cita.getFechaHoraInicio().minusHours(6))) {
            throw new IllegalArgumentException("No se puede reprogramar la cita con menos de 6 horas de anticipación.");
        }

        // Validate availability of the professional in the new requested date and time slot
        checkAvailability(request.getVeterinarioId(), request.getFechaHoraInicio(), cita.getServicio().getId(), id);

        return citaService.reprogramarCita(id, request);
    }

    private void checkAvailability(Long empleadoId, LocalDateTime dateTime, Long servicioId, Long excludeCitaId) {
        LocalDate localDate = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        Apoderado apoderado = getAuthenticatedApoderado();
        Integer companyId = apoderado.getUser().getCompany().getId();

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con el ID: " + empleadoId));

        ServiciosVeterinarios servicio = serviciosVeterinariosRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con el ID: " + servicioId));

        int duracion = servicio.getDuracionEstimada() != null ? servicio.getDuracionEstimada() : 20;
        LocalTime slotStart = time;
        LocalTime slotEnd = time.plusMinutes(duracion);

        // 1. Validate holidays / exceptions
        boolean clinicClosed = companyExceptionRepository.findByCompanyIdAndDate(companyId, localDate)
                .map(ex -> Boolean.FALSE.equals(ex.getIsOpen()))
                .orElse(false);
        if (clinicClosed) {
            throw new IllegalArgumentException("La clínica veterinaria está cerrada en la fecha seleccionada.");
        }

        // 2. Validate clinic master operating hours
        java.time.DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        veterinaria.vargasvet.domain.enums.DiaSemana diaSemana = switch (dayOfWeek) {
            case MONDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.LUNES;
            case TUESDAY   -> veterinaria.vargasvet.domain.enums.DiaSemana.MARTES;
            case WEDNESDAY -> veterinaria.vargasvet.domain.enums.DiaSemana.MIERCOLES;
            case THURSDAY  -> veterinaria.vargasvet.domain.enums.DiaSemana.JUEVES;
            case FRIDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.VIERNES;
            case SATURDAY  -> veterinaria.vargasvet.domain.enums.DiaSemana.SABADO;
            case SUNDAY    -> veterinaria.vargasvet.domain.enums.DiaSemana.DOMINGO;
        };

        var opHourOpt = companyOperatingHourRepository.findByCompanyIdAndDiaSemana(companyId, diaSemana);
        if (opHourOpt.isEmpty() || Boolean.FALSE.equals(opHourOpt.get().getIsOpen())) {
            throw new IllegalArgumentException("La clínica no está abierta el día " + diaSemana.toString().toLowerCase());
        }
        LocalTime clinicOpen = opHourOpt.get().getOpeningTime();
        LocalTime clinicClose = opHourOpt.get().getClosingTime();

        if (slotStart.isBefore(clinicOpen) || slotEnd.isAfter(clinicClose)) {
            throw new IllegalArgumentException("La franja horaria seleccionada está fuera del horario de atención de la clínica (" + clinicOpen + " - " + clinicClose + ").");
        }

        // 3. Validate veterinarian shifts
        List<HorarioEmpleado> shifts = horarioEmpleadoRepository.findByEmpleadoIdAndFecha(empleadoId, localDate);
        boolean inShift = false;
        for (HorarioEmpleado shift : shifts) {
            if (Boolean.TRUE.equals(shift.getActivo()) &&
                (slotStart.equals(shift.getHoraInicio()) || slotStart.isAfter(shift.getHoraInicio())) &&
                (slotEnd.equals(shift.getHoraFin()) || slotEnd.isBefore(shift.getHoraFin()))) {
                inShift = true;
                break;
            }
        }
        if (!inShift) {
            throw new IllegalArgumentException("El profesional seleccionado no tiene un turno programado para esta franja horaria.");
        }

        // 4. Validate overlapping appointments (excluding current appointment)
        List<Cita> existingAppointments = citaRepository.findActiveByEmpleadoIdAndFecha(empleadoId, localDate);
        for (Cita appt : existingAppointments) {
            if (excludeCitaId != null && appt.getId().equals(excludeCitaId)) {
                continue;
            }
            LocalTime apptStart = appt.getFechaHoraInicio().toLocalTime();
            LocalTime apptEnd = appt.getFechaHoraFin().toLocalTime();

            if (slotStart.isBefore(apptEnd) && slotEnd.isAfter(apptStart)) {
                throw new IllegalArgumentException("El profesional seleccionado ya tiene otra cita reservada en esta franja horaria.");
            }
        }
    }

    @Override
    @Transactional
    public void cancelPortalCita(Long id, String motivo) {
        throw new IllegalArgumentException("Un apoderado no tiene permiso para cancelar citas");
    }
}

