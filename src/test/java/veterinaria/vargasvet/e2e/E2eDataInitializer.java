package veterinaria.vargasvet.e2e;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RazaRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.UUID;

@Component
@Profile("e2e")
@Order(2)
@RequiredArgsConstructor
public class E2eDataInitializer implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "e2e.admin@vargasvet.test";
    public static final String ADMIN_PASSWORD = "E2eTest!123";

    private final CompanyRepository companyRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final RazaRepository razaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    private final CompanyOperatingHourRepository companyOperatingHourRepository;
    private final HorarioEmpleadoRepository horarioEmpleadoRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;
    private final E2eFixtureRegistry fixtureRegistry;

    @Override
    @Transactional
    public void run(String... args) {
        Company company = seedCompany();
        Role superAdmin = roleRepository.findFirstByName("ROLE_SUPER_ADMIN").orElseThrow();
        seedSystemRole("ROLE_APODERADO", "Propietario de mascotas");
        seedLaboratorioView(superAdmin);
        Usuario admin = seedAdmin(company, superAdmin);
        Apoderado apoderado = seedApoderado(company);
        Raza raza = seedRaza(company);
        Mascota mascota = seedMascota(apoderado, raza);
        Empleado veterinario = seedVeterinario(company);
        ServiciosVeterinarios servicio = seedServicio(company);
        seedOperatingHours(company, veterinario);

        LocalDate date = LocalDate.now().plusDays(2);
        LocalDateTime clinicalStart = LocalDateTime.now().minusMinutes(5);
        Cita reprogramar = seedCita(mascota, veterinario, servicio, date.atTime(9, 0), "E2E REPROGRAMAR");
        Cita cancelar = seedCita(mascota, veterinario, servicio, date.atTime(10, 0), "E2E CANCELAR");
        Cita consulta = seedCita(mascota, veterinario, servicio, clinicalStart, "E2E CONSULTA");
        Cita pago = seedCita(mascota, veterinario, servicio, date.atTime(12, 0), "E2E PAGO YAPE");

        fixtureRegistry.put("adminEmail", ADMIN_EMAIL);
        fixtureRegistry.put("adminPassword", ADMIN_PASSWORD);
        fixtureRegistry.put("companyId", company.getId());
        fixtureRegistry.put("ownerId", apoderado.getId());
        fixtureRegistry.put("petId", mascota.getId());
        fixtureRegistry.put("petUuid", mascota.getUuid());
        fixtureRegistry.put("petName", mascota.getNombreCompleto());
        fixtureRegistry.put("vetId", veterinario.getId());
        fixtureRegistry.put("serviceId", servicio.getId());
        fixtureRegistry.put("appointmentDate", date.toString());
        fixtureRegistry.put("clinicalAppointmentDate", clinicalStart.toLocalDate().toString());
        fixtureRegistry.put("reprogramAppointmentId", reprogramar.getId());
        fixtureRegistry.put("cancelAppointmentId", cancelar.getId());
        fixtureRegistry.put("clinicalAppointmentId", consulta.getId());
        fixtureRegistry.put("paymentAppointmentId", pago.getId());
        fixtureRegistry.put("adminUserId", admin.getId());
    }

    private Company seedCompany() {
        Company company = new Company();
        company.setName("VargasVet E2E");
        company.setRuc("20999999991");
        company.setAddress("Av. Pruebas 123");
        company.setPhone("999888777");
        company.setEmail("e2e@vargasvet.test");
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private void seedSystemRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setDescripcion(description);
        role.setActivo(true);
        roleRepository.save(role);
    }

    private Usuario seedAdmin(Company company, Role role) {
        Usuario admin = user(ADMIN_EMAIL, "Administrador", "E2E", company);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin = usuarioRepository.save(admin);

        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setUsuario(admin);
        assignment.setRol(role);
        usuarioPorRolRepository.save(assignment);
        return admin;
    }

    private void seedLaboratorioView(Role superAdmin) {
        Vista view = vistaRepository.findByCodigo("VISTA_LABORATORIO").orElseGet(() -> {
            Vista created = new Vista();
            created.setCodigo("VISTA_LABORATORIO");
            created.setNombre("Laboratorio IA");
            created.setRuta("/laboratorio");
            created.setGrupo("CLINICA");
            created.setOrden(6);
            created.setActivo(true);
            return vistaRepository.save(created);
        });

        if (rolVistaPermisoRepository.findByRolId(superAdmin.getId()).stream()
                .noneMatch(permission -> permission.getVista().getId().equals(view.getId()))) {
            RolVistaPermiso permission = new RolVistaPermiso();
            permission.setRol(superAdmin);
            permission.setVista(view);
            permission.setLeer(true);
            permission.setEscribir(true);
            permission.setModificar(true);
            permission.setEliminar(true);
            rolVistaPermisoRepository.save(permission);
        }
    }

    private Apoderado seedApoderado(Company company) {
        Usuario owner = usuarioRepository.save(user("e2e.owner@vargasvet.test", "Ana", "Pruebas", company));
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(owner);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento("70000001");
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.save(apoderado);
    }

    private Raza seedRaza(Company company) {
        Raza raza = new Raza();
        raza.setNombre("Mestizo E2E");
        raza.setDescripcion("Raza aislada para pruebas E2E");
        raza.setEspecie(EspecieMascota.PERRO);
        raza.setActivo(true);
        raza.setCompanyId(company.getId().longValue());
        return razaRepository.save(raza);
    }

    private Mascota seedMascota(Apoderado apoderado, Raza raza) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Luna E2E");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setSexo(SexoMascota.HEMBRA);
        mascota.setFechaNacimiento(LocalDate.now().minusYears(3));
        mascota.setPeso(12.5);
        mascota.setEsterilizado(true);
        mascota.setActivo(true);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setApoderado(apoderado);
        mascota.setRaza(raza);
        return mascotaRepository.save(mascota);
    }

    private Empleado seedVeterinario(Company company) {
        Usuario user = usuarioRepository.save(user("e2e.vet@vargasvet.test", "Victor", "Veterinario", company));
        Empleado empleado = new Empleado();
        empleado.setUser(user);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("70000002");
        empleado.setGenero(Genero.MASCULINO);
        empleado.setNumeroColegiatura("CMVP-E2E-001");
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private ServiciosVeterinarios seedServicio(Company company) {
        ServiciosVeterinarios service = new ServiciosVeterinarios();
        service.setCompany(company);
        service.setNombre("Consulta general E2E");
        service.setDescripcion("Servicio aislado para pruebas E2E");
        service.setPrecio(new BigDecimal("100.00"));
        service.setDuracionEstimada(30);
        service.setDisponible(true);
        service.setActivo(true);
        service.setPermiteEmergencia(true);
        return serviciosVeterinariosRepository.save(service);
    }

    private void seedOperatingHours(Company company, Empleado empleado) {
        for (DiaSemana day : DiaSemana.values()) {
            CompanyOperatingHour companyHour = new CompanyOperatingHour();
            companyHour.setCompany(company);
            companyHour.setDiaSemana(day);
            companyHour.setOpeningTime(LocalTime.of(8, 0));
            companyHour.setClosingTime(LocalTime.of(18, 0));
            companyHour.setIsOpen(true);
            companyOperatingHourRepository.save(companyHour);
        }

        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = LocalDate.now().plusDays(offset);
            HorarioEmpleado schedule = new HorarioEmpleado();
            schedule.setEmpleado(empleado);
            schedule.setFecha(date);
            schedule.setDiaSemana(toDiaSemana(date.getDayOfWeek()));
            schedule.setHoraInicio(LocalTime.of(8, 0));
            schedule.setHoraFin(LocalTime.of(18, 0));
            schedule.setActivo(true);
            horarioEmpleadoRepository.save(schedule);
        }
    }

    private Cita seedCita(
            Mascota mascota,
            Empleado empleado,
            ServiciosVeterinarios servicio,
            LocalDateTime inicio,
            String motivo
    ) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita(motivo);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setTotalServicio(servicio.getPrecio());
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario user(String email, String nombre, String apellido, Company company) {
        Usuario user = new Usuario();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("E2eUser!123"));
        user.setNombre(nombre);
        user.setApellido(apellido);
        user.setDni(String.valueOf(71000000 + usuarioRepository.count()));
        user.setActivo(true);
        user.setEmailVerified(true);
        user.setPasswordChanged(true);
        user.setCompany(company);
        return user;
    }

    private DiaSemana toDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }
}
