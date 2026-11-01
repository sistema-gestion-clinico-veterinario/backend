package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.mapper.MascotaCartillaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoDesparasitanteRepository;
import veterinaria.vargasvet.repository.TipoVacunaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.AppClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Registro real (sin simular repositorios) sobre H2. El comportamiento bajo concurrencia real, que depende del
 * bloqueo pesimista de PostgreSQL, se prueba en {@link CartillaConcurrenciaPostgresTest}.
 */
@DataJpaTest
class CartillaServiceConcurrencyIntegrationTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;
    @Autowired private TipoVacunaRepository tipoVacunaRepository;
    @Autowired private TipoDesparasitanteRepository tipoDesparasitanteRepository;
    @Autowired private RegistroVacunaRepository vacunaRepository;
    @Autowired private RegistroDesparasitacionRepository desparasitacionRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;

    @Test
    @DisplayName("[BUG] Un registro real (sin mocks de repositorio) persiste la cita de cobro y el registro con id")
    void unSoloRegistroPersisteCitaYRegistroConIdReal() {
        CartillaServiceImpl cartillaService = new CartillaServiceImpl(
                mascotaRepository, historiaClinicaRepository, empleadoRepository, serviciosRepository,
                tipoVacunaRepository, tipoDesparasitanteRepository, vacunaRepository, desparasitacionRepository,
                controlRepository, citaRepository, mock(UsuarioContactoService.class), mock(AuditLogService.class),
                mock(SimpMessagingTemplate.class), mock(MascotaCartillaMapper.class),
                mock(veterinaria.vargasvet.service.PetOwnershipService.class));

        EscenarioCartillaParaPruebas.Datos datos = new EscenarioCartillaParaPruebas(companyRepository, usuarioRepository,
                apoderadoRepository, mascotaRepository, empleadoRepository, serviciosRepository, tipoVacunaRepository).crear();

        CartillaAplicacionRequest request = new CartillaAplicacionRequest();
        request.setMascotaId(datos.mascotaId());
        request.setServicioId(datos.servicioId());
        request.setTipoVacunaId(datos.vacunaId());
        request.setFechaAplicacion(AppClock.today());
        request.setIntervaloCantidad(21);
        request.setIntervaloUnidad(IntervaloUnidad.DIAS);

        EscenarioCartillaParaPruebas.autenticarVeterinario(datos.vetUserId());
        try {
            var response = cartillaService.registrarVacunacion(request);
            assertThat(response.getRegistroId()).isNotNull();
            assertThat(response.getCitaId()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
