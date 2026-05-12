package veterinaria.vargasvet.modules.citas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.company.domain.entity.Company;
import veterinaria.vargasvet.modules.pagos.domain.entity.Purchase;
import veterinaria.vargasvet.modules.users.domain.entity.Empleado;
import veterinaria.vargasvet.modules.users.domain.entity.Usuario;
import veterinaria.vargasvet.modules.clinical.domain.entity.Consulta;
import veterinaria.vargasvet.modules.clinical.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.modules.inventory.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.modules.users.domain.enums.DiaSemana;
import veterinaria.vargasvet.modules.users.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.modules.citas.domain.enums.EstadoCita;
import veterinaria.vargasvet.modules.clinical.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.modules.clinical.domain.enums.TipoConsulta;
import veterinaria.vargasvet.modules.citas.dto.CitaRequest;
import veterinaria.vargasvet.modules.citas.dto.CitaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.citas.mapper.CitaMapper;
import veterinaria.vargasvet.modules.company.repository.CompanyRepository;
import veterinaria.vargasvet.modules.pagos.repository.PurchaseRepository;
import veterinaria.vargasvet.modules.users.repository.EmpleadoRepository;
import veterinaria.vargasvet.modules.users.repository.UsuarioRepository;
import veterinaria.vargasvet.modules.clinical.repository.ConsultaRepository;
import veterinaria.vargasvet.modules.clinical.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.modules.inventory.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.modules.citas.repository.CitaRepository;
import veterinaria.vargasvet.modules.citas.domain.entity.Cita;
import veterinaria.vargasvet.modules.mascotas.domain.entity.Mascota;
import veterinaria.vargasvet.modules.mascotas.repository.MascotaRepository;
import veterinaria.vargasvet.modules.users.security.SecurityUtils;


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
    private final CitaMapper citaMapper;

    private static final int DURACION_ESTIMADA_MINUTOS = 20;

    @Override
    @Transactional
    public CitaResponse createCita(CitaRequest request) {
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        if (!mascota.getActivo()) {
            throw new IllegalArgumentException("No se puede crear una cita para una mascota inactiva");
        }

        Empleado veterinario = empleadoRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado con ID: " + request.getVeterinarioId()));

        if (veterinario.getUser() == null || !veterinario.getUser().isActivo()) {
            throw new IllegalArgumentException("No se puede asignar la cita a un veterinario inactivo");
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
        if (!esEmergencia && company != null && company.getOpeningTime() != null && company.getClosingTime() != null) {
            if (horaInicio.isBefore(company.getOpeningTime()) || horaFinCita.isAfter(company.getClosingTime())) {
                throw new IllegalArgumentException(
                        "La cita está fuera del horario de atención de la clínica (" + company.getName() + "). " +
                        "Atiende de " + company.getOpeningTime() + " a " + company.getClosingTime());
            }
        }

        if (!esEmergencia) {
            HorarioEmpleado horario = veterinario.getHorarios().stream()
                    .filter(h -> h.getDiaSemana() == diaSemana && Boolean.TRUE.equals(h.getActivo()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El veterinario no atiende los días " + diaSemana.name().charAt(0) +
                            diaSemana.name().substring(1).toLowerCase()));

            if (horaInicio.isBefore(horario.getHoraInicio()) || horaFinCita.isAfter(horario.getHoraFin())) {
                throw new IllegalArgumentException(
                        "La cita está fuera del horario del veterinario. Atiende de " +
                        horario.getHoraInicio() + " a " + horario.getHoraFin());
            }
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
        return citaMapper.toResponse(savedCita);
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

        cita.setEstado(EstadoCita.EN_PROCESO);
        citaRepository.save(cita);

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

        if (cita.getEstado() == EstadoCita.COMPLETADA || cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalArgumentException("No se puede cancelar una cita que ya está " + cita.getEstado());
        }

        if (cita.getEstado() == EstadoCita.EN_PROCESO) {
            throw new IllegalArgumentException("No se puede cancelar una cita que ya está en proceso médico");
        }

        // Regla: No se puede cancelar faltando menos de 1 hora
        if (LocalDateTime.now().isAfter(cita.getFechaHoraInicio().minusHours(1))) {
            throw new IllegalArgumentException("No se puede cancelar la cita faltando menos de 1 hora para su inicio");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(motivo);
        citaRepository.save(cita);
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

        cita.setEliminada(true);
        cita.setEliminadoAt(LocalDateTime.now());
        
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        usuarioRepository.findByEmail(currentUserEmail).ifPresent(cita::setEliminadoPor);
        
        citaRepository.save(cita);
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
        return citaMapper.toResponse(updatedCita);
    }

    @Override
    @Transactional
    public CitaResponse reprogramarCita(Long id, CitaRequest request) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        validarPermisoEmpresa(cita);

        // Regla: Solo Programada o Cancelada
        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CANCELADA && cita.getEstado() != EstadoCita.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo se pueden reprogramar citas en estado Programada, Cancelada o Reprogramada");
        }

        // Regla: 1 hora antes si está programada
        if (cita.getEstado() == EstadoCita.PROGRAMADA || cita.getEstado() == EstadoCita.REPROGRAMADA) {
            if (LocalDateTime.now().isAfter(cita.getFechaHoraInicio().minusHours(1))) {
                throw new IllegalArgumentException("No se puede reprogramar una cita con menos de 1 hora de anticipación");
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
        return citaMapper.toResponse(savedCita);
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

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            }
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }
}
