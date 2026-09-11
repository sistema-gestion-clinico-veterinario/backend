package veterinaria.vargasvet.ers.catalogos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.EspecialidadServiceImpl;
import veterinaria.vargasvet.service.impl.ServicioServiceImpl;
import veterinaria.vargasvet.service.impl.TipoEmpleadoServiceImpl;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoProteccionServiceTest {

    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ServiciosVeterinariosRepository servicioRepository;
    @Mock private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private CitaRepository citaRepository;

    @BeforeEach
    void authenticateSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(1, "qa@system.local", "", authorities, null,
                1, RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cpRf3601_actualizarEspecialidadConservaEmpleadosAsignados() {
        Especialidad existente = new Especialidad();
        existente.setId(10L);
        existente.setNombre("Cirugía");
        Empleado asignado = new Empleado();
        asignado.setId(20L);
        existente.setEmpleados(new ArrayList<>(List.of(asignado)));

        Especialidad cambios = new Especialidad();
        cambios.setNombre("Cirugía General");
        cambios.setDescripcion("Atención quirúrgica");

        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(especialidadRepository.save(any(Especialidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Especialidad actualizada = especialidadService().update(10L, cambios);

        assertEquals("Cirugía General", actualizada.getNombre());
        assertEquals(1, actualizada.getEmpleados().size());
        assertSame(asignado, actualizada.getEmpleados().get(0));
    }

    @Test
    void cpRf3701_documentaDefectoEspecialidadEnUsoSeIntentaEliminarFisicamente() {
        Especialidad enUso = new Especialidad();
        enUso.setId(10L);
        Empleado asignado = new Empleado();
        asignado.setId(20L);
        enUso.setEmpleados(new ArrayList<>(List.of(asignado)));
        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(enUso));

        especialidadService().delete(10L);

        verify(especialidadRepository).delete(enUso);
    }

    @Test
    void cpRf4301_bloqueaEliminarServicioUtilizadoEnCitas() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setId(30L);
        when(servicioRepository.findById(30L)).thenReturn(Optional.of(servicio));
        when(citaRepository.existsByServicioId(30L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> servicioService().eliminar(30L)
        );

        assertEquals("No se puede eliminar el servicio porque ya ha sido utilizado en citas", error.getMessage());
        verify(servicioRepository, never()).save(any(ServiciosVeterinarios.class));
    }

    @Test
    void cpRf4001_bloqueaEliminarTipoLaboralConEmpleadosAsignados() {
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setId(40L);
        tipo.setNombre("Veterinario");
        when(tipoEmpleadoRepository.findById(40L)).thenReturn(Optional.of(tipo));
        when(empleadoRepository.countByTipoEmpleadoId(40L)).thenReturn(2L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tipoEmpleadoService().delete(40L)
        );

        assertEquals("No se puede eliminar el tipo de empleado porque tiene empleados asignados", error.getMessage());
        verify(tipoEmpleadoRepository, never()).delete(any(TipoEmpleado.class));
    }

    private EspecialidadServiceImpl especialidadService() {
        return new EspecialidadServiceImpl(especialidadRepository, companyRepository);
    }

    private ServicioServiceImpl servicioService() {
        return new ServicioServiceImpl(servicioRepository, companyRepository, tipoEmpleadoRepository, citaRepository);
    }

    private TipoEmpleadoServiceImpl tipoEmpleadoService() {
        return new TipoEmpleadoServiceImpl(tipoEmpleadoRepository, companyRepository, empleadoRepository);
    }
}
