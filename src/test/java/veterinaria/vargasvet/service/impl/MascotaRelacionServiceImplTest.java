package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.dto.request.MascotaRelacionRequest;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaRelacionServiceImplTest {

    @Mock MascotaPersonaRelacionRepository relacionRepository;
    @Mock MascotaRepository mascotaRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock AuditLogService auditLogService;
    @Mock veterinaria.vargasvet.service.PetOwnershipService petOwnershipService;
    @Mock veterinaria.vargasvet.service.PetLinkNotifier petLinkNotifier;
    @Mock veterinaria.vargasvet.service.ApoderadoService apoderadoService;

    private MascotaRelacionServiceImpl service;
    private Mascota mascota;
    private Apoderado principal;
    private Apoderado persona;

    @BeforeEach
    void setUp() {
        service = new MascotaRelacionServiceImpl(
                relacionRepository, mascotaRepository, apoderadoRepository, auditLogService,
                petOwnershipService, petLinkNotifier, apoderadoService);

        Company company = new Company();
        company.setId(7);
        company.setName("Clínica prueba");

        principal = apoderado(1L, company, "Ana", "Principal");
        persona = apoderado(2L, company, "Carlos", "Cuidador");

        mascota = new Mascota();
        mascota.setId(10L);
        mascota.setUuid("mascota-uuid");
        mascota.setNombreCompleto("Max");
        mascota.setApoderado(principal);

        UsuarioPrincipal usuarioPrincipal = new UsuarioPrincipal(
                15, "empleado@clinica.test", "", List.of(), 7);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        usuarioPrincipal, null, usuarioPrincipal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearCopropietarioConcedeLasTresAutorizacionesPorDefinicion() {
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(apoderadoRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(persona));
        when(relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(10L, 2L)).thenReturn(List.of());
        when(relacionRepository.save(any())).thenAnswer(invocation -> {
            MascotaPersonaRelacion relacion = invocation.getArgument(0);
            relacion.setUuid("relacion-uuid");
            relacion.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            relacion.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
            return relacion;
        });

        MascotaRelacionRequest request = request(TipoRelacionMascota.COPROPIETARIO);
        request.setPuedeRecibirInformacion(false);
        request.setPuedeAutorizarAtencion(false);
        request.setPuedeRealizarPagos(false);

        var response = service.crear("mascota-uuid", request);

        assertTrue(response.getPuedeRecibirInformacion());
        assertTrue(response.getPuedeAutorizarAtencion());
        assertTrue(response.getPuedeRealizarPagos());
    }

    @Test
    void representanteDebeTenerAlMenosUnaAutorizacion() {
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(apoderadoRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(persona));
        when(relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(10L, 2L)).thenReturn(List.of());

        MascotaRelacionRequest request = request(TipoRelacionMascota.REPRESENTANTE_AUTORIZADO);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crear("mascota-uuid", request));

        assertEquals("Seleccione al menos una autorización para el representante", error.getMessage());
        verify(relacionRepository, never()).save(any());
    }

    @Test
    void noBuscaNiVinculaPersonasFueraDeLaEmpresaActual() {
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(apoderadoRepository.findByIdAndCompanyId(99L, 7)).thenReturn(Optional.empty());
        MascotaRelacionRequest request = request(TipoRelacionMascota.RESPONSABLE_PAGO);
        request.setApoderadoId(99L);

        assertThrows(veterinaria.vargasvet.exception.ResourceNotFoundException.class,
                () -> service.crear("mascota-uuid", request));

        verify(apoderadoRepository, never()).findById(99L);
        verify(relacionRepository, never()).save(any());
    }

    @Test
    void responsablePagoNoRecibeInformacionNiAutorizaAtencion() {
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(apoderadoRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(persona));
        when(relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(10L, 2L)).thenReturn(List.of());
        when(relacionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MascotaRelacionRequest request = request(TipoRelacionMascota.RESPONSABLE_PAGO);
        request.setPuedeRecibirInformacion(true);
        request.setPuedeAutorizarAtencion(true);

        service.crear("mascota-uuid", request);

        ArgumentCaptor<MascotaPersonaRelacion> captor = ArgumentCaptor.forClass(MascotaPersonaRelacion.class);
        verify(relacionRepository).save(captor.capture());
        assertFalse(captor.getValue().getPuedeRecibirInformacion());
        assertFalse(captor.getValue().getPuedeAutorizarAtencion());
        assertTrue(captor.getValue().getPuedeRealizarPagos());
    }

    @Test
    void alVincularSeReevaluaLaMascotaYSeDevuelveElAviso() {
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(apoderadoRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(persona));
        when(relacionRepository.findAllByMascotaIdAndApoderadoIdAndActivoTrue(10L, 2L)).thenReturn(List.of());
        when(relacionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(petOwnershipService.reevaluate(mascota)).thenReturn("La mascota Max se reactivó");

        var response = service.crear("mascota-uuid", request(TipoRelacionMascota.COPROPIETARIO));

        assertEquals("La mascota Max se reactivó", response.getAvisoMascota());
    }

    @Test
    void alRevocarSeReevaluaLaMascotaYSeDevuelveElAviso() {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setUuid("relacion-uuid");
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setTipoRelacion(TipoRelacionMascota.COPROPIETARIO);
        relacion.setFechaInicio(java.time.LocalDate.now().minusDays(1));
        relacion.setActivo(true);
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(relacionRepository.findByUuidAndCompanyId("relacion-uuid", 7)).thenReturn(Optional.of(relacion));
        when(petOwnershipService.reevaluate(mascota)).thenReturn("La mascota Max quedó inactiva");

        String aviso = service.revocar("mascota-uuid", "relacion-uuid");

        assertEquals("La mascota Max quedó inactiva", aviso);
        assertFalse(relacion.getActivo());
        verify(petOwnershipService).reevaluate(mascota);
    }

    @Test
    void revocarUnaRelacionYaRevocadaNoReevaluaNada() {
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setUuid("relacion-uuid");
        relacion.setMascota(mascota);
        relacion.setApoderado(persona);
        relacion.setTipoRelacion(TipoRelacionMascota.COPROPIETARIO);
        relacion.setFechaInicio(java.time.LocalDate.now().minusDays(1));
        relacion.setActivo(false);
        when(mascotaRepository.findByUuidAndCompanyId("mascota-uuid", 7)).thenReturn(Optional.of(mascota));
        when(relacionRepository.findByUuidAndCompanyId("relacion-uuid", 7)).thenReturn(Optional.of(relacion));

        assertNull(service.revocar("mascota-uuid", "relacion-uuid"));
        verify(petOwnershipService, never()).reevaluate(any());
    }

    private MascotaRelacionRequest request(TipoRelacionMascota tipo) {
        MascotaRelacionRequest request = new MascotaRelacionRequest();
        request.setApoderadoId(2L);
        request.setTipoRelacion(tipo);
        request.setPuedeRecibirInformacion(false);
        request.setPuedeAutorizarAtencion(false);
        request.setPuedeRealizarPagos(false);
        return request;
    }

    private Apoderado apoderado(Long id, Company company, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setCompany(company);
        apoderado.setUser(usuario);
        apoderado.setEstado(true);
        apoderado.setNumeroDocumento("DOC-" + id);
        return apoderado;
    }
}
