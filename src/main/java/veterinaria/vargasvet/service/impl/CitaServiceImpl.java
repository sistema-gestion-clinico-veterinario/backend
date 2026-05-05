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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.mapper.CitaMapper;

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

        LocalDateTime fechaInicio = request.getFechaHoraInicio();
        LocalDateTime fechaFin = fechaInicio.plusMinutes(DURACION_ESTIMADA_MINUTOS);

        DiaSemana diaSemana = toDiaSemana(fechaInicio.getDayOfWeek());
        LocalTime horaInicio = fechaInicio.toLocalTime();
        LocalTime horaFinCita = fechaFin.toLocalTime();

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
        cita.setDuracionMinutos(DURACION_ESTIMADA_MINUTOS);
        cita.setMotivoCita(request.getMotivoCita());
        cita.setNotas(request.getNotas());
        cita.setEstado(EstadoCita.PROGRAMADA);
        
        // Atributos obligatorios en la entidad Cita
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setTotalServicio(BigDecimal.ZERO);

        if (request.getServicioId() != null) {
            ServiciosVeterinarios servicio = servicioRepository.findById(request.getServicioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + request.getServicioId()));
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
            // Si por alguna razón no tiene consulta pero está en proceso, intentamos buscarla
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
        return citaRepository.buscar(resolvedCompanyId, fecha, estado, veterinarioId,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(citaMapper::toResponse);
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
