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
import veterinaria.vargasvet.domain.entity.EmpleadoServicio;
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
    public static final String USER_PASSWORD = "E2eUser!123";

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
        Role adminRole = roleRepository.findFirstByName("ROLE_ADMIN").orElseThrow();
        Role ownerRole = seedRole("ROLE_APODERADO", "Propietario de mascotas", null);
        Role vetRole = seedRole("ROLE_VETERINARIO", "Veterinario E2E", null);
        Role restrictedRole = seedRole("ROLE_E2E_RESTRINGIDO", "Rol desactivable Selenium", company);
        seedLaboratorioView(superAdmin);
        seedRolePermissions(ownerRole, "VISTA_APODERADO_DASHBOARD", "VISTA_MIS_CITAS", "VISTA_MIS_MASCOTAS",
                "VISTA_MI_HISTORIAL", "VISTA_MIS_RECETAS", "VISTA_MIS_PAGOS", "VISTA_PROFILE");
        seedRolePermissions(vetRole, "VISTA_EMPLEADO_DASHBOARD", "VISTA_CITAS_AGENDA", "VISTA_HISTORIAS",
                "VISTA_RECETAS", "VISTA_MASCOTAS", "VISTA_LABORATORIO", "VISTA_MI_HORARIO", "VISTA_PROFILE");
        seedRolePermissions(adminRole, "VISTA_DASHBOARD");
        seedRolePermissions(restrictedRole, "VISTA_DASHBOARD", "VISTA_PROFILE");
        Usuario admin = seedAdmin(company, superAdmin);
        Apoderado apoderado = seedApoderado(company);
        assignRole(apoderado.getUser(), ownerRole);
        Apoderado otherOwner = seedApoderado(company, "e2e.other.owner@vargasvet.test", "Beatriz", "Aislada", "70000011");
        assignRole(otherOwner.getUser(), ownerRole);
        Raza raza = seedRaza(company);
        Mascota mascota = seedMascota(apoderado, raza);
        Mascota secondPet = seedMascota(apoderado, raza, "Nube E2E");
        Mascota thirdPet = seedMascota(apoderado, raza, "Sol E2E");
        Mascota foreignPet = seedMascota(otherOwner, raza, "Mascota Ajena E2E");
        Empleado veterinario = seedVeterinario(company);
        assignRole(veterinario.getUser(), vetRole);
        ServiciosVeterinarios servicio = seedServicio(company);
        assignService(veterinario, servicio);
        ServiciosVeterinarios toggleService = seedServicio(company, "Servicio Desactivable E2E", false);
        assignService(veterinario, toggleService);
        Empleado toggleEmployee = seedVeterinario(company, "e2e.toggle.vet@vargasvet.test", "Teresa", "Desactivable",
                "70000012", "CMVP-E2E-002");
        assignRole(toggleEmployee.getUser(), vetRole);
        assignService(toggleEmployee, servicio);
        Empleado activationEmployee = seedActivationEmployee(company, vetRole);
        Usuario restrictedUser = usuarioRepository.save(user("e2e.restricted@vargasvet.test", "Rol", "Restringido", company));
        assignRole(restrictedUser, restrictedRole);

        Company toggleCompany = seedCompany("Clínica Desactivable E2E", "20999999992", "e2e.toggle.company@vargasvet.test");
        Usuario toggleCompanyAdmin = usuarioRepository.save(user("e2e.company.admin@vargasvet.test", "Admin", "Empresa", toggleCompany));
        assignRole(toggleCompanyAdmin, adminRole);
        seedOperatingHours(company, veterinario);
        seedEmployeeSchedules(toggleEmployee);

        LocalDate date = LocalDate.now().plusDays(2);
        LocalDateTime clinicalStart = LocalDateTime.now().minusMinutes(5);
        Cita reprogramar = seedCita(mascota, veterinario, servicio, date.atTime(9, 0), "E2E REPROGRAMAR");
        Cita cancelar = seedCita(mascota, veterinario, servicio, date.atTime(10, 0), "E2E CANCELAR");
        Cita consulta = seedCita(mascota, veterinario, servicio, clinicalStart, "E2E CONSULTA");
        Cita consultaConcurrente = seedCita(secondPet, veterinario, servicio, clinicalStart.plusMinutes(1), "E2E CONSULTA CONCURRENTE");
        Cita consultaIa = seedCita(thirdPet, veterinario, servicio, clinicalStart.plusMinutes(2), "E2E CONSULTA IA");
        Cita pago = seedCita(mascota, veterinario, servicio, date.atTime(12, 0), "E2E PAGO YAPE");
        Cita historicalService = seedCita(secondPet, veterinario, toggleService, date.plusDays(3).atTime(15, 0), "E2E SERVICIO HISTORICO");

        fixtureRegistry.put("adminEmail", ADMIN_EMAIL);
        fixtureRegistry.put("adminPassword", ADMIN_PASSWORD);
        fixtureRegistry.put("userPassword", USER_PASSWORD);
        fixtureRegistry.put("companyId", company.getId());
        fixtureRegistry.put("companyName", company.getName());
        fixtureRegistry.put("ownerId", apoderado.getId());
        fixtureRegistry.put("ownerEmail", apoderado.getUser().getEmail());
        fixtureRegistry.put("ownerPassword", USER_PASSWORD);
        fixtureRegistry.put("otherOwnerId", otherOwner.getId());
        fixtureRegistry.put("petId", mascota.getId());
        fixtureRegistry.put("petUuid", mascota.getUuid());
        fixtureRegistry.put("petName", mascota.getNombreCompleto());
        fixtureRegistry.put("secondPetId", secondPet.getId());
        fixtureRegistry.put("secondPetName", secondPet.getNombreCompleto());
        fixtureRegistry.put("thirdPetId", thirdPet.getId());
        fixtureRegistry.put("thirdPetName", thirdPet.getNombreCompleto());
        fixtureRegistry.put("foreignPetId", foreignPet.getId());
        fixtureRegistry.put("foreignPetName", foreignPet.getNombreCompleto());
        fixtureRegistry.put("vetId", veterinario.getId());
        fixtureRegistry.put("vetEmail", veterinario.getUser().getEmail());
        fixtureRegistry.put("vetPassword", USER_PASSWORD);
        fixtureRegistry.put("vetName", veterinario.getUser().getNombre() + " " + veterinario.getUser().getApellido());
        fixtureRegistry.put("serviceId", servicio.getId());
        fixtureRegistry.put("serviceName", servicio.getNombre());
        fixtureRegistry.put("toggleServiceId", toggleService.getId());
        fixtureRegistry.put("toggleServiceName", toggleService.getNombre());
        fixtureRegistry.put("toggleEmployeeId", toggleEmployee.getId());
        fixtureRegistry.put("toggleEmployeeName", toggleEmployee.getUser().getNombre() + " " + toggleEmployee.getUser().getApellido());
        fixtureRegistry.put("appointmentDate", date.toString());
        fixtureRegistry.put("concurrencyDate", date.plusDays(2).toString());
        fixtureRegistry.put("quotaDate", date.plusDays(4).toString());
        fixtureRegistry.put("rescheduleTargetDate", date.plusDays(1).toString());
        fixtureRegistry.put("historicalServiceDate", date.plusDays(3).toString());
        fixtureRegistry.put("clinicalAppointmentDate", clinicalStart.toLocalDate().toString());
        fixtureRegistry.put("reprogramAppointmentId", reprogramar.getId());
        fixtureRegistry.put("cancelAppointmentId", cancelar.getId());
        fixtureRegistry.put("clinicalAppointmentId", consulta.getId());
        fixtureRegistry.put("concurrentClinicalAppointmentId", consultaConcurrente.getId());
        fixtureRegistry.put("iaClinicalAppointmentId", consultaIa.getId());
        fixtureRegistry.put("paymentAppointmentId", pago.getId());
        fixtureRegistry.put("historicalServiceAppointmentId", historicalService.getId());
        fixtureRegistry.put("adminUserId", admin.getId());
        fixtureRegistry.put("toggleCompanyId", toggleCompany.getId());
        fixtureRegistry.put("toggleCompanyName", toggleCompany.getName());
        fixtureRegistry.put("toggleCompanyAdminEmail", toggleCompanyAdmin.getEmail());
        fixtureRegistry.put("toggleCompanyAdminPassword", USER_PASSWORD);
        fixtureRegistry.put("restrictedRoleId", restrictedRole.getId());
        fixtureRegistry.put("restrictedRoleName", restrictedRole.getName());
        fixtureRegistry.put("restrictedUserEmail", restrictedUser.getEmail());
        fixtureRegistry.put("restrictedUserPassword", USER_PASSWORD);
        fixtureRegistry.put("activationEmployeeId", activationEmployee.getId());
        fixtureRegistry.put("activationEmail", activationEmployee.getUser().getEmail());
        fixtureRegistry.put("activationToken", activationEmployee.getUser().getVerificationToken());
    }

    private Company seedCompany() {
        return seedCompany("VargasVet E2E", "20999999991", "e2e@vargasvet.test");
    }

    private Company seedCompany(String name, String ruc, String email) {
        Company company = new Company();
        company.setName(name);
        company.setRuc(ruc);
        company.setAddress("Av. Pruebas 123");
        company.setPhone("999888777");
        company.setEmail(email);
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Role seedRole(String name, String description, Company company) {
        Role existing = company == null
                ? roleRepository.findFirstByName(name).orElse(null)
                : roleRepository.findByNameAndCompanyId(name, company.getId()).orElse(null);
        if (existing != null) return existing;
        Role role = new Role();
        role.setName(name);
        role.setDescripcion(description);
        role.setActivo(true);
        role.setCompany(company);
        return roleRepository.save(role);
    }

    private void seedRolePermissions(Role role, String... viewCodes) {
        for (String code : viewCodes) {
            Vista view = vistaRepository.findByCodigo(code).orElse(null);
            if (view == null) continue;
            boolean exists = rolVistaPermisoRepository.findByRolId(role.getId()).stream()
                    .anyMatch(permission -> permission.getVista().getId().equals(view.getId()));
            if (exists) continue;
            RolVistaPermiso permission = new RolVistaPermiso();
            permission.setRol(role);
            permission.setVista(view);
            permission.setLeer(true);
            permission.setEscribir(true);
            permission.setModificar(true);
            permission.setEliminar(true);
            rolVistaPermisoRepository.save(permission);
        }
    }

    private void assignRole(Usuario user, Role role) {
        if (usuarioPorRolRepository.existsByUsuarioIdAndRolId(user.getId(), role.getId())) return;
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setUsuario(user);
        assignment.setRol(role);
        usuarioPorRolRepository.save(assignment);
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
        return seedApoderado(company, "e2e.owner@vargasvet.test", "Ana", "Pruebas", "70000001");
    }

    private Apoderado seedApoderado(Company company, String email, String name, String lastName, String document) {
        Usuario owner = usuarioRepository.save(user(email, name, lastName, company));
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(owner);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(document);
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
        return seedMascota(apoderado, raza, "Luna E2E");
    }

    private Mascota seedMascota(Apoderado apoderado, Raza raza, String name) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(name);
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
        return seedVeterinario(company, "e2e.vet@vargasvet.test", "Victor", "Veterinario", "70000002", "CMVP-E2E-001");
    }

    private Empleado seedVeterinario(Company company, String email, String name, String lastName, String document, String license) {
        Usuario user = usuarioRepository.save(user(email, name, lastName, company));
        Empleado empleado = new Empleado();
        empleado.setUser(user);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(document);
        empleado.setGenero(Genero.MASCULINO);
        empleado.setNumeroColegiatura(license);
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private Empleado seedActivationEmployee(Company company, Role role) {
        Empleado employee = seedVeterinario(company, "e2e.activation@vargasvet.test", "Alicia", "Activación",
                "70000013", "CMVP-E2E-003");
        Usuario user = employee.getUser();
        user.setActivo(false);
        user.setEmailVerified(false);
        user.setPasswordChanged(false);
        user.setVerificationToken("selenium-e2e-activation-token");
        usuarioRepository.save(user);
        assignRole(user, role);
        return employee;
    }

    private ServiciosVeterinarios seedServicio(Company company) {
        return seedServicio(company, "Consulta general E2E", true);
    }

    private ServiciosVeterinarios seedServicio(Company company, String name, boolean emergency) {
        ServiciosVeterinarios service = new ServiciosVeterinarios();
        service.setCompany(company);
        service.setNombre(name);
        service.setDescripcion("Servicio aislado para pruebas E2E");
        service.setPrecio(new BigDecimal("100.00"));
        service.setDuracionEstimada(30);
        service.setDisponible(true);
        service.setActivo(true);
        service.setPermiteEmergencia(emergency);
        return serviciosVeterinariosRepository.save(service);
    }

    private void assignService(Empleado employee, ServiciosVeterinarios service) {
        employee.getServicios().add(new EmpleadoServicio(employee, service));
        empleadoRepository.save(employee);
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

        seedEmployeeSchedules(empleado);
    }

    private void seedEmployeeSchedules(Empleado empleado) {
        for (int offset = 0; offset < 21; offset++) {
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
        user.setPassword(passwordEncoder.encode(USER_PASSWORD));
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
