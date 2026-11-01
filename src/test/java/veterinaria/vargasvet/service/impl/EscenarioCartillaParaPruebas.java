package veterinaria.vargasvet.service.impl;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.TipoVacuna;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoControlServicio;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoVacunaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Datos mínimos reales (clínica, propietario, mascota, veterinario, servicio y vacuna) para probar la cartilla. */
final class EscenarioCartillaParaPruebas {

    record Datos(Long mascotaId, Long servicioId, Long vacunaId, Integer vetUserId) {
    }

    private final CompanyRepository companyRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServiciosVeterinariosRepository serviciosRepository;
    private final TipoVacunaRepository tipoVacunaRepository;

    EscenarioCartillaParaPruebas(CompanyRepository companyRepository, UsuarioRepository usuarioRepository,
                                 ApoderadoRepository apoderadoRepository, MascotaRepository mascotaRepository,
                                 EmpleadoRepository empleadoRepository, ServiciosVeterinariosRepository serviciosRepository,
                                 TipoVacunaRepository tipoVacunaRepository) {
        this.companyRepository = companyRepository;
        this.usuarioRepository = usuarioRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.mascotaRepository = mascotaRepository;
        this.empleadoRepository = empleadoRepository;
        this.serviciosRepository = serviciosRepository;
        this.tipoVacunaRepository = tipoVacunaRepository;
    }

    Datos crear() {
        Company company = new Company();
        company.setName("VargasVet Concurrencia");
        company.setSlug("vargasvet-concurrencia-" + UUID.randomUUID());
        company.setRuc(digitos(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario vetUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(vetUser);
        empleado.setCompany(company);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(digitos(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleadoRepository.save(empleado);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Vacunacion");
        servicio.setDescripcion("Aplicacion de vacunas");
        servicio.setPrecio(new BigDecimal("90.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(15);
        servicio.setPermiteEmergencia(false);
        servicio.setTipoControlPreventivo(TipoControlServicio.VACUNACION);
        servicio = serviciosRepository.save(servicio);

        TipoVacuna vacuna = new TipoVacuna();
        vacuna.setCompany(company);
        vacuna.setNombre("Nobivac Parvo-C");
        vacuna.setEspecie(EspecieMascota.PERRO);
        vacuna.setPrecio(new BigDecimal("90.00"));
        vacuna.setActivo(true);
        vacuna.setCreatedBy("SYSTEM");
        vacuna.setUpdatedBy("SYSTEM");
        vacuna = tipoVacunaRepository.save(vacuna);

        return new Datos(mascota.getId(), servicio.getId(), vacuna.getId(), vetUser.getId());
    }

    static void autenticarVeterinario(Integer userId) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(
                userId, "vet-" + userId + "@vargasvet.test", "", authorities, null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null, authorities));
    }

    private Usuario usuario(String prefijo, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(prefijo);
        usuario.setApellido("Test");
        usuario.setDni(digitos(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private static String digitos(int largo) {
        StringBuilder digitos = new StringBuilder(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())));
        while (digitos.length() < largo) {
            digitos.append('0');
        }
        return digitos.substring(0, largo);
    }
}
