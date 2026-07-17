package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceUnitTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ConsultaMapper consultaMapper;

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateConsulta_lanzaResourceNotFoundSiConsultaNoExiste() {
        // Arrange
        ConsultaServiceImpl service = service();
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(1L);
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> service.updateConsulta(99L, request));

        // Assert
        assertEquals(ResourceNotFoundException.class, ex.getClass());
    }

    @Test
    void updateConsulta_lanzaExcepcionCuandoVersionNoCoincide() {
        // Arrange
        ConsultaServiceImpl service = service();
        Consulta consulta = consultaAbiertaCompleta();
        consulta.setVersion(2L);
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(1L);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConsulta(10L, request)
        );

        // Assert
        assertEquals("La consulta ha sido modificada por otro usuario. Por favor, refresque la página.", ex.getMessage());
        verify(consultaRepository, never()).saveAndFlush(any(Consulta.class));
    }

    @Test
    void updateConsulta_actualizaPesoDeMascotaYAntecedentes() {
        // Arrange
        autenticarSuperAdmin();
        ConsultaServiceImpl service = service();
        Consulta consulta = consultaAbiertaCompleta();
        ConsultaRequest request = new ConsultaRequest();
        request.setVersion(1L);
        request.setPesoEnConsulta(12.4);
        request.setAntecedentesEnfermedades("Alergia previa");
        ConsultaResponse mapped = new ConsultaResponse();

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.saveAndFlush(consulta)).thenReturn(consulta);
        when(consultaMapper.toResponse(consulta)).thenReturn(mapped);

        // Act
        ConsultaResponse response = service.updateConsulta(10L, request);

        // Assert
        assertEquals(mapped, response);
        assertEquals(12.4, consulta.getPesoEnConsulta());
        assertEquals(12.4, consulta.getHistoriaClinica().getMascota().getPeso());
        assertEquals("Alergia previa", consulta.getHistoriaClinica().getEnfermedades());
        verify(mascotaRepository).save(consulta.getHistoriaClinica().getMascota());
        verify(historiaClinicaRepository).save(consulta.getHistoriaClinica());
    }

    @Test
    void cerrarConsulta_lanzaExcepcionSiFaltanCamposObligatorios() {
        // Arrange
        autenticarSuperAdmin();
        ConsultaServiceImpl service = service();
        Consulta consulta = consultaAbiertaCompleta();
        consulta.setAnamnesis(" ");
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(1L);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.cerrarConsulta(10L, request)
        );

        // Assert
        assertEquals("La anamnesis es obligatoria para cerrar la consulta", ex.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
        verify(consultaRepository, never()).saveAndFlush(any(Consulta.class));
    }

    @Test
    void cerrarConsulta_cierraConsultaYCompletaCitaAsociada() {
        // Arrange
        autenticarSuperAdmin();
        ConsultaServiceImpl service = service();
        Consulta consulta = consultaAbiertaCompleta();
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(1L);
        ConsultaResponse mapped = new ConsultaResponse();

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.saveAndFlush(consulta)).thenReturn(consulta);
        when(consultaMapper.toResponse(consulta)).thenReturn(mapped);

        // Act
        ConsultaResponse response = service.cerrarConsulta(10L, request);

        // Assert
        assertEquals(mapped, response);
        assertEquals(EstadoConsulta.CERRADA, consulta.getEstado());
        assertEquals(EstadoCita.COMPLETADA, consulta.getCita().getEstado());
        assertEquals("doctor@vargasvet.test", consulta.getCerradoPor());
        assertNotNull(consulta.getFechaCierre());
        verify(citaRepository).save(consulta.getCita());
    }

    private ConsultaServiceImpl service() {
        return new ConsultaServiceImpl(
                consultaRepository,
                historiaClinicaRepository,
                mascotaRepository,
                citaRepository,
                consultaMapper,
                auditLogService
        );
    }

    private void autenticarSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "doctor@vargasvet.test",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );
    }

    private Consulta consultaAbiertaCompleta() {
        Company company = new Company();
        company.setId(3);

        Usuario apoderadoUser = new Usuario();
        apoderadoUser.setCompany(company);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setPeso(10.0);
        mascota.setApoderado(apoderado);

        HistoriaClinica historiaClinica = new HistoriaClinica();
        historiaClinica.setId(5L);
        historiaClinica.setNumeroHc("HC-000005");
        historiaClinica.setMascota(mascota);

        Usuario vetUser = new Usuario();
        vetUser.setNombre("Ana");
        vetUser.setApellido("Vet");

        Empleado veterinario = new Empleado();
        veterinario.setUser(vetUser);

        Cita cita = new Cita();
        cita.setId(20L);
        cita.setEstado(EstadoCita.EN_PROCESO);

        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setVersion(1L);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setHistoriaClinica(historiaClinica);
        consulta.setCita(cita);
        consulta.setVeterinario(veterinario);
        consulta.setFechaConsulta(LocalDateTime.of(2026, 7, 7, 10, 0));
        consulta.setMotivoConsulta("Control general");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta.setPesoEnConsulta(10.0);
        consulta.setAnamnesis("Paciente estable");
        return consulta;
    }
}
