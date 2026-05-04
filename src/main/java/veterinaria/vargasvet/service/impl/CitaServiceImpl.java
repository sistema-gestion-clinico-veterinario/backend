package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.CitaMapper;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CitaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo se pueden iniciar citas que estén Programadas o Reprogramadas");
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
        
        Consulta savedConsulta = consultaRepository.save(consulta);
        
        return savedConsulta.getId();
    }
}
