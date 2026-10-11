package veterinaria.vargasvet.ers.seguridad;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.security.RateLimitFilter;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Entorno común de las pruebas de seguridad con la aplicación completa: sin límites de tasa ni bloqueos de cuenta que
 * distorsionen lo que se mide, y un sembrador de clínicas con datos reales.
 */
@TestConfiguration
class ConfiguracionDePruebaSeguridad {

    @Bean
    @Primary
    SharedRateLimitService limitadorPermisivo() {
        SharedRateLimitService limitador = mock(SharedRateLimitService.class);
        when(limitador.tryConsume(any(), any(), anyInt(), any())).thenReturn(true);
        return limitador;
    }

    @Bean
    @Primary
    AccountLockoutService bloqueoNulo() {
        return mock(AccountLockoutService.class);
    }

    @Bean
    @Primary
    RateLimitFilter rateLimitFilter(SharedRateLimitService limitador) {
        return new RateLimitFilter(limitador, new ClientIpResolver(1)) {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain chain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    DosEmpresasSembradas sembradorDeClinicas(CompanyRepository companias, UsuarioRepository usuarios,
                                             ApoderadoRepository apoderados, MascotaRepository mascotas,
                                             EmpleadoRepository empleados, ServiciosVeterinariosRepository servicios,
                                             CitaRepository citas, HistoriaClinicaRepository historias,
                                             ConsultaRepository consultas, DiagnosticoRepository diagnosticos,
                                             TratamientoRepository tratamientos, PrescripcionRepository prescripciones,
                                             CategoriaProductoRepository categorias, MarcaProductoRepository marcas,
                                             UnidadMedidaRepository unidades, ProductoRepository productos,
                                             LoteRepository lotes, RoleRepository roles,
                                             UsuarioPorRolRepository asignaciones,
                                             UsuarioEmpresaContactoRepository contactos,
                                             UsuarioEmpresaCredencialRepository credenciales,
                                             UsuarioMembresiaRepository membresias, VistaRepository vistas,
                                             RolVistaPermisoRepository permisos,
                                             CompanyRoleProvisioningService aprovisionamiento,
                                             PasswordEncoder codificador, PlatformTransactionManager transacciones) {
        return new DosEmpresasSembradas(companias, usuarios, apoderados, mascotas, empleados, servicios, citas, historias,
                consultas, diagnosticos, tratamientos, prescripciones, categorias, marcas, unidades, productos, lotes,
                roles, asignaciones, contactos, credenciales, membresias, vistas, permisos, aprovisionamiento,
                codificador, new TransactionTemplate(transacciones));
    }
}
