package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Caja;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.EstadoEquipoResponse;
import veterinaria.vargasvet.dto.response.PuntoCobroResponse;
import veterinaria.vargasvet.repository.CajaRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.CajaDispositivoCookie;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuntoCobroServiceTest {

    @Mock CajaRepository cajaRepository;
    @Mock SesionCajaRepository sesionCajaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock CajaDispositivoCookie dispositivoCookie;
    @Mock AuditLogService auditLogService;
    @Mock veterinaria.vargasvet.repository.CompanyRepository companyRepository;

    private static final String SLUG = "sede-siete";

    private PuntoCobroService service;

    @BeforeEach
    void setUp() {
        service = new PuntoCobroService(cajaRepository, sesionCajaRepository, usuarioRepository, dispositivoCookie, auditLogService, companyRepository);
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        veterinaria.vargasvet.domain.entity.Company sede = new veterinaria.vargasvet.domain.entity.Company();
        sede.setId(7);
        sede.setSlug(SLUG);
        lenient().when(companyRepository.findById(7)).thenReturn(Optional.of(sede));
        lenient().when(dispositivoCookie.leer(SLUG)).thenReturn(Optional.empty());
        lenient().when(dispositivoCookie.leer(any(), eq(SLUG))).thenReturn(Optional.empty());
        lenient().when(cajaRepository.save(any(Caja.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sesionCajaRepository.findFirstByCajaIdAndEstado(any(), eq(EstadoSesionCaja.ABIERTA)))
                .thenReturn(Optional.empty());
        lenient().when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA)).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void sesion(RolePurpose purpose, Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "", List.of(), companyId,
                2, RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Caja caja(long id, String nombre, boolean activa, String token) {
        Caja caja = new Caja();
        caja.setId(id);
        caja.setCompanyId(7);
        caja.setNombre(nombre);
        caja.setActiva(activa);
        caja.setDispositivoTokenHash(token == null ? null : SecurityTokenUtils.hash(token));
        return caja;
    }

    private void cajas(Caja... cajas) {
        when(cajaRepository.findByCompanyIdOrderByNombreAsc(7)).thenReturn(List.of(cajas));
        lenient().when(cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(7))
                .thenReturn(java.util.Arrays.stream(cajas).filter(Caja::isActiva).toList());
    }

    // ---- a qué caja corresponde cada equipo

    @Test
    void conUnaSolaCajaSinEquipoCualquierNavegadorLaUsa() {
        cajas(caja(1, "Caja principal", true, null));

        PuntoCobroService.Resolucion resolucion = service.resolver(7);

        assertThat(resolucion.modo()).isEqualTo("SENCILLO");
        assertThat(resolucion.caja().getNombre()).isEqualTo("Caja principal");
    }

    @Test
    void unEquipoRegistradoOperaSuPropiaCaja() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        Caja dos = caja(2, "Mostrador 2", true, "token-dos");
        cajas(uno, dos);
        when(dispositivoCookie.leer(SLUG)).thenReturn(Optional.of("token-dos"));
        when(cajaRepository.findByDispositivoTokenHashAndCompanyId(SecurityTokenUtils.hash("token-dos"), 7)).thenReturn(Optional.of(dos));

        PuntoCobroService.Resolucion resolucion = service.resolver(7);

        assertThat(resolucion.modo()).isEqualTo("DISPOSITIVO");
        assertThat(resolucion.caja()).isSameAs(dos);
    }

    @Test
    void conVariasCajasUnEquipoSinRegistrarNoPuedeOperar() {
        cajas(caja(1, "Mostrador 1", true, null), caja(2, "Mostrador 2", true, null));

        assertThat(service.resolver(7).modo()).isEqualTo("NO_REGISTRADO");
        assertThatThrownBy(() -> service.resolverParaOperar(7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está registrado como punto de cobro");
    }

    @Test
    void siLaUnicaCajaYaTieneEquipoOtroNavegadorNoLaPuedeUsar() {
        cajas(caja(1, "Caja principal", true, "token-uno"));

        assertThat(service.resolver(7).modo()).isEqualTo("NO_REGISTRADO");
    }

    @Test
    void elEquipoDeOtraSedeNoSirveAqui() {
        Caja deOtraSede = caja(5, "Mostrador", true, "token-ajeno");
        deOtraSede.setCompanyId(8);
        cajas(caja(1, "Caja principal", true, null));
        when(dispositivoCookie.leer(SLUG)).thenReturn(Optional.of("token-ajeno"));

        PuntoCobroService.Resolucion resolucion = service.resolver(7);

        assertThat(resolucion.caja().getId()).isEqualTo(1L);
        assertThat(resolucion.modo()).isEqualTo("SENCILLO");
    }

    @Test
    void unaCajaDesactivadaNoSeOperaAunqueSeaElEquipo() {
        Caja vieja = caja(1, "Vieja", false, "token-viejo");
        cajas(vieja, caja(2, "Nueva", true, null), caja(3, "Otra", true, null));
        when(dispositivoCookie.leer(SLUG)).thenReturn(Optional.of("token-viejo"));
        when(cajaRepository.findByDispositivoTokenHashAndCompanyId(SecurityTokenUtils.hash("token-viejo"), 7)).thenReturn(Optional.of(vieja));

        assertThat(service.resolver(7).modo()).isEqualTo("NO_REGISTRADO");
    }

    @Test
    void siLaSedeNoTieneCajasSeCreaLaCajaPrincipal() {
        when(cajaRepository.findByCompanyIdOrderByNombreAsc(7)).thenReturn(List.of());
        lenient().when(cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(7)).thenReturn(List.of());

        service.resolver(7);

        ArgumentCaptor<Caja> guardada = ArgumentCaptor.forClass(Caja.class);
        verify(cajaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Caja principal");
        assertThat(guardada.getValue().getCompanyId()).isEqualTo(7);
    }

    @Test
    void nadieOperaLaCajaDeOtraSede() {
        assertThatThrownBy(() -> service.resolver(8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra sede");
    }

    @Test
    void elEstadoDeEsteEquipoExplicaQueEsYQueHacer() {
        cajas(caja(1, "Mostrador 1", true, null), caja(2, "Mostrador 2", true, null));

        EstadoEquipoResponse estado = service.estadoDeEsteEquipo(7);

        assertThat(estado.modo()).isEqualTo("NO_REGISTRADO");
        assertThat(estado.cajaNombre()).isNull();
        assertThat(estado.mensaje()).contains("Pide a un administrador");
    }

    // ---- gestión (solo administradores)

    @Test
    void soloUnAdministradorGestionaLosPuntosDeCobro() {
        sesion(RolePurpose.CUSTOM, 7);

        assertThatThrownBy(() -> service.crear(7, "Mostrador 3")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.actualizar(7, 1L, "X", null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.vincular(7, 1L, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.desvincular(7, 1L, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crearNormalizaElNombreYNoAceptaRepetidos() {
        when(cajaRepository.existsByCompanyIdAndNombreIgnoreCase(7, "Mostrador 2")).thenReturn(false);
        when(cajaRepository.existsByCompanyIdAndNombreIgnoreCase(7, "Principal")).thenReturn(true);

        PuntoCobroResponse creado = service.crear(7, "  Mostrador    2 ");

        assertThat(creado.nombre()).isEqualTo("Mostrador 2");
        assertThatThrownBy(() -> service.crear(7, "Principal"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ya existe");
        assertThatThrownBy(() -> service.crear(7, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedeDesactivarUnaCajaAbiertaNiLaUltimaActiva() {
        Caja uno = caja(1, "Mostrador 1", true, null);
        Caja dos = caja(2, "Mostrador 2", true, null);
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));
        when(cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(7)).thenReturn(List.of(uno));
        when(sesionCajaRepository.findFirstByCajaIdAndEstado(2L, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.of(new SesionCaja()));
        when(cajaRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(dos));

        assertThatThrownBy(() -> service.actualizar(7, 1L, "Mostrador 1", false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos un punto de cobro activo");
        assertThatThrownBy(() -> service.actualizar(7, 2L, "Mostrador 2", false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cierra la caja");
        assertThat(uno.isActiva()).isTrue();
    }

    @Test
    void desactivarUnPuntoLeQuitaSuEquipo() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        Caja dos = caja(2, "Mostrador 2", true, null);
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));
        when(cajaRepository.findByCompanyIdAndActivaTrueOrderByNombreAsc(7)).thenReturn(List.of(uno, dos));

        service.actualizar(7, 1L, "Mostrador 1", false);

        assertThat(uno.isActiva()).isFalse();
        assertThat(uno.getDispositivoTokenHash()).isNull();
    }

    @Test
    void unPuntoDeOtraSedeSeTrataComoInexistente() {
        when(cajaRepository.findByIdAndCompanyId(99L, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vincular(7, 99L, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(veterinaria.vargasvet.exception.ResourceNotFoundException.class);
    }

    @Test
    void vincularGuardaSoloElHashFijaLaCookieYDescribeElEquipo() {
        Caja uno = caja(1, "Mostrador 1", true, null);
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        MockHttpServletResponse response = new MockHttpServletResponse();

        PuntoCobroResponse resultado = service.vincular(7, 1L, request, response);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(dispositivoCookie).fijar(eq(response), token.capture(), eq(SLUG));
        assertThat(uno.getDispositivoTokenHash()).isEqualTo(SecurityTokenUtils.hash(token.getValue()))
                .isNotEqualTo(token.getValue());
        assertThat(token.getValue()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(uno.getDispositivoInfo()).isEqualTo("Chrome en Windows");
        assertThat(resultado.esEsteEquipo()).isTrue();
        verify(auditLogService).log(eq(7), eq("VINCULAR_PUNTO_COBRO"), eq("Caja"), anyString());
    }

    @Test
    void unNavegadorSoloPuedeSerUnPuntoDeCobro() {
        Caja uno = caja(1, "Mostrador 1", true, "token-viejo");
        Caja dos = caja(2, "Mostrador 2", true, null);
        when(cajaRepository.findByIdAndCompanyId(2L, 7)).thenReturn(Optional.of(dos));
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(dispositivoCookie.leer(request, SLUG)).thenReturn(Optional.of("token-viejo"));
        when(cajaRepository.findByDispositivoTokenHashAndCompanyId(SecurityTokenUtils.hash("token-viejo"), 7)).thenReturn(Optional.of(uno));

        service.vincular(7, 2L, request, new MockHttpServletResponse());

        assertThat(uno.getDispositivoTokenHash()).isNull();
        assertThat(dos.getDispositivoTokenHash()).isNotNull();
    }

    @Test
    void conLaCajaAbiertaNoSeCambiaNiSeQuitaSuEquipo() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));
        when(sesionCajaRepository.findFirstByCajaIdAndEstado(1L, EstadoSesionCaja.ABIERTA)).thenReturn(Optional.of(new SesionCaja()));

        assertThatThrownBy(() -> service.vincular(7, 1L, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cierra la caja");
        assertThatThrownBy(() -> service.desvincular(7, 1L, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cierra la caja");
        assertThat(uno.getDispositivoTokenHash()).isNotNull();
    }

    @Test
    void desvincularQuitaElEquipoYBorraLaCookieSiEraEsteNavegador() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(dispositivoCookie.leer(request, SLUG)).thenReturn(Optional.of("token-uno"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.desvincular(7, 1L, request, response);

        assertThat(uno.getDispositivoTokenHash()).isNull();
        verify(dispositivoCookie).borrar(response, SLUG);
    }

    @Test
    void desvincularDesdeOtroNavegadorNoTocaLaCookieDeEsteNavegador() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        when(cajaRepository.findByIdAndCompanyId(1L, 7)).thenReturn(Optional.of(uno));

        service.desvincular(7, 1L, new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(uno.getDispositivoTokenHash()).isNull();
        verify(dispositivoCookie, never()).borrar(any(), any());
    }

    @Test
    void elListadoMarcaCualEsEsteEquipoYQuienTieneLaCajaAbierta() {
        Caja uno = caja(1, "Mostrador 1", true, "token-uno");
        Caja dos = caja(2, "Mostrador 2", true, "token-dos");
        cajas(uno, dos);
        when(dispositivoCookie.leer(SLUG)).thenReturn(Optional.of("token-dos"));
        SesionCaja abierta = new SesionCaja();
        abierta.setCajaId(1L);
        abierta.setAbiertaPorUsuarioId(11);
        abierta.setAbiertaPor("luis@example.test");
        when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA)).thenReturn(List.of(abierta));
        veterinaria.vargasvet.domain.entity.Usuario luis = new veterinaria.vargasvet.domain.entity.Usuario();
        luis.setNombre("Luis");
        luis.setApellido("Gómez");
        when(usuarioRepository.findById(11)).thenReturn(Optional.of(luis));

        List<PuntoCobroResponse> lista = service.listar(7);

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).sesionAbierta()).isTrue();
        assertThat(lista.get(0).abiertaPorNombre()).isEqualTo("Luis Gómez");
        assertThat(lista.get(0).esEsteEquipo()).isFalse();
        assertThat(lista.get(1).esEsteEquipo()).isTrue();
        assertThat(lista.get(1).sesionAbierta()).isFalse();
        assertThat(lista).allSatisfy(punto -> assertThat(punto.toString()).doesNotContain("token"));
    }
}
