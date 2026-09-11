package veterinaria.vargasvet.ers.historias;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.domain.entity.ArchivoClinico;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoArchivo;
import veterinaria.vargasvet.repository.ArchivoClinicoRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.StorageService;
import veterinaria.vargasvet.service.impl.ArchivoClinicoServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF32Test {

    private ConsultaRepository consultaRepository;
    private ArchivoClinicoRepository archivoRepository;
    private StorageService storageService;
    private ArchivoClinicoServiceImpl service;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        archivoRepository = mock(ArchivoClinicoRepository.class);
        storageService = mock(StorageService.class);
        service = new ArchivoClinicoServiceImpl(consultaRepository, archivoRepository, storageService,
                mock(veterinaria.vargasvet.service.AuditLogService.class));

        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "superadmin@vargasvet.test", "", List.of(), null,
                1, RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF32-01] Adjunta un PDF de laboratorio y permite consultarlo después")
    void adjuntaArchivoPermitidoYLoLista() {
        Consulta consulta = consultaAbierta(10L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "hemograma.pdf", "application/pdf", "%PDF-prueba".getBytes());

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(storageService.storeBytes(any(byte[].class), any(), any(), any()))
                .thenReturn("clinical/hemograma.pdf");
        when(archivoRepository.save(any(ArchivoClinico.class))).thenAnswer(invocation -> {
            ArchivoClinico archivo = invocation.getArgument(0);
            archivo.setId(99L);
            return archivo;
        });

        var response = service.subirArchivo(10L, file, TipoArchivo.LABORATORIO, "Hemograma inicial");
        when(archivoRepository.findByConsultaId(10L)).thenReturn(List.of(archivoDesde(response, consulta)));

        var archivos = service.listarPorConsulta(10L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getUrl()).isEqualTo("clinical/hemograma.pdf");
        assertThat(response.getDescripcion()).isEqualTo("Hemograma inicial");
        assertThat(archivos).singleElement().extracting("nombre").isEqualTo("hemograma.pdf");
    }

    @Test
    @DisplayName("[CP-RF32-02] Rechaza una extensión no permitida sin almacenar ni persistir")
    void rechazaExtensionNoPermitidaSinEfectos() {
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consultaAbierta(10L)));
        MockMultipartFile file = new MockMultipartFile(
                "file", "resultado.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> service.subirArchivo(10L, file, TipoArchivo.LABORATORIO, "Resultado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Extensión no permitida");

        verify(storageService, never()).storeBytes(any(), any(), any(), any());
        verify(archivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("[CP-RF32-02] Rechaza más de 20 MB sin almacenar ni persistir")
    void rechazaArchivoDemasiadoGrandeSinEfectos() {
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consultaAbierta(10L)));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.subirArchivo(10L, file, TipoArchivo.LABORATORIO, "Resultado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 MB");

        verify(storageService, never()).storeBytes(any(), any(), any(), any());
        verify(archivoRepository, never()).save(any());
    }

    private Consulta consultaAbierta(Long id) {
        Consulta consulta = new Consulta();
        consulta.setId(id);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        return consulta;
    }

    private ArchivoClinico archivoDesde(veterinaria.vargasvet.dto.response.ArchivoClinicoResponse response,
                                        Consulta consulta) {
        ArchivoClinico archivo = new ArchivoClinico();
        archivo.setId(response.getId());
        archivo.setConsulta(consulta);
        archivo.setNombre(response.getNombre());
        archivo.setTipo(response.getTipo());
        archivo.setTipoMime(response.getTipoMime());
        archivo.setTamanioBytes(response.getTamanioBytes());
        archivo.setUrl(response.getUrl());
        archivo.setDescripcion(response.getDescripcion());
        archivo.setSubidoPor(response.getSubidoPor());
        return archivo;
    }
}
