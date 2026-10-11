package veterinaria.vargasvet.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerVentanasTest {

    private final VentanaRepository ventanaRepository = mock(VentanaRepository.class);
    private final DataInitializer inicializador = new DataInitializer(
            mock(RoleRepository.class), mock(VistaRepository.class), ventanaRepository,
            mock(RolVistaPermisoRepository.class), mock(CompanyRepository.class),
            mock(CompanyRoleProvisioningService.class));

    private void sembrar(String codigo, String nombre, String grupo, int orden) {
        ReflectionTestUtils.invokeMethod(inicializador, "createVentanaIfNotExists", codigo, nombre, grupo, orden);
    }

    @Test
    void unModuloQueNoExisteSeCreaConSusValoresIniciales() {
        when(ventanaRepository.findByCodigo("CLINICA")).thenReturn(null);

        sembrar("CLINICA", "Clínica", "clinica", 4);

        var captor = org.mockito.ArgumentCaptor.forClass(Ventana.class);
        verify(ventanaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Clínica");
        assertThat(captor.getValue().getOrden()).isEqualTo(4);
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(captor.getValue().getPresentacionDefault()).isEqualTo(MenuPresentation.GROUPED);
    }

    @Test
    void elPortalDelClienteSeCreaConPresentacionPlana() {
        when(ventanaRepository.findByCodigo("PORTAL_APODERADO")).thenReturn(null);

        sembrar("PORTAL_APODERADO", "Portal Apoderado", "apoderado", 5);

        var captor = org.mockito.ArgumentCaptor.forClass(Ventana.class);
        verify(ventanaRepository).save(captor.capture());
        assertThat(captor.getValue().getPresentacionDefault()).isEqualTo(MenuPresentation.FLAT);
    }

    @Test
    void alReiniciarNoSeSobrescribeLoQueLaPlataformaConfiguro() {
        Ventana configurada = new Ventana();
        configurada.setCodigo("CLINICA");
        configurada.setNombre("Atención clínica");
        configurada.setGrupo("personalizado");
        configurada.setOrden(9);
        configurada.setActivo(false);
        configurada.setPresentacionDefault(MenuPresentation.FLAT);
        when(ventanaRepository.findByCodigo("CLINICA")).thenReturn(configurada);

        sembrar("CLINICA", "Clínica", "clinica", 4);

        verify(ventanaRepository, never()).save(any());
        assertThat(configurada.getNombre()).isEqualTo("Atención clínica");
        assertThat(configurada.getOrden()).isEqualTo(9);
        assertThat(configurada.isActivo()).isFalse();
        assertThat(configurada.getPresentacionDefault()).isEqualTo(MenuPresentation.FLAT);
    }
}
