package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.*;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartillaServiceImplTest {

    @Mock MascotaRepository mascotaRepository;
    @Mock HistoriaClinicaRepository historiaClinicaRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ServiciosVeterinariosRepository serviciosRepository;
    @Mock TipoVacunaRepository tipoVacunaRepository;
    @Mock TipoDesparasitanteRepository tipoDesparasitanteRepository;
    @Mock RegistroVacunaRepository vacunaRepository;
    @Mock RegistroDesparasitacionRepository desparasitacionRepository;
    @Mock ControlPreventivoRepository controlRepository;
    @Mock CitaRepository citaRepository;
    @Mock AuditLogService auditLogService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks CartillaServiceImpl service;

    private Company company;
    private Mascota mascota;
    private Empleado veterinario;
    private ServiciosVeterinarios servicio;
    private TipoVacuna vacuna;
    private HistoriaClinica historia;
    private ControlPreventivo pendiente;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(7);

        Usuario propietario = usuario(10, "Ana", "Perez");
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(propietario);
        mascota = new Mascota();
        mascota.setId(20L);
        mascota.setNombreCompleto("Tobby");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setFechaNacimiento(AppClock.today().minusYears(2));
        mascota.setApoderado(apoderado);

        veterinario = new Empleado();
        veterinario.setId(30L);
        veterinario.setEstado(true);
        veterinario.setNumeroColegiatura("CMVP-1234");
        veterinario.setUser(usuario(11, "Luis", "Saavedra"));

        servicio = new ServiciosVeterinarios();
        servicio.setId(40L);
        servicio.setCompany(company);
        servicio.setNombre("Vacunacion");
        servicio.setTipoControlPreventivo(TipoControlServicio.VACUNACION);
        servicio.setDuracionEstimada(15);

        vacuna = new TipoVacuna();
        vacuna.setId(50L);
        vacuna.setCompany(company);
        vacuna.setEspecie(EspecieMascota.PERRO);
        vacuna.setActivo(true);
        vacuna.setNombre("Nobivac Parvo-C");
        vacuna.setPrecio(new BigDecimal("95.00"));

        historia = new HistoriaClinica();
        historia.setId(60L);
        historia.setMascota(mascota);
        historia.setNumeroHc("HC-000020");

        pendiente = new ControlPreventivo();
        pendiente.setId(70L);
        pendiente.setMascota(mascota);
        pendiente.setTipo(TipoControlPreventivo.VACUNACION);
        pendiente.setTipoVacuna(vacuna);
        pendiente.setNombreControl(vacuna.getNombre());
        pendiente.setFechaRecomendada(AppClock.today());
        pendiente.setEstado(EstadoControlPreventivo.PENDIENTE);
    }

    @Test
    void registrarVacunacionCierraControlCreaSiguienteYCalculaPrecioEnServidor() {
        when(mascotaRepository.findById(20L)).thenReturn(Optional.of(mascota));
        when(empleadoRepository.findByUserId(11)).thenReturn(Optional.of(veterinario));
        when(serviciosRepository.findById(40L)).thenReturn(Optional.of(servicio));
        when(tipoVacunaRepository.findById(50L)).thenReturn(Optional.of(vacuna));
        when(historiaClinicaRepository.findByMascotaId(20L)).thenReturn(Optional.of(historia));
        when(controlRepository.findById(70L)).thenReturn(Optional.of(pendiente));
        when(controlRepository.existsByMascotaIdAndTipoVacunaIdAndFechaRecomendadaAndEstadoIn(
                eq(20L), eq(50L), any(), any())).thenReturn(false);
        when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita cita = inv.getArgument(0);
            cita.setId(80L);
            cita.setNumeroCita("CIT-TEST");
            return cita;
        });
        when(vacunaRepository.save(any())).thenAnswer(inv -> {
            RegistroVacuna registro = inv.getArgument(0);
            registro.setId(90L);
            return registro;
        });

        CartillaAplicacionRequest request = new CartillaAplicacionRequest();
        request.setMascotaId(20L);
        request.setControlPreventivoId(70L);
        request.setServicioId(40L);
        request.setTipoVacunaId(50L);
        request.setFechaAplicacion(AppClock.today());
        request.setIntervaloCantidad(21);
        request.setIntervaloUnidad(IntervaloUnidad.DIAS);
        request.setTotal(new BigDecimal("1.00"));
        request.setLote(" L2408A ");
        request.setDosis(new BigDecimal("1.0"));
        request.setUnidadDosis("ml");

        CartillaAplicacionResponse response;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            security.when(SecurityUtils::getCurrentUserId).thenReturn(11);
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("vet@example.com");
            response = service.registrarVacunacion(request);
        }

        assertThat(pendiente.getEstado()).isEqualTo(EstadoControlPreventivo.APLICADO);
        assertThat(response.getTotal()).isEqualByComparingTo("95.00");
        assertThat(response.getFechaProxima()).isEqualTo(AppClock.today().plusDays(21));
        assertThat(response.getLote()).isEqualTo("L2408A");

        ArgumentCaptor<RegistroVacuna> registroCaptor = ArgumentCaptor.forClass(RegistroVacuna.class);
        verify(vacunaRepository).save(registroCaptor.capture());
        assertThat(registroCaptor.getValue().getControlPreventivo()).isSameAs(pendiente);
        assertThat(registroCaptor.getValue().getIntervaloUnidad()).isEqualTo(IntervaloUnidad.DIAS);

        ArgumentCaptor<Cita> citaCaptor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(citaCaptor.capture());
        assertThat(citaCaptor.getValue().getEstado()).isEqualTo(EstadoCita.COMPLETADA);
        assertThat(citaCaptor.getValue().getTotalServicio()).isEqualByComparingTo("95.00");
        verify(messagingTemplate).convertAndSend(eq("/topic/caja/7"), any(Object.class));

        ArgumentCaptor<ControlPreventivo> controlCaptor = ArgumentCaptor.forClass(ControlPreventivo.class);
        verify(controlRepository, times(2)).save(controlCaptor.capture());
        List<ControlPreventivo> guardados = controlCaptor.getAllValues();
        assertThat(guardados).anySatisfy(control -> {
            assertThat(control.getId()).isNull();
            assertThat(control.getFechaRecomendada()).isEqualTo(AppClock.today().plusDays(21));
            assertThat(control.getEstado()).isEqualTo(EstadoControlPreventivo.PROGRAMADO);
        });
    }

    private Usuario usuario(Integer id, String nombre, String apellido) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setNombre(nombre);
        user.setApellido(apellido);
        user.setCompany(company);
        return user;
    }
}
