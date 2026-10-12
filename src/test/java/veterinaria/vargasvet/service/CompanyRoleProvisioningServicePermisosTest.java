package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyRoleProvisioningServicePermisosTest {

    @Mock RoleRepository roleRepository;
    @Mock VistaRepository vistaRepository;
    @Mock RolVistaPermisoRepository rolVistaPermisoRepository;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock RolVentanaPermisoRepository rolVentanaPermisoRepository;

    private final List<RolVistaPermiso> guardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Vista misCitas = vista("VISTA_MIS_CITAS", ViewAudience.CLIENT);
        Vista misMascotas = vista("VISTA_MIS_MASCOTAS", ViewAudience.CLIENT);
        Vista empleados = vista("VISTA_EMPLEADOS", ViewAudience.STAFF);
        when(vistaRepository.findByActivoTrue()).thenReturn(List.of(misCitas, misMascotas, empleados));
        lenient().when(roleRepository.findFirstByCompanyIsNullAndPurpose(any())).thenReturn(Optional.empty());
        lenient().when(roleRepository.findFirstByCompanyIdAndPurpose(any(), any())).thenReturn(Optional.empty());
        lenient().when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            if (role.getId() == null) {
                role.setId(role.getPurpose() == RolePurpose.CLIENT_PORTAL ? 2 : 1);
            }
            return role;
        });
        lenient().when(rolVistaPermisoRepository.findByRolId(any())).thenReturn(List.of());
        lenient().when(rolVistaPermisoRepository.save(any(RolVistaPermiso.class))).thenAnswer(invocation -> {
            guardados.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
    }

    private Vista vista(String codigo, ViewAudience audiencia) {
        Vista vista = new Vista();
        vista.setCodigo(codigo);
        vista.setAudience(audiencia);
        vista.setActivo(true);
        return vista;
    }

    private RolVistaPermiso permisoDe(RolePurpose proposito, String vista) {
        return guardados.stream()
                .filter(p -> p.getRol().getPurpose() == proposito && p.getVista().getCodigo().equals(vista))
                .findFirst().orElseThrow();
    }

    private void aprovisionar() {
        Company empresa = new Company();
        empresa.setId(7);
        new CompanyRoleProvisioningService(roleRepository, vistaRepository, rolVistaPermisoRepository,
                usuarioPorRolRepository, rolVentanaPermisoRepository).ensureRequiredRoles(empresa);
    }

    @Test
    void elClienteAgendaYReprogramaYCancelaSusCitasSinPoderEliminarlas() {
        aprovisionar();

        RolVistaPermiso citas = permisoDe(RolePurpose.CLIENT_PORTAL, "VISTA_MIS_CITAS");
        assertThat(citas.isLeer()).isTrue();
        assertThat(citas.isEscribir()).as("agendar").isTrue();
        assertThat(citas.isModificar()).as("reprogramar y cancelar").isTrue();
        assertThat(citas.isEliminar()).as("eliminar no es cancelar").isFalse();
    }

    @Test
    void elRestoDelPortalDelClienteEsSoloLectura() {
        aprovisionar();

        RolVistaPermiso mascotas = permisoDe(RolePurpose.CLIENT_PORTAL, "VISTA_MIS_MASCOTAS");
        assertThat(mascotas.isLeer()).isTrue();
        assertThat(mascotas.isEscribir() || mascotas.isModificar() || mascotas.isEliminar()).isFalse();
    }

    @Test
    void ningunPermisoPorDefectoDelClienteIncluyeEliminar() {
        aprovisionar();

        assertThat(guardados.stream().filter(p -> p.getRol().getPurpose() == RolePurpose.CLIENT_PORTAL))
                .isNotEmpty().noneMatch(RolVistaPermiso::isEliminar);
    }

    @Test
    void elAdministradorDeLaClinicaConservaTodasLasOperaciones() {
        aprovisionar();

        RolVistaPermiso empleados = permisoDe(RolePurpose.COMPANY_ADMIN, "VISTA_EMPLEADOS");
        assertThat(empleados.isLeer() && empleados.isEscribir() && empleados.isModificar() && empleados.isEliminar()).isTrue();
    }
}
