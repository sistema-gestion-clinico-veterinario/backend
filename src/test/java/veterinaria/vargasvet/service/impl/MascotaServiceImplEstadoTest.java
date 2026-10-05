package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.dto.request.EstadoMascotaRequest;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.PetOwnershipService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cambio manual de estado de una mascota: no se reactiva si nadie activo puede autorizar su atención, y
 * los motivos que el sistema asigna al suspender o dar de baja al propietario no se pueden elegir a mano.
 */
@ExtendWith(MockitoExtension.class)
class MascotaServiceImplEstadoTest {

    private static final int COMPANY_ID = 7;

    @Mock MascotaRepository mascotaRepository;
    @Mock CitaRepository citaRepository;
    @Mock AuditLogService auditLogService;
    @Mock PetOwnershipService petOwnershipService;
    @InjectMocks MascotaServiceImpl service;

    private Mascota mascota;

    @BeforeEach
    void setUp() {
        mascota = new Mascota();
        mascota.setId(5L);
        mascota.setNombreCompleto("Luna");
        mascota.setActivo(true);
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "recepcion@empresa.test", "", List.of(), COMPANY_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        org.mockito.Mockito.lenient().when(mascotaRepository.findByIdAndCompanyId(5L, COMPANY_ID))
                .thenReturn(Optional.of(mascota));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private EstadoMascotaRequest request(boolean activo, MotivoBajaMascota motivo) {
        EstadoMascotaRequest request = new EstadoMascotaRequest();
        request.setActive(activo);
        request.setMotivoBaja(motivo);
        return request;
    }

    @Test
    void elPersonalPuedeDarDeBajaConUnMotivoPropio() {
        service.cambiarEstado(5L, request(false, MotivoBajaMascota.FALLECIMIENTO));

        assertThat(mascota.getActivo()).isFalse();
        assertThat(mascota.getMotivoBaja()).isEqualTo(MotivoBajaMascota.FALLECIMIENTO);
    }

    @Test
    void losMotivosAutomaticosNoSePuedenElegirAMano() {
        assertThatThrownBy(() -> service.cambiarEstado(5L, request(false, MotivoBajaMascota.BAJA_DEL_PROPIETARIO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solo lo asigna el sistema");
        assertThatThrownBy(() -> service.cambiarEstado(5L, request(false, MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(mascota.getActivo()).isTrue();
        verify(mascotaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reactivarExigeQueAlguienActivoPuedaAutorizarSuAtencion() {
        mascota.setActivo(false);
        mascota.setMotivoBaja(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);
        doThrow(new IllegalArgumentException("No se puede reactivar la mascota porque no tiene un propietario activo"))
                .when(petOwnershipService).assertCanBeReactivated(mascota);

        assertThatThrownBy(() -> service.cambiarEstado(5L, request(true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propietario activo");

        assertThat(mascota.getActivo()).isFalse();
        assertThat(mascota.getMotivoBaja()).isEqualTo(MotivoBajaMascota.BAJA_DEL_PROPIETARIO);
        verify(mascotaRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void reactivarConUnAutorizadorActivoLimpiaElMotivo() {
        mascota.setActivo(false);
        mascota.setMotivoBaja(MotivoBajaMascota.DEJA_ASISTIR);

        service.cambiarEstado(5L, request(true, null));

        verify(petOwnershipService).assertCanBeReactivated(mascota);
        assertThat(mascota.getActivo()).isTrue();
        assertThat(mascota.getMotivoBaja()).isNull();
    }
}
