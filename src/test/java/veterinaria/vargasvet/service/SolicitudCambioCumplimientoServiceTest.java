package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.SolicitudCambioCumplimientoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudCambioCumplimientoServiceTest {

    @Mock private SolicitudCambioCumplimientoRepository repository;
    @Mock private CompanyRepository companyRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CompanyComplianceService complianceService;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private SolicitudCambioCumplimientoService service;

    @Test
    void listarGlobalUsaLimitesTipadosCuandoNoHayFiltrosDeFecha() {
        when(repository.buscarGlobal(eq(null), eq(null), eq(null), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listarGlobal(null, null, null, null, null, 0, 10);

        verify(repository).buscarGlobal(
                eq(null), eq(null), eq(null),
                eq(LocalDateTime.of(1900, 1, 1, 0, 0)),
                eq(LocalDateTime.of(9999, 12, 31, 23, 59, 59)),
                any(Pageable.class));
    }
}
