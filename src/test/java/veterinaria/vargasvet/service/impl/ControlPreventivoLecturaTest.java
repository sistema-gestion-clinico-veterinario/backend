package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ControlPreventivoLecturaTest {

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listarCalculaElEstadoVigenteSinModificarLaEntidadPersistida() {
        ControlPreventivoRepository controles = mock(ControlPreventivoRepository.class);
        MascotaRepository mascotas = mock(MascotaRepository.class);
        ControlPreventivoServiceImpl service = new ControlPreventivoServiceImpl(
                mock(TipoVacunaRepository.class), mock(TipoDesparasitanteRepository.class), controles,
                mock(RegistroVacunaRepository.class), mock(RegistroDesparasitacionRepository.class), mascotas,
                mock(ConsultaRepository.class), mock(AuditLogService.class), mock(AccesoValidator.class));

        autenticarSuperAdmin();
        Mascota mascota = new Mascota();
        mascota.setId(50L);
        ControlPreventivo control = new ControlPreventivo();
        control.setId(70L);
        control.setMascota(mascota);
        control.setTipo(TipoControlPreventivo.VACUNACION);
        control.setNombreControl("Antirrabica");
        control.setFechaRecomendada(LocalDate.now().minusDays(1));
        control.setEstado(EstadoControlPreventivo.PROGRAMADO);

        when(mascotas.findById(50L)).thenReturn(Optional.of(mascota));
        when(controles.findByMascotaIdOrderByFechaRecomendadaDesc(50L)).thenReturn(List.of(control));

        var resultado = service.listarControles(50L);

        assertThat(resultado).singleElement()
                .extracting("estado").isEqualTo(EstadoControlPreventivo.ATRASADO);
        assertThat(control.getEstado()).isEqualTo(EstadoControlPreventivo.PROGRAMADO);
        verify(controles, never()).save(any());
    }

    private void autenticarSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(1, "qa@vargasvet.test", "", authorities, null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities));
    }
}
